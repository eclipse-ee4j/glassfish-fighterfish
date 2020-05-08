/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.osgijavaeebase;

import static org.glassfish.osgijavaeebase.Constants.FILE_PROTOCOL;
import static org.glassfish.osgijavaeebase.Constants.REFERENCE_PROTOCOL;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.osgi.framework.Bundle;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import com.sun.enterprise.deploy.shared.AbstractReadableArchive;

/**
 * Adapts a {@link Bundle} to {@link Archive}. It uses JAR File space of the bundle (via getEntry and getEntryPaths
 * APIs), so a bundle does not have to be in resolved state. Since it represents JAR File space of the bundle, it does
 * not consider resources from any fragments.
 */
public final class OSGiBundleArchive extends AbstractReadableArchive implements URIable, Iterable<BundleResource> {

    /**
     * The bundle.
     */
    private final Bundle bundle;

    /**
     * The archive name.
     */
    private String name;

    /**
     * The archive URI.
     */
    private URI uri;

    /**
     * The nested archives.
     */
    private final Map<String, ReadableArchive> subArchives = new HashMap<>();

    /**
     * Create a new instance.
     *
     * @param bnd the bundle
     */
    public OSGiBundleArchive(final Bundle bnd) {
        this.bundle = bnd;
        init();
    }

