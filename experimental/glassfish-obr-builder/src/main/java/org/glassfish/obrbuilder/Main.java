/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.obrbuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.glassfish.obrbuilder.xmlentities.ObrXmlReaderWriter;
import org.glassfish.obrbuilder.xmlentities.Repository;

import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Simple program which does the following:
 * 1) Reads obr.xml from a specified file.
 * 2) Reads OSGi bundle metadata for all bundles from a specified directory
 * tree.
 * 3) Updates the OBR with the bundle information.
 * 4) Saves the new data in the same obr.xml.
 */
public final class Main {

    /**
     * Cannot be instanciated.
     */
    private Main() {
    }

    /**
     * Main entry point.
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java " + Main.class
                    + " <directory containing OSGi bundles>"
                    + " <path to obr.xml>");
            return;
        }
        File dir = new File(args[0]);
        File obrXML = new File(args[1]);
        final ObrXmlReaderWriter obrParser = new ObrXmlReaderWriter();
        final Repository obr;
        if (obrXML.exists()) {
            obr = obrParser.read(obrXML.toURI());
        } else {
            obr = new Repository();
        }
        dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                } else {
                    if (isBundle(pathname)) {
                        processBundle(obr, pathname);
                    }
                }
                return false;
            }
        });
        obrParser.write(obr, obrXML);
    }

    /**
     * Process a bundle.
     * @param obr repository
     * @param bundle file for the bundle to process
     */
    private static void processBundle(final Repository obr,
            final File bundle) {

        System.out.println("bundle = " + bundle.getAbsolutePath());
        // TODO(Sahoo): Add bundle details to obr
    }

    /**
     * Test if the given file is a bundle.
     * @param file file to test
     * @return {@code true} if the file is a bundle, {@code false} otherwise
     */
    private static boolean isBundle(final File file) {
        // Existence of any of these artifact is considered a bundle
        String[] headersToCheck = {"Bundle-ManifestVersion",
                "Bundle-SymbolicName",
                "Bundle-Version",
                "Export-Package",
                "Import-Package",
                "DynamicImport-Package"
        };
        try {
            JarFile jar = new JarFile(file);
            final Attributes attrs = jar.getManifest().getMainAttributes();
            for (String header : headersToCheck) {
                if (attrs.getValue(header) != null) {
                    return true;
                }
            }
            jar.close();
        } catch (IOException e) {
            // ignore and continue
        }
        return false;
    }
}
