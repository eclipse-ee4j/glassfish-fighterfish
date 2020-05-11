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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.osgi.framework.Bundle;

import com.sun.enterprise.deploy.shared.AbstractReadableArchive;

/**
 * This is a very important class in our implementation of hybrid applications. This class maps a bundle and its
 * attached fragments to Java EE archive format.
 */
public abstract class OSGiJavaEEArchive extends AbstractReadableArchive implements ReadableArchive {

    // TODO(Sahoo): Lazy population of entries

    /**
     * Constant for JAR file extension.
     */
    protected static final String JAR_EXT = ".jar";

    /**
     * Constant for the dot character.
     */
    protected static final String DOT = ".";

    /**
     * The host bundle.
     */
    private Bundle host;

    /**
     * The bundle fragments.
     */
    private Bundle[] fragments;

    /**
     * The archive entries.
     */
    private Map<String, ArchiveEntry> entries = new HashMap<>();

    /**
     * The bundle archives.
     */
    private final Map<Bundle, OSGiBundleArchive> archives;

    /**
     * Create a new instance.
     *
     * @param frags bundle fragments
     * @param hostBdn host bundle
     */
    public OSGiJavaEEArchive(final Bundle[] frags, final Bundle hostBdn) {

        if (frags != null) {
            this.fragments = frags;
        } else {
            this.fragments = new Bundle[0];
        }
        archives = new HashMap<>(this.fragments.length + 1);
        this.host = hostBdn;
        init();
        // ensure that we replace the MANIFEST.MF by host's manifest. If host
        // does not have a manifest, then this archive will also not have a
        // manifest.
        final URI hostManifestURI = getArchive(hostBdn).getEntryURI(JarFile.MANIFEST_NAME);
        if (hostManifestURI == null) {
            getEntries().remove(JarFile.MANIFEST_NAME);
        } else {
            getEntries().put(JarFile.MANIFEST_NAME, new ArchiveEntry() {

                @Override
                public String getName() {
                    return JarFile.MANIFEST_NAME;
                }

                @Override
                public URI getURI() {
                    return hostManifestURI;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return getURI().toURL().openStream();
                }
            });
        }
    }

    /**
     * Get the host bundle.
     *
     * @return Bundle
     */
    protected Bundle getHost() {
        return host;
    }

    /**
     * Get the bundle fragments.
     *
     * @return Bundle[]
     */
    protected Bundle[] getFragments() {
        return fragments;
    }

    /**
     * Get the archive for a given bundle.
     *
     * @param bnd the bundle for which to get the archive
     * @return OSGiBundleArchive
     */
    protected final synchronized OSGiBundleArchive getArchive(final Bundle bnd) {

        OSGiBundleArchive archive = archives.get(bnd);
        if (archive == null) {
            archive = new OSGiBundleArchive(bnd);
            archives.put(bnd, archive);
        }
        return archive;
    }

    /**
     * Get the archive entries.
     *
     * @return map of entries
     */
    protected final Map<String, ArchiveEntry> getEntries() {
        return entries;
    }

    /**
     * Initialize the archive.
     */
    protected abstract void init();

    /**
     * Get the effective bundle class-path.
     *
     * @return EffectiveBCP
     */
    protected final EffectiveBCP getEffectiveBCP() {
        EffectiveBCPBuilder builder = new EffectiveBCPBuilder();
        builder.createForHost();
        for (Bundle f : fragments) {
            builder.createForFragment(f);
        }
        return builder.build();
    }

    @Override
    public final InputStream getEntry(final String name) throws IOException {
        final ArchiveEntry archiveEntry = entries.get(name);
        if (archiveEntry != null) {
            return archiveEntry.getInputStream();
        }
        return null;
    }

    @Override
    public final boolean exists(final String name) throws IOException {
        return entries.containsKey(name);
    }

    @Override
    public final long getEntrySize(final String name) {
        // can't determine
        return 0;
    }

    @Override
    public final void open(final URI uri) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public final ReadableArchive getSubArchive(final String name) throws IOException {

        // TODO(Sahoo): Not Yet Implemented
        return null;
    }

    @Override
    public final boolean exists() {
        return true;
    }

    @Override
    public final boolean delete() {
        return false;
    }

    @Override
    public final boolean renameTo(final String name) {
        return false; // can't rename
    }