    /**
     * This method initializes {@link #uri} and {@link #name}.
     */
    private void init() {
        // The only time we can rely on a bundle's location is when the
        // location string begins with reference: scheme, as both Felix and
        // equinox assumes that the rest of the location is a file.
        // In no other case, we can use rely on bundle.getLocation()
        // to arrive at the URI of the underlying archive.
        // e.g., user can install like this:
        // bundleContext.install ("file:/a/b.jar",
        // new URL("file:/c/d.jar").openStream));
        // In the above case, although location returns a.jar, the actual
        // archive is read from d.jar.
        // So, we return a valid URI only for reference: scheme and in all
        // cases, we prefer to return null as opposed to throwing an exception
        // to keep the behavior same as MemoryMappedArchive.
        String location = bundle.getLocation();
        if (location != null && location.startsWith(REFERENCE_PROTOCOL)) {
            location = location.substring(REFERENCE_PROTOCOL.length());

            // We only know how to handle reference:file: type urls.
            if (location.startsWith(FILE_PROTOCOL)) {

                try {
                    // Decode any URL escaped sequences.
                    location = URLDecoder.decode(location, Charset.defaultCharset().name());
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }

                // Return iff referenced file exists.
                File file = new File(location.substring(FILE_PROTOCOL.length()));
                if (file.exists()) {
                    uri = file.toURI();
                }
            }
        }

        // See if there is a symbolic name & version. Use them,
        // else use location. Either symbolic name or location must exist
        // in a bundle.
        String symName = bundle.getSymbolicName();
        String version = bundle.getHeaders().get(BUNDLE_VERSION);
        if (symName != null) {
            if (version == null) {
                name = symName;
            } else {
                name = symName.concat("_").concat(version);
            }
        } else {
            name = location;
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Enumeration<String> entries() {
        ArrayList<String> entries = new ArrayList<>();
        getEntryPaths(entries, "/");
        ListIterator<String> entriesIter = entries.listIterator();
        while (entriesIter.hasNext()) {
            String next = entriesIter.next();
            if (next.endsWith("/")) {
                // return only file entries as per the conract of this method
                entriesIter.remove();
            }
        }
        return Collections.enumeration(entries);
    }

    /**
     * Returns the enumeration of first level directories in this archive.
     *
     * @return enumeration of directories under the root of this archive
     * @throws java.io.IOException
     */
    @Override
    public Collection<String> getDirectories() throws IOException {
        return getSubDirectories("/");
    }

    /**
     * Return subdirectories under a given path. This returns only result from one level, i.e., non-recursive
     *
     * @param path base path
     * @return list of subdirectory name
     */
    private Collection<String> getSubDirectories(final String path) {
        final Enumeration firstLevelEntries = bundle.getEntryPaths(path);
        if (firstLevelEntries == null) {
            return Collections.emptyList();
        }
        Collection<String> firstLevelDirs = new ArrayList<>();
        while (firstLevelEntries.hasMoreElements()) {
            String firstLevelEntry = (String) firstLevelEntries.nextElement();
            if (firstLevelEntry.endsWith("/")) {
                firstLevelDirs.add(firstLevelEntry);
            }
        }
        return firstLevelDirs;
    }

    /**
     * Get the entry paths using {@link org.osgi.framework.Bundle#getEntryPaths(java.lang.String)}.
     *
     * @param entries the collection to add to
     * @param path the base path
     */
    private void getEntryPaths(final Collection<String> entries, final String path) {

        Enumeration<String> subPaths = bundle.getEntryPaths(path);
        if (subPaths != null) {
            while (subPaths.hasMoreElements()) {
                String next = subPaths.nextElement();
                if ("META-INF/".equals(next) && entries.contains(next)) {
                    // Work around for FELIX-2935 (GLASSFISH-16477)
                    continue;
                }
                entries.add(next);
                getEntryPaths(entries, next);
            }
        }
        // BECAUSE OF A BUG IN FELIX (FELIX-1210), THE CODE ABOVE DOES NOT
        // WORK WHEN THERE ARE NO DIRECTORY ENTRIES IN THE JAR FILE.
        // IF WE CONSISTENTLY FACE THE ISSUE, THEN WE CAN USE AN ALTERNATIVE
        // IMPL BASED ON findEntries.
        // OF COURSE, IT WILL HAVE THE UNDESIRED SIDE EFFECT OF FINDINDG
        // ENTRIES FROM FRAGMENTS AS WELL.
        // WE HAVE NASTY SIDE EFFECTS WHEN THAT HAPPENS. e.g. NPE. SO,
        // WE DON'T USE THE ALTERNATIVE IMPLEMENTATION ANY MORE.
        // WE EXPECT JAR TO HAVE PROPER DIRECTORY ENTRIES.
        // getEntryPaths2(entries, path); // call the new implementation
    }

    @Override
    public Enumeration<String> entries(final String prefix) {
        Collection<String> entries = new ArrayList<>();
        getEntryPaths(entries, prefix);
        return Collections.enumeration(entries);
    }

    @Override
    public boolean isDirectory(final String entryName) {
        String zEntryName;
        if (entryName.endsWith("/")) {
            zEntryName = entryName;
        } else {
            zEntryName = entryName + "/";
        }
        return bundle.getEntry(zEntryName) != null;
    }

    @Override
    public Manifest getManifest() throws IOException {
        URL url = bundle.getEntry(JarFile.MANIFEST_NAME);
        if (url != null) {
            InputStream is = url.openStream();
            try {
                return new Manifest(is);
            } finally {
                is.close();
            }

        }
        return null;
    }

    /**
     * It returns URI for the underlying file if it can locate such a file. Else, it returns null.
     *
     * @return URI
     */
    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public long getArchiveSize() throws SecurityException {
        // Don't know how to calculate the size.
        return -1;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream getEntry(final String entryName) throws IOException {
        URL entry = bundle.getEntry(entryName);
        if (entry != null) {
            return entry.openStream();
        }
        return null;
    }

    @Override
    public boolean exists(final String entryName) {
        return bundle.getEntry(entryName) != null;
    }

    @Override
    public long getEntrySize(final String entryName) {
        return 0;
    }

    @Override
    public void open(final URI entryUri) throws IOException {
        throw new UnsupportedOperationException("Not applicable method");
    }

    @Override
    public ReadableArchive getSubArchive(final String entryName) throws IOException {

        if (!exists(entryName)) {
            return null;
        }
        synchronized (this) {
            if (!subArchives.containsKey(entryName)) {
                ReadableArchive subArchive;
                if (isDirectory(entryName)) {
                    subArchive = new EmbeddedDirectoryArchive(entryName);
                } else {
                    subArchive = new EmbeddedJarArchive(entryName);
                }
                subArchives.put(entryName, subArchive);
            }
            return subArchives.get(entryName);
        }
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean renameTo(final String newName) {
        return false;
    }

    @Override
    public void setParentArchive(final ReadableArchive parentArchive) {
        // Not needed until we support ear file containing bundles.
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public ReadableArchive getParentArchive() {
        return null;
    }

    @Override
    public URI getEntryURI(final String entryName) {
        try {
            return bundle.getEntry(entryName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDistanceFromTop() {
        // this is the top level archive
        return "";
    }

    @Override
    public Iterator<BundleResource> iterator() {
        return new BundleResourceIterator();
    }

    /**
     * Get the archive input stream.
     *
     * @return a Jar format InputStream for this bundle's content
     * @throws java.io.IOException if an error occurs
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public InputStream getInputStream() throws IOException {
        // [TangYong]fixing GLASSFISH-19662
        if (uri != null && !new File(uri).isDirectory()) {
            return uri.toURL().openStream();
        } else {
            // create a JarOutputStream on the fly from the bundle's content
            // Can we optimize by reading off Felix's cache? Investigate in
            // future.
            PipedInputStream is = new PipedInputStream();
            final PipedOutputStream os = new PipedOutputStream(is);
            new Thread() {
                @Override
                public void run() {
                    try {
                        JarOutputStream jos = new JarOutputStream(os, getManifest());
                        ByteBuffer buf = ByteBuffer.allocate(1024);
                        for (String s : Collections.list(entries())) {
                            if (s.equals(JarFile.MANIFEST_NAME)) {
                                continue; // we have already inserted manifest
                            }
                            jos.putNextEntry(new JarEntry(s));
                            if (!isDirectory(s)) {
                                InputStream in = getEntry(s);
                                try {
                                    JarHelper.copy(in, jos, buf);
                                } finally {
                                    try {
                                        in.close();
                                    } catch (IOException e) {
                                        // ignore
                                    }
                                }
                            }
                            jos.closeEntry();
                        }
                        jos.close();
                        os.close();
                    } catch (IOException e) {
                        // TODO(Sahoo): Proper Exception Handling
                        throw new RuntimeException(e);
                    }
                }
            }.start();
            return is;
        }
    }

    /**
     * A directory (typically a bundle class-path) in the bundle represented as an archive.
     */
    private class EmbeddedDirectoryArchive extends AbstractReadableArchive implements ReadableArchive, URIable {

        /**
         * This is the entry name by which this is identified in the bundle space.
         */
        private final String distanceFromTop;

        /**
         * Create a new instance.
         *
         * @param dst the distance from the top level archive
         */
        EmbeddedDirectoryArchive(final String dst) {
            this.distanceFromTop = dst;
        }

        @Override
        public InputStream getEntry(final String entryName) throws IOException {
            if (!exists(entryName)) {
                return null;
            }
            String bundleEntry = distanceFromTop + entryName;
            return bundle.getEntry(bundleEntry).openStream();
        }

        @Override
        public boolean exists(final String entryName) {
            return OSGiBundleArchive.this.exists(distanceFromTop + entryName);
        }

        @Override
        public long getEntrySize(final String entryName) {
            return OSGiBundleArchive.this.getEntrySize(distanceFromTop + entryName);
        }

        @Override
        public void open(final URI entryUri) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableArchive getSubArchive(final String entryName) throws IOException {

            return null;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public boolean renameTo(final String newName) {
            return false;
        }

        @Override
        public void setParentArchive(final ReadableArchive parentArchive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableArchive getParentArchive() {
            return OSGiBundleArchive.this;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public Enumeration<String> entries() {
            return entries("");
        }

        @Override
        public Enumeration<String> entries(final String prefix) {
            Collection<String> entries = new ArrayList<>();
            getEntryPaths(entries, distanceFromTop + prefix);

            // entries contains path names which is with respect to bundle
            // root.
            // We need names with respect to this directory root.
            // So, we need to strip entryName from the entries.
            Collection<String> subEntries = stripEntryName(entries);
            return Collections.enumeration(subEntries);
        }

        /**
         * This method strips off entryName from collection of entries.
         *
         * @param entries the initial collection of entries
         * @return collection of entry name
         */
        private Collection<String> stripEntryName(final Collection<String> entries) {

            Collection<String> subEntries = new ArrayList<>(entries.size());
            final int idx = distanceFromTop.length();
            for (String entry : entries) {
                subEntries.add(entry.substring(idx));
            }
            return subEntries;
        }

        @Override
        public Collection<String> getDirectories() throws IOException {
            return stripEntryName(getSubDirectories(distanceFromTop));
        }

        @Override
        public boolean isDirectory(final String entryName) {
            String zeEntryName;
            if (entryName.endsWith("/")) {
                zeEntryName = entryName;
            } else {
                zeEntryName = entryName + "/";
            }
            return exists(zeEntryName);
        }

        @Override
        public Manifest getManifest() throws IOException {
            return null; // TODO(Sahoo): Not Yet Implemented
        }

        @Override
        public URI getURI() {
            try {
                return bundle.getEntry(distanceFromTop).toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long getArchiveSize() throws SecurityException {
            return 0;
        }

        @Override
        public String getName() {
            return distanceFromTop;
        }

        @Override
        public URI getEntryURI(final String entryName) {
            return OSGiBundleArchive.this.getEntryURI(distanceFromTop + entryName);
        }

        @Override
        public String getDistanceFromTop() {
            return distanceFromTop;
        }
    }

    /**
     * A jar (typically a bundle class-path) in the bundle represented as an archive.
     */
    private final class EmbeddedJarArchive extends AbstractReadableArchive implements URIable {

        /**
         * This is the entry name by which this is identified in the bundle space.
         */
        private String distanceFromTop;

        /**
         * All the entries that this archive has.
         */
        private List<String> entries = new ArrayList<>();

        /**
         * Create a new instance.
         *
         * @param dst the distance from top
         * @throws IOException if the an error occurs
         */
        private EmbeddedJarArchive(final String dst) throws IOException {
            this.distanceFromTop = dst;
            ZipInputStream zis = getZIS();
            try {
                while (true) {
                    ZipEntry ze = zis.getNextEntry();
                    if (ze == null) {
                        break;
                    }
                    entries.add(ze.getName());
                }
            } finally {
                closeZIS(zis);
            }
        }

        /**
         * Get the zip input stream.
         *
         * @return ZipInputStream
         * @throws IOException if an error occurs
         */
        private ZipInputStream getZIS() throws IOException {
            // Since user can supply random entry and ask for an embedded
            // archive, propagate the exception to user.
            return new ZipInputStream(bundle.getEntry(distanceFromTop).openStream());
        }

        /**
         * Get the entries.
         *
         * @return collection of entry path
         */
        private Collection<String> getEntries() {
            return entries;
        }

        @Override
        public InputStream getEntry(final String entryName) throws IOException {

            if (!exists(entryName)) {
                return null;
            }
            final ZipInputStream zis = getZIS();
            while (true) {
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    // end of stream, which is unlikely because the
                    // entry exists.
                    break;
                }
                if (ze.getName().equals(entryName)) {
                    return zis;
                }
            }
            // don't close the stream, as we are returning it to caller
            assert false;
            return null;
        }

        @Override
        public boolean exists(final String entryName) {
            return getEntries().contains(entryName);
        }

        @Override
        public long getEntrySize(final String entryName) {
            if (exists(entryName)) {
                ZipInputStream zis = null;
                try {
                    zis = getZIS();
                    while (true) {
                        ZipEntry ze = zis.getNextEntry();
                        if (entryName.equals(ze.getName())) {
                            return ze.getSize();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (zis != null) {
                        closeZIS(zis);
                    }
                }
            }
            return 0;
        }

        @Override
        public void open(final URI entryUri) throws IOException {
            throw new UnsupportedOperationException("Not Applicable");
        }

        @Override
        public ReadableArchive getSubArchive(final String entryName) throws IOException {
            // Only one level embedding allowed in a bundle
            return null;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean delete() {
            // TODO(Sahoo): Not Yet Implemented
            return false;
        }

        @Override
        public boolean renameTo(final String entryName) {
            // TODO(Sahoo): Not Yet Implemented
            return false;
        }

        @Override
        public void setParentArchive(final ReadableArchive parentArchive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableArchive getParentArchive() {
            return OSGiBundleArchive.this;
        }

        @Override
        public void close() throws IOException {
            // noop
        }

        @Override
        public Enumeration<String> entries() {
            return Collections.enumeration(getEntries());
        }

        @Override
        public Enumeration<String> entries(final String prefix) {
            List<String> result = new ArrayList<>();
            for (String entry : getEntries()) {
                if (entry.startsWith(prefix)) {
                    result.add(entry);
                }
            }
            return Collections.enumeration(result);
        }

        @Override
        public Collection<String> getDirectories() throws IOException {
            List<String> result = new ArrayList<>();
            for (String entry : getEntries()) {
                final int idx = entry.indexOf('/');
                if (idx != -1 && idx == entry.length() - 1) {
                    result.add(entry);
                }
            }
            return result;
        }

        @Override
        public boolean isDirectory(final String entryName) {
            // directory entries always end with "/", so unless we append a
            // "/" when not there, we are not going
            // to find it in our entry list.
            String zeEntryName;
            if (entryName.endsWith("/")) {
                zeEntryName = entryName;
            } else {
                zeEntryName = entryName + "/";
            }
            return exists(zeEntryName);
        }

        @Override
        public Manifest getManifest() throws IOException {
            String manifestName = JarFile.MANIFEST_NAME;
            if (exists(manifestName)) {
                return new Manifest(getEntry(manifestName));
            }
            return null;
        }

        @Override
        public URI getURI() {
            try {
                return bundle.getEntry(distanceFromTop).toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long getArchiveSize() throws SecurityException {
            // unknown
            return 0;
        }

        @Override
        public String getName() {
            return distanceFromTop;
        }

        @Override
        public URI getEntryURI(final String entryName) {
            return URI
                    .create(EmbeddedJarURLStreamHandlerService.EMBEDDED_JAR_SCHEME + ":" + getURI() + EmbeddedJarURLStreamHandlerService.SEPARATOR + entryName);
        }

        @Override
        public String getDistanceFromTop() {
            return distanceFromTop;
        }

        /**
         * Close the given zip input stream.
         *
         * @param zis stream to close
         */
        private void closeZIS(final ZipInputStream zis) {
            try {
                zis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Iterator of bundle resource.
     */
    private final class BundleResourceIterator implements Iterator<BundleResource> {

        /**
         * Constant for the dot character.
         */
        private static final String DOT = ".";

        /**
         * The delegate iterator.
         */
        private final Iterator<BundleResource> delegate;

        /**
         * All bundle resources.
         */
        private final Collection<BundleResource> bundleResources = new ArrayList<>();

        /**
         * Create a new instance.
         */
        private BundleResourceIterator() {
            // for each bundle classpath entry, get the subarchive
            String bcp = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_CLASSPATH);
            if (bcp == null || bcp.isEmpty()) {
                bcp = DOT;
            }
            String seps = ";,";
            StringTokenizer bcpes = new StringTokenizer(bcp, seps);
            List<ReadableArchive> archives = new ArrayList<>();
            while (bcpes.hasMoreTokens()) {
                String bcpe = bcpes.nextToken();
                bcpe = bcpe.trim();
                if (bcpe.startsWith("/")) {
                    // it is always relative to bundle root
                    bcpe = bcpe.substring(1);
                }
                if (bcpe.equals(DOT)) {
                    archives.add(OSGiBundleArchive.this);
                } else {
                    if (isDirectory(bcpe) && !bcpe.endsWith("/")) {
                        bcpe = bcpe.concat("/");
                    }
                    try {
                        ReadableArchive archive = getSubArchive(bcpe);
                        if (archive != null) {
                            archives.add(archive);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace(); // ignore and continue
                    }
                }
            }

            for (ReadableArchive archive : archives) {
                Enumeration<String> entries = archive.entries();
                final URIable urIable = URIable.class.cast(archive);
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement();
                    URI entryUri = urIable.getEntryURI(entry);
                    final String archivePath = urIable.getDistanceFromTop();
                    BundleResource bundleResource = new BundleResource(entryUri, entry, archivePath);
                    bundleResources.add(bundleResource);
                }
            }
            delegate = bundleResources.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public BundleResource next() {
            return delegate.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * URL handler for embedded JAR URL support.
     */
    static final class EmbeddedJarURLStreamHandlerService extends AbstractURLStreamHandlerService {

        /**
         * URI scheme used for resource embedded in a jar in a bundle.
         */
        static final String EMBEDDED_JAR_SCHEME = "embeddedjar";

        /**
         * Separator used by embedded jar scheme.
         */
        static final String SEPARATOR = "!/";

        @Override
        public URLConnection openConnection(final URL entryUri) throws IOException {

            assert entryUri.getProtocol().equals(EMBEDDED_JAR_SCHEME);
            try {
                String schemeSpecificPart = entryUri.toURI().getSchemeSpecificPart();
                int idx = schemeSpecificPart.indexOf(SEPARATOR);
                assert idx > 0;
                URL embeddedURL = URI.create(schemeSpecificPart.substring(0, idx)).toURL();
                final URLConnection con = embeddedURL.openConnection();
                final String entryPath = schemeSpecificPart.substring(idx + 2);
                assert entryPath.length() > 0;
                return new URLConnection(entryUri) {

                    @Override
                    public void connect() throws IOException {
                        con.connect();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        JarInputStream jis = new JarInputStream(con.getInputStream());
                        for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry()) {

                            if (je.getName().equals(entryPath)) {
                                return jis;
                            }
                        }
                        throw new IOException("No entry by name " + entryPath);
                    }
                };
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
