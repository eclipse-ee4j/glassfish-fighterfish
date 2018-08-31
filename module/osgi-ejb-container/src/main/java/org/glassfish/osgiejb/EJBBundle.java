/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgiejb;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.osgijavaeebase.OSGiBundleArchive;
import org.glassfish.osgijavaeebase.OSGiJavaEEArchive;
import org.glassfish.osgijavaeebase.URIable;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EJBBundle extends OSGiJavaEEArchive {

    public EJBBundle(Bundle[] fragments, Bundle host) {
        super(fragments, host);
    }

    @Override
    protected void init() {
        // Very important implementation note:
        //
        // Now we need to merge individual namespaces represented in each of the the subarchives
        // corresponding to entries in the effetcive bundle classpath.
        // During merging of namespaces, collision can be expected. e.g.:
        // a bundle with BCP: bin, . and having a content tree like this:
        // p/A.class, bin/p/A.class.
        // Actually, there is only name here which is p/A.class, but it appears in both the namespaces.
        // Our collision avoidance strategy is based on how Bundle.getResource() behaves. Since bin
        // appears ahead of . in BCP, bundle.getResource(p/A.class) will return bin/p/A.class.
        // So,our merged namespace must also contain bin/p/A.class.
        // The simplest way to achieve this is to collect entries from the subarchives in the reverse
        // order of bundle classpath and put them into a hasmap with entry name being the key.
        // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=14268
        final EffectiveBCP bcp = getEffectiveBCP();
        List<BCPEntry> bcpEntries = new ArrayList(bcp.getBCPEntries());
        Collections.reverse(bcpEntries);
        for (BCPEntry bcpEntry : bcpEntries) {
            bcpEntry.accept(new BCPEntry.BCPEntryVisitor() {

                public void visitDir(final DirBCPEntry bcpEntry) {
                visitBCPEntry(bcpEntry);
            }

            public void visitJar(final JarBCPEntry bcpEntry) {
                // do special processing for Bundle-ClassPath DOT
                if (bcpEntry.getName().equals(DOT)) {
                    OSGiBundleArchive subArchive = getArchive(bcpEntry.getBundle());
                    addEntriesForSubArchive(subArchive);
                } else {
                    visitBCPEntry(bcpEntry);
                }
            }

            private void visitBCPEntry(BCPEntry bcpEntry) {
                try {
                    final Archive subArchive = getArchive(bcpEntry.getBundle()).getSubArchive(bcpEntry.getName());
                    addEntriesForSubArchive(subArchive);
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
                }
            }

            private void addEntriesForSubArchive(Archive subArchive) {
                final URIable uriableArchive = (URIable) subArchive;
                for (final String subEntry : Collections.list(subArchive.entries())) {
                    ArchiveEntry archiveEntry = new ArchiveEntry() {
                        public String getName() {
                            return subEntry;
                        }

                        public URI getURI() throws URISyntaxException {
                            return uriableArchive.getEntryURI(subEntry);
                        }

                        public InputStream getInputStream() throws IOException {
                            try {
                                return getURI().toURL().openStream();
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
                            }
                        }
                    };
                    getEntries().put(archiveEntry.getName(), archiveEntry);
                }
            }

        });
        }
    }
}