    @Override
    public final void setParentArchive(final ReadableArchive parentArchive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final ReadableArchive getParentArchive() {
        return null;
    }

    @Override
    public final void close() throws IOException {
        // nothing to do
    }

    @Override
    public final Enumeration<String> entries() {
        final Enumeration<String> all = Collections.enumeration(entries.keySet());

        // return only file entries as per the conract of this method
        return new Enumeration<String>() {

            private String next = getNext();

            @Override
            public boolean hasMoreElements() {
                return next != null;
            }

            @Override
            public String nextElement() {
                if (hasMoreElements()) {
                    String result = next;
                    this.next = getNext();
                    return result;
                }
                throw new NoSuchElementException();
            }

            private String getNext() {
                while (all.hasMoreElements()) {
                    String s = all.nextElement();
                    if (!s.endsWith("/")) { // not a directory entry
                        return s;
                    }
                }
                return null;
            }
        };
    }

    @Override
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public final Enumeration<String> entries(final String prefix) {
        final Enumeration<String> all = entries();
        return new Enumeration<String>() {
            String next = getNext();

            @Override
            public boolean hasMoreElements() {
                return next != null;
            }

            @Override
            public String nextElement() {
                if (hasMoreElements()) {
                    String result = next;
                    next = getNext();
                    return result;
                }
                throw new NoSuchElementException();
            }

            private String getNext() {
                while (all.hasMoreElements()) {
                    String s = all.nextElement();
                    if (s.startsWith(prefix)) {
                        return s;
                    }
                }
                return null;
            }
        };
    }

    @Override
    public final Collection<String> getDirectories() throws IOException {
        Collection<String> dirEntries = new ArrayList<>();
        Enumeration<String> all = entries();
        while (all.hasMoreElements()) {
            final String s = all.nextElement();
            if (s.endsWith("/")) {
                dirEntries.add(s);
            }
        }
        return dirEntries;
    }

    @Override
    public final boolean isDirectory(final String name) {
        // TODO(Sahoo): Check if this is correct.
        return name.endsWith("/");
    }

    @Override
    public final Manifest getManifest() throws IOException {
        final InputStream is = getEntry(JarFile.MANIFEST_NAME);
        if (is != null) {
            try {
                return new Manifest(is);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public final URI getURI() {
        // this represents a collection, so return null
        return null;
    }

    @Override
    public final long getArchiveSize() throws SecurityException {
        return 0;
    }

    @Override
    public final String getName() {
        return getArchive(host).getName();
    }

    /**
     * Archive entry.
     */
    protected interface ArchiveEntry {

        /**
         * Get the entry name.
         *
         * @return entry name
         */
        String getName();

        /**
         * Get the entry URI.
         *
         * @return URI
         * @throws URISyntaxException if an error occurs
         */
        URI getURI() throws URISyntaxException;

        /**
         * Get the entry input stream.
         *
         * @return InputStream
         * @throws IOException if an error occurs
         */
        InputStream getInputStream() throws IOException;
    }

    /**
     * Bundle class-path entry.
     */
    protected interface BCPEntry {

        /**
         * Get the entry name.
         *
         * @return path relative to its bundle.
         */
        String getName();

        /**
         * Get the bundle.
         *
         * @return the bundle this entry belongs to. Please note, a host bundle can insert a class-path entry into a fragment
         * bundle.
         */
        Bundle getBundle();

        /**
         * Visit the entry.
         *
         * @param visitor visitor to use
         */
        void accept(OSGiJavaEEArchive.BCPEntry.BCPEntryVisitor visitor);

        /**
         * Visitor interface.
         */
        interface BCPEntryVisitor {

            /**
             * Visit a directory entry.
             *
             * @param bcpEntry entry to visit
             */
            void visitDir(OSGiJavaEEArchive.DirBCPEntry bcpEntry);

            /**
             * Visit a file entry.
             *
             * @param bcpEntry entry to visit
             */
            void visitJar(OSGiJavaEEArchive.JarBCPEntry bcpEntry);
        }
    }

    /**
     * Bundle class-path directory entry.
     */
    protected static final class DirBCPEntry implements BCPEntry {

        /**
         * The entry name.
         */
        private final String name;

        /**
         * The bundle.
         */
        private final Bundle bundle;

        /**
         * Create a new instance.
         *
         * @param entryName the entry name
         * @param bnd the bundle
         */
        public DirBCPEntry(final String entryName, final Bundle bnd) {
            this.name = entryName;
            this.bundle = bnd;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }

        @Override
        public void accept(final BCPEntryVisitor visitor) {
            visitor.visitDir(this);
        }
    }

    /**
     * Bundle class-path file entry.
     */
    protected static final class JarBCPEntry implements BCPEntry {

        /**
         * The entry name.
         */
        private final String name;

        /**
         * The bundle.
         */
        private final Bundle bundle;

        /**
         * Create a new instance.
         *
         * @param entryName the entry name
         * @param bnd the bundle
         */
        public JarBCPEntry(final String entryName, final Bundle bnd) {
            this.name = entryName;
            this.bundle = bnd;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }

        @Override
        public void accept(final BCPEntryVisitor visitor) {
            visitor.visitJar(this);
        }
    }

    /**
     * Effective bundle class-path.
     */
    protected class EffectiveBCP {

        /**
         * List of entries.
         */
        private final List<BCPEntry> bcpEntries = new ArrayList<>();

        /**
         * Get the entries.
         *
         * @return list of BCPEntry
         */
        public List<BCPEntry> getBCPEntries() {
            return bcpEntries;
        }

        /**
         * Visit the entries.
         *
         * @param visitor visitor to use
         */
        public void accept(final BCPEntry.BCPEntryVisitor visitor) {
            for (BCPEntry bcpEntry : getBCPEntries()) {
                bcpEntry.accept(visitor);
            }
        }

        /**
         * Add an entry.
         *
         * @param bcpEntry entry to add
         */
        public void add(final BCPEntry bcpEntry) {
            bcpEntries.add(bcpEntry);
        }
    }

    /**
     * Effective bundle class-path builder.
     */
    class EffectiveBCPBuilder {

        /**
         * The effective bundle class-path.
         */
        private final EffectiveBCP result = new EffectiveBCP();

        /**
         * Get the resulting effective bundle class-path.
         *
         * @return EffectiveBCP
         */
        public EffectiveBCP build() {
            return result;
        }

        /**
         * Create the effective bundle class-path for the host bundle.
         */
        @SuppressWarnings("unchecked")
        void createForHost() {
            List<Bundle> bundles = new ArrayList(Arrays.asList(fragments));
            bundles.add(0, host); // search in host first
            for (String s : tokenizeBCP(host)) {
                s = s.trim();
                for (Bundle b : bundles) {
                    OSGiBundleArchive archive = getArchive(b);
                    if (DOT.equals(s)) {
                        result.add(createJarBCPEntry(DOT, b));
                    } else if (archive.exists(s)) {
                        if (archive.isDirectory(s)) {
                            if (!s.endsWith("/")) {
                                // This ensures that entries from subarchive
                                // won't have leading /
                                s = s.concat("/");
                            }
                            result.add(createDirBCPEntry(s, b));
                        } else {
                            result.add(createJarBCPEntry(s, b));
                        }
                    }
                }
            }
        }

        /**
         * Create the effective bundle class-path for the given fragment.
         *
         * @param bnd fragment bundle
         */
        void createForFragment(final Bundle bnd) {
            for (String s : tokenizeBCP(bnd)) {
                s = s.trim();
                OSGiBundleArchive archive = getArchive(bnd);
                if (DOT.equals(s)) {
                    result.add(createJarBCPEntry(DOT, bnd));
                } else if (archive.exists(s)) {
                    if (archive.isDirectory(s)) {
                        if (!s.endsWith("/")) {
                            // This ensures that entries from subarchive
                            // won't have leading /
                            s = s.concat("/");
                        }
                        result.add(createDirBCPEntry(s, bnd));
                    } else {
                        result.add(createJarBCPEntry(s, bnd));
                    }
                }
            }

        }

        /**
         * Create a new bundle class-path file entry.
         *
         * @param entryPath the path of the entry
         * @param bnd the bundle
         * @return JarBCPEntry
         */
        private JarBCPEntry createJarBCPEntry(final String entryPath, final Bundle bnd) {

            return new JarBCPEntry(entryPath, bnd);
        }

        /**
         * Create a new bundle class-path directory entry.
         *
         * @param entryPath the path of the entry
         * @param bnd the bundle
         * @return DirBCPEntry
         */
        private DirBCPEntry createDirBCPEntry(final String entryPath, final Bundle bnd) {

            return new DirBCPEntry(entryPath, bnd);
        }

        /**
         * Parses Bundle-ClassPath of a bundle and returns it as a sequence of String tokens.
         *
         * @param bnd the bundle
         * @return parsed tokens
         */
        private String[] tokenizeBCP(final Bundle bnd) {
            String bcp = bnd.getHeaders().get(org.osgi.framework.Constants.BUNDLE_CLASSPATH);
            if (bcp == null || bcp.isEmpty()) {
                bcp = DOT;
            }
            return bcp.split(";|,");
        }
    }
}
