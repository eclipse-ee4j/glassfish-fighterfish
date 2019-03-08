/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import static org.glassfish.osgijavaeebase.Constants.FILE_PROTOCOL;
import static org.glassfish.osgijavaeebase.Constants.REFERENCE_PROTOCOL;
import org.osgi.framework.Bundle;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import com.sun.enterprise.deploy.shared.AbstractReadableArchive;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Adapts a {@link Bundle} to {@link Archive}. It uses JAR File space of the
 * bundle (via getEntry and getEntryPaths APIs), so a bundle does not have to be
 * in resolved state. Since it represents JAR File space of the bundle, it does
 * not consider resources from any fragments.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiBundleArchive extends AbstractReadableArchive
        implements URIable, Iterable<BundleResource> {


    private final Bundle bundle;
    private String name;
    private URI uri;
    private final Map<String, ReadableArchive> subArchives =
            new HashMap<String, ReadableArchive>();

    public OSGiBundleArchive(Bundle bundle) {
        this.bundle = bundle;
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
                    location = URLDecoder.decode(location,
                            Charset.defaultCharset().name());
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }

                // Return iff referenced file exists.
                File file = new File(location.substring(
                        FILE_PROTOCOL.length()));
                if (file.exists()) {
                    uri = file.toURI();
                }
            }
        }

        // See issue #10536. We can't use the same policy for obtaining
        // the name as OSGi container does.
//        if (uri != null) {
//            name = Util.getURIName(uri);
//        } else {
        // See if there is a symbolic name & version. Use them,
        // else use location. Either symbolic name or location must exist
        // in a bundle.
        String symName = bundle.getSymbolicName();
        String version = (String) bundle.getHeaders().get(BUNDLE_VERSION);
        if (symName != null) {
            name = version == null
                    ? symName : symName.concat("_").concat(version);
        } else {
            name = location;
        }
