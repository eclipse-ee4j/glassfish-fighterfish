/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgiweb;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.osgijavaeebase.OSGiBundleArchive;
import org.glassfish.osgijavaeebase.OSGiJavaEEArchive;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Servlet spec, the spec which defines the term Web Application, defines the overall structure of a Web Application as
 * a hierarchical arrangement of files (and directories), but does not mandate them to be available in a hierarchical
 * file system per se. See section #10.4 of Servlet 3.0 spec, which mentions the following: This specification defines a
 * hierarchical structure used for deployment and packaging purposes that can exist in an open file system, in an
 * archive file, or in some other form. It is recommended, but not required, that servlet containers support this
 * structure as a runtime representation.
 * <p/>
 * A WAB provides such a view of web application which is actually composed of a host OSGi bundle and zero or more
 * attached fragment bundles.
 *
 * Implementation Notes: We don't create virtual jar from directory type Bundle-ClassPath entry, because rfc #66 says
 * that such entries should be treated like WEB-INF/classes/, which means, they must not be searched for
 * web-fragments.xml.
 */
public final class WAB extends OSGiJavaEEArchive {

    /**
     * All Bundle-ClassPath entries of type jars are represented as WEB-INF/lib/{N}.jar, where N is a number starting with
     * 0.
     */
    private static final String LIB_DIR = "WEB-INF/lib/";

    /**
     * Classes directory.
     */
    private static final String CLASSES_DIR = "WEB-INF/classes/";

    /**
     * Create a new instance.
     * 
     * @param host the host bundle.
     * @param fragments the associated bundle fragments
     */
    public WAB(final Bundle host, final Bundle[] fragments) {
        super(fragments, host);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected synchronized void init() {
        List<Bundle> bundles = new ArrayList(Arrays.asList(getFragments()));
        bundles.add(0, getHost());
        for (Bundle b : bundles) {
            final OSGiBundleArchive archive = getArchive(b);
            for (final String entry : Collections.list(archive.entries())) {
                if (getEntries().containsKey(entry)) {
                    continue; // encountering second time - ignore
                }
                ArchiveEntry archiveEntry = new ArchiveEntry() {
                    @Override
                    public String getName() {
                        return entry;
                    }

                    @Override
                    public URI getURI() throws URISyntaxException {
                        return archive.getEntryURI(entry);
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return archive.getEntry(entry);
                    }
                };
                getEntries().put(entry, archiveEntry);
            }
        }

        final EffectiveBCP bcp = getEffectiveBCP();
        bcp.accept(new BCPEntry.BCPEntryVisitor() {
            private int i = 0;

            @Override
            public void visitDir(final DirBCPEntry bcpEntry) {
                try {
                    // do special processing if the dir name is not
                    // WEB-INF/classes/
                    if (bcpEntry.getName().equals(CLASSES_DIR)) {
                        return;
                    }
                    final Archive subArchive = getArchive(bcpEntry.getBundle()).getSubArchive(bcpEntry.getName());
                    for (final String subEntry : Collections.list(subArchive.entries())) {

                        ArchiveEntry archiveEntry = new ArchiveEntry() {
                            @Override
                            public String getName() {
                                return CLASSES_DIR + subEntry;
                            }

                            @Override
                            public URI getURI() throws URISyntaxException {
                                return bcpEntry.getBundle().getEntry(bcpEntry.getName() + subEntry).toURI();
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                try {
                                    return getURI().toURL().openStream();
                                } catch (URISyntaxException e) {
                                    // TODO(Sahoo): Proper Exception Handling
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                        getEntries().put(archiveEntry.getName(), archiveEntry);
                    }
                } catch (IOException e) {
                    // TODO(Sahoo): Proper Exception Handling
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void visitJar(final JarBCPEntry bcpEntry) {
                // do special processing if the jar does not belong to
                // WEB-INF/lib/
                if (bcpEntry.getName().startsWith(LIB_DIR) && bcpEntry.getName().endsWith(JAR_EXT)) {
                    String jarName = bcpEntry.getName().substring(LIB_DIR.length());
                    if (!jarName.contains("/")) {
                        // This jar is already first level jar in WEB-INF/lib
                        return;
                    }
                }

                // do special processing for Bundle-ClassPath DOT
                if (bcpEntry.getName().equals(DOT)) {
                    final String newJarName = LIB_DIR + "Bundle" + bcpEntry.getBundle().getBundleId() + JAR_EXT;
                    getEntries().put(newJarName, new ArchiveEntry() {
                        @Override
                        public String getName() {
                            return newJarName;
                        }

                        @Override
                        public URI getURI() throws URISyntaxException {
                            return getArchive(bcpEntry.getBundle()).getURI();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {

                            return getArchive(bcpEntry.getBundle()).getInputStream();
                        }
                    });
                } else {
                    final String newJarName = LIB_DIR + "Bundle" + bcpEntry.getBundle().getBundleId() + "-" + bcpEntry.getName().replace('/', '-') + JAR_EXT;
                    getEntries().put(newJarName, new ArchiveEntry() {
                        @Override
                        public String getName() {
                            return newJarName;
                        }

                        @Override
                        public URI getURI() throws URISyntaxException {

                            return bcpEntry.getBundle().getEntry(bcpEntry.getName()).toURI();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {

                            try {
                                return getURI().toURL().openStream();
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }
        });
    }
}
