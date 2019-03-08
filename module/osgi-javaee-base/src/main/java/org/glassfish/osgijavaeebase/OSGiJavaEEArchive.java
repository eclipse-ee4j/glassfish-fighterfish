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

import com.sun.enterprise.deploy.shared.AbstractReadableArchive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is a very important class in our implementation of hybrid applications.
 * This class maps a bundle and its attached fragments to Java EE archive
 * format.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class OSGiJavaEEArchive extends AbstractReadableArchive implements ReadableArchive {

    // TODO(Sahoo): Lazy population of entries
    protected Bundle host;
    protected Bundle[] fragments;
    private Map<String, ArchiveEntry> entries = new HashMap<String, ArchiveEntry>();
    protected final static String JAR_EXT = ".jar";
    protected static final String DOT = ".";
    protected final Map<Bundle, OSGiBundleArchive> archives;

    public OSGiJavaEEArchive(Bundle[] fragments, Bundle host) {
        this.fragments = fragments != null ? fragments : new Bundle[0];
        archives = new HashMap<Bundle, OSGiBundleArchive>(
                this.fragments.length + 1);
        this.host = host;
        init();
        // ensure that we replace the MANIFEST.MF by host's manifest. If host
        // does not have a manifest, then this archive will also not have a
        // manifest.
        final URI hostManifestURI = getArchive(host)
                .getEntryURI(JarFile.MANIFEST_NAME);
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

    protected final synchronized OSGiBundleArchive getArchive(Bundle b) {
        OSGiBundleArchive archive = archives.get(b);
        if (archive == null) {
            archive = new OSGiBundleArchive(b);
            archives.put(b, archive);
        }
        return archive;
    }

    protected final Map<String, ArchiveEntry> getEntries() {
        return entries;
    }

    protected abstract void init();

    protected EffectiveBCP getEffectiveBCP() {
        EffectiveBCPBuilder builder = new EffectiveBCPBuilder();
        builder.createForHost();
        for (Bundle f : fragments) {
            builder.createForFragment(f);
        }
        return builder.build();
    }

    @Override
    public InputStream getEntry(String name) throws IOException {
        final ArchiveEntry archiveEntry = entries.get(name);
        return archiveEntry != null ? archiveEntry.getInputStream() : null;
    }

    @Override
    public boolean exists(String name) throws IOException {
        return entries.containsKey(name);
    }

    @Override
    public long getEntrySize(String name) {
        // can't determine
        return 0;
    }

    @Override
    public void open(URI uri) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReadableArchive getSubArchive(String name) throws IOException {
        //TODO(Sahoo): Not Yet Implemented
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
        return false; // can't rename
    }

    @Override
    public void setParentArchive(ReadableArchive parentArchive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReadableArchive getParentArchive() {
        return null;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public Enumeration<String> entries() {
        final Enumeration<String> all = Collections
                .enumeration(entries.keySet());

        // return only file entries as per the conract of this method
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
                    if (!s.endsWith("/")) { // not a directory entry
                        return s;
                    }
                }
                return null;
            }
        };
    }

    @Override
    public Enumeration<String> entries(final String prefix) {
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
    public Collection<String> getDirectories() throws IOException {
        Collection<String> dirEntries = new ArrayList<String>();
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
    public boolean isDirectory(String name) {
        // TODO(Sahoo): Check if this is correct.
        return name.endsWith("/");
    }

    @Override
    public Manifest getManifest() throws IOException {
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
    public URI getURI() {
        // this represents a collection, so return null
        return null;
    }

    @Override
    public long getArchiveSize() throws SecurityException {
        return 0;
    }

    @Override
    public String getName() {
        return getArchive(host).getName();
    }

    protected interface ArchiveEntry {

        String getName();

        URI getURI() throws URISyntaxException;

        InputStream getInputStream() throws IOException;
    }

    protected interface BCPEntry {

        /**
         * @return path relative to its bundle.
         */
        String getName();

        /**
         * @return the bundle this entry belongs to. Please note, a host bundle
         * can insert a class-path entry into a fragment bundle.
         */
        Bundle getBundle();

        void accept(OSGiJavaEEArchive.BCPEntry.BCPEntryVisitor visitor);

        interface BCPEntryVisitor {

            void visitDir(OSGiJavaEEArchive.DirBCPEntry bcpEntry);

            void visitJar(OSGiJavaEEArchive.JarBCPEntry bcpEntry);
        }
    }

    protected static class DirBCPEntry implements BCPEntry {

        private final String name;
        private final Bundle bundle;

        public DirBCPEntry(String name, Bundle bundle) {
            this.name = name;
            this.bundle = bundle;
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
        public void accept(BCPEntryVisitor visitor) {
            visitor.visitDir(this);
        }
    }

    protected static class JarBCPEntry implements BCPEntry {

        private final String name;
        private final Bundle bundle;

        public JarBCPEntry(String name, Bundle bundle) {
            this.name = name;
            this.bundle = bundle;
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
        public void accept(BCPEntryVisitor visitor) {
            visitor.visitJar(this);
        }
    }

    protected class EffectiveBCP {

        private final List<BCPEntry> bcpEntries = new ArrayList<BCPEntry>();

        public List<BCPEntry> getBCPEntries() {
            return bcpEntries;
        }

        public void accept(BCPEntry.BCPEntryVisitor visitor) {
            for (BCPEntry bcpEntry : getBCPEntries()) {
                bcpEntry.accept(visitor);
            }
        }

        public void add(BCPEntry bcpEntry) {
            bcpEntries.add(bcpEntry);
        }

    }

    class EffectiveBCPBuilder {

        private final EffectiveBCP result = new EffectiveBCP();

        public EffectiveBCP build() {
            return result;
        }

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
         * @param bundle fragment bundle
         */
        void createForFragment(Bundle bundle) {
            for (String s : tokenizeBCP(bundle)) {
                s = s.trim();
                OSGiBundleArchive archive = getArchive(bundle);
                if (DOT.equals(s)) {
                    result.add(createJarBCPEntry(DOT, bundle));
                } else if (archive.exists(s)) {
                    if (archive.isDirectory(s)) {
                        if (!s.endsWith("/")) {
                            // This ensures that entries from subarchive
                            // won't have leading /
                            s = s.concat("/");
                        }
                        result.add(createDirBCPEntry(s, bundle));
                    } else {
                        result.add(createJarBCPEntry(s, bundle));
                    }
                }
            }

        }

        private JarBCPEntry createJarBCPEntry(String entryPath,
                Bundle bundle) {

            return new JarBCPEntry(entryPath, bundle);
        }

        private DirBCPEntry createDirBCPEntry(String entryPath,
                Bundle bundle) {

            return new DirBCPEntry(entryPath, bundle);
        }

        /**
         * Parses Bundle-ClassPath of a bundle and returns it as a sequence of
         * String tokens.
         */
        private String[] tokenizeBCP(Bundle b) {
            String bcp = (String) b.getHeaders()
                    .get(org.osgi.framework.Constants.BUNDLE_CLASSPATH);
            if (bcp == null || bcp.isEmpty()) {
                bcp = DOT;
            }
            return bcp.split(";|,");
        }
    }
}