//        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Enumeration<String> entries() {
        ArrayList<String> entries = new ArrayList<String>();
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
     * Returns the enumeration of first level directories in this archive
     *
     * @return enumeration of directories under the root of this archive
     * @throws java.io.IOException
     */
    @Override
    public Collection<String> getDirectories() throws IOException {
        return getSubDiretcories("/");
    }

    /**
     * Return subdirectories under a given path. This returns only result from
     * one level, i.e., non-recursive
     *
     * @param path
     * @return
     */
    private Collection<String> getSubDiretcories(String path) {
        final Enumeration firstLevelEntries = bundle.getEntryPaths(path);
        if (firstLevelEntries == null) {
            return Collections.emptyList();
        }
        Collection<String> firstLevelDirs = new ArrayList<String>();
        while (firstLevelEntries.hasMoreElements()) {
            String firstLevelEntry = (String) firstLevelEntries.nextElement();
            if (firstLevelEntry.endsWith("/")) {
                firstLevelDirs.add(firstLevelEntry);
            }
        }
        return firstLevelDirs;
    }

    private void getEntryPaths(Collection<String> entries, String path) {
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
        // BECAUSE OF A BUG IN FELIX (FELIX-1210), THE CODE ABOVE DOES NOT WORK
        // WHEN THERE ARE NO DIRECTORY ENTRIES IN THE JAR FILE.
        // IF WE CONSISTENTLY FACE THE ISSUE, THEN WE CAN USE AN ALTERNATIVE IMPL
        // BASED ON findEntries.
        // OF COURSE, IT WILL HAVE THE UNDESIRED SIDE EFFECT OF FINDINDG ENTRIES
        // FROM FRAGMENTS AS WELL.
        // WE HAVE NASTY SIDE EFFECTS WHEN THAT HAPPENS. e.g. NPE. SO, WE DON'T
        // USE THE ALTERNATIVE
        // IMPLEMENTATION ANY MORE. WE EXPECT JAR TO HAVE PROPER DIRECTORY
        // ENTRIES.
        // getEntryPaths2(entries, path); // call the new implementation
    }

    private void getEntryPaths2(Collection<String> entries, String path) {
        // findEntries expect the path to begin with "/"
        Enumeration e = bundle.findEntries(
                path.startsWith("/") ? path : "/".concat(path), "*", true);
        if (e != null) {
            while (e.hasMoreElements()) {
                URL next = (URL) e.nextElement();
                String nextPath = next.getPath();
                // As per the OSGi R4 spec,
                // "The getPath method for a bundle entry URL must return
                // an absolute path (a path that starts with '/') to a resource
                // or entry in a bundle. For example, the URL returned from
                // getEntry("myimages/test .gif ") must have a path of
                // /myimages/test.gif.
                entries.add(nextPath.substring(1)); // remove the leading "/"
            }
        }
    }

    @Override
    public Enumeration<String> entries(String prefix) {
        Collection<String> entries = new ArrayList<String>();
        getEntryPaths(entries, prefix);
        return Collections.enumeration(entries);
    }

    @Override
    public boolean isDirectory(String name) {
        return bundle.getEntry(name.endsWith("/") ? name : name + "/") != null;
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
     * It returns URI for the underlying file if it can locate such a file.
     * Else, it returns null.
     *
     * @return
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
    public InputStream getEntry(String name) throws IOException {
        URL entry = bundle.getEntry(name);
        return entry != null ? entry.openStream() : null;
    }

    @Override
    public boolean exists(String name) {
        return bundle.getEntry(name) != null;
    }

    @Override
    public long getEntrySize(String name) {
        return 0;
    }

    @Override
    public void open(URI uri) throws IOException {
        throw new UnsupportedOperationException("Not applicable method");
    }

    @Override
    public ReadableArchive getSubArchive(String name) throws IOException {
        if (!exists(name)) {
            return null;
        }
        synchronized (this) {
            if (!subArchives.containsKey(name)) {
                ReadableArchive subArchive
                        = isDirectory(name) ?
                        new EmbeddedDirectoryArchive(name)
                        : new EmbeddedJarArchive(name);
                subArchives.put(name, subArchive);
            }
            return subArchives.get(name);
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
    public boolean renameTo(String name) {
        return false;
    }

    @Override
    public void setParentArchive(ReadableArchive parentArchive) {
        // Not needed until we support ear file containing bundles.
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public ReadableArchive getParentArchive() {
        return null;
    }

    @Override
    public URI getEntryURI(String name) {
        try {
            return bundle.getEntry(name).toURI();
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
     *
     * @return a Jar format InputStream for this bundle's content
     * @throws java.io.IOException
     */
    public InputStream getInputStream() throws IOException {
        //[TangYong]fixing GLASSFISH-19662  
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
                        JarOutputStream jos = new JarOutputStream(os,
                                getManifest());
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
     * A directory (typically a bundle classpath) in the bundle represented as
     * an archive.
     */
    private class EmbeddedDirectoryArchive extends AbstractReadableArchive
            implements ReadableArchive, URIable {

        /**
         * This is the entry name by which this is identified in the bundle
         * space.
         */
        private final String distanceFromTop;

        public EmbeddedDirectoryArchive(String distanceFromTop) {
            this.distanceFromTop = distanceFromTop;
        }

        @Override
        public InputStream getEntry(String name) throws IOException {
            if (!exists(name)) {
                return null;
            }
            String bundleEntry = distanceFromTop + name;
            return bundle.getEntry(bundleEntry).openStream();
        }

        @Override
        public boolean exists(String name) {
            return OSGiBundleArchive.this.exists(distanceFromTop + name);
        }

        @Override
        public long getEntrySize(String name) {
            return OSGiBundleArchive.this.getEntrySize(distanceFromTop + name);
        }

        @Override
        public void open(URI uri) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableArchive getSubArchive(String name) throws IOException {
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
        public boolean renameTo(String name) {
            return false;
        }

        @Override
        public void setParentArchive(ReadableArchive parentArchive) {
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
        public Enumeration<String> entries(String prefix) {
            Collection<String> entries = new ArrayList<String>();
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
         * @param entries
         * @return
         */
        private Collection<String> stripEntryName(Collection<String> entries) {
            Collection<String> subEntries = new ArrayList<String>(
                    entries.size());
            final int idx = distanceFromTop.length();
            for (String entry : entries) {
                subEntries.add(entry.substring(idx));
            }
            return subEntries;
        }

        @Override
        public Collection<String> getDirectories() throws IOException {
            return stripEntryName(getSubDiretcories(distanceFromTop));
        }

        @Override
        public boolean isDirectory(String name) {
            return exists(name.endsWith("/") ? name : name + "/");
        }

        @Override
        public Manifest getManifest() throws IOException {
            return null;  //TODO(Sahoo): Not Yet Implemented
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
        public URI getEntryURI(String name) {
            return OSGiBundleArchive.this.getEntryURI(distanceFromTop + name);
        }

        @Override
        public String getDistanceFromTop() {
            return distanceFromTop;
        }
    }

    /**
     * A jar (typically a bundle class-path) in the bundle represented as an
     * archive.
     */
    private class EmbeddedJarArchive extends AbstractReadableArchive
            implements URIable {

        /**
         * This is the entry name by which this is identified in the bundle
         * space.
         */
        private String distanceFromTop;

        /**
         * All the entries that this archive has
         */
        private List<String> entries = new ArrayList<String>();

        private EmbeddedJarArchive(String distanceFromTop) throws IOException {
            this.distanceFromTop = distanceFromTop;
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

        private ZipInputStream getZIS() throws IOException {
            // Since user can supply random entry and ask for an embedded
            // archive, propagate the exception to user.
            return new ZipInputStream(bundle.getEntry(distanceFromTop).openStream());
        }

        private Collection<String> getEntries() {
            return entries;
        }

        @Override
        public InputStream getEntry(String name) throws IOException {
            if (!exists(name)) {
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
                if (ze.getName().equals(name)) {
                    return zis;
                }
            }
            // don't close the stream, as we are returning it to caller
            assert (false);
            return null;
        }

        @Override
        public boolean exists(String name) {
            return getEntries().contains(name);
        }

        @Override
        public long getEntrySize(String name) {
            if (exists(name)) {
                ZipInputStream zis = null;
                try {
                    zis = getZIS();
                    while (true) {
                        ZipEntry ze = zis.getNextEntry();
                        if (name.equals(ze.getName())) {
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
        public void open(URI uri) throws IOException {
            throw new UnsupportedOperationException("Not Applicable");
        }

        @Override
        public ReadableArchive getSubArchive(String name) throws IOException {
            return null;  // Only one level embedding allowed in a bundle
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean delete() {
            //TODO(Sahoo): Not Yet Implemented
            return false;
        }

        @Override
        public boolean renameTo(String name) {
            //TODO(Sahoo): Not Yet Implemented
            return false;
        }

        @Override
        public void setParentArchive(ReadableArchive parentArchive) {
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
        public Enumeration<String> entries(String prefix) {
            List<String> result = new ArrayList<String>();
            for (String entry : getEntries()) {
                if (entry.startsWith(prefix)) {
                    result.add(entry);
                }
            }
            return Collections.enumeration(result);
        }

        @Override
        public Collection<String> getDirectories() throws IOException {
            List<String> result = new ArrayList<String>();
            for (String entry : getEntries()) {
                final int idx = entry.indexOf('/');
                if (idx != -1 && idx == entry.length() - 1) {
                    result.add(entry);
                }
            }
            return result;
        }

        @Override
        public boolean isDirectory(String name) {
            // directory entries always end with "/", so unless we append a
            // "/" when not there, we are not going
            // to find it in our entry list.
            return exists(name.endsWith("/") ? name : (name + "/"));
        }

        @Override
        public Manifest getManifest() throws IOException {
            String name = JarFile.MANIFEST_NAME;
            return exists(name) ? new Manifest(getEntry(name)) : null;
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
            return 0; // unknown
        }

        @Override
        public String getName() {
            return distanceFromTop;
        }

        @Override
        public URI getEntryURI(String name) {
            return URI.create(EmbeddedJarURLStreamHandlerService
                    .EMBEDDED_JAR_SCHEME + ":" + getURI()
                    + EmbeddedJarURLStreamHandlerService.SEPARATOR + name);
        }

        @Override
        public String getDistanceFromTop() {
            return distanceFromTop;
        }

        private void closeZIS(ZipInputStream zis) {
            try {
                zis.close();
            } catch (Exception e) {
            }
        }
    }

    private class BundleResourceIterator implements Iterator<BundleResource> {

        private static final String DOT = ".";
        private final Iterator<BundleResource> delegate;
        private final Collection<BundleResource> bundleResources =
                new ArrayList<BundleResource>();

        private BundleResourceIterator() {
            // for each bundle classpath entry, get the subarchive
            String bcp = (String) bundle.getHeaders().get(
                    org.osgi.framework.Constants.BUNDLE_CLASSPATH);
            if (bcp == null || bcp.isEmpty()) {
                bcp = DOT;
            }
            String seps = ";,";
            StringTokenizer bcpes = new StringTokenizer(bcp, seps);
            List<ReadableArchive> archives = new ArrayList<ReadableArchive>();
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
                    URI uri = urIable.getEntryURI(entry);
                    final String archivePath = urIable.getDistanceFromTop();
                    BundleResource bundleResource = new BundleResource(uri,
                            entry, archivePath);
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

    static class EmbeddedJarURLStreamHandlerService extends
            AbstractURLStreamHandlerService {

        /**
         * URI scheme used for resource embedded in a jar in a bundle
         */
        static final String EMBEDDED_JAR_SCHEME = "embeddedjar";

        /**
         * Separator used by embedded jar scheme.
         */
        static final String SEPARATOR = "!/";

        @Override
        public URLConnection openConnection(URL u) throws IOException {
            assert (u.getProtocol().equals(EMBEDDED_JAR_SCHEME));
            try {
                String schemeSpecificPart = u.toURI().getSchemeSpecificPart();
                int idx = schemeSpecificPart.indexOf(SEPARATOR);
                assert (idx > 0);
                URL embeddedURL = URI.create(schemeSpecificPart.substring(0,
                        idx)).toURL();
                final URLConnection con = embeddedURL.openConnection();
                final String entryPath = schemeSpecificPart.substring(idx + 2);
                assert (entryPath.length() > 0);
                return new URLConnection(u) {

                    @Override
                    public void connect() throws IOException {
                        con.connect();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        JarInputStream jis = new JarInputStream(
                                con.getInputStream());
                        for (JarEntry je = jis.getNextJarEntry(); je != null;
                                je = jis.getNextJarEntry()) {

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
