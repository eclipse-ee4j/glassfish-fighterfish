/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.obrbuilder.xmlentities.ObrXmlReaderWriter;
import org.glassfish.obrbuilder.xmlentities.Repository;

import java.io.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Simple program which does the following:
 * 1) Reads obr.xml from a specified file.
 * 2) Reads OSGi bundle metadata for all bundles from a specified directory tree.
 * 3) Updates the OBR with the bundle information.
 * 4) Saves the new data in the same obr.xml. 
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length != 2) {
            System.out.println("Usage: java " + Main.class + " <directory containing OSGi bundles> <path to obr.xml>");
            return;
        }
        File dir = new File(args[0]);
        File obrXML = new File(args[1]);
        final ObrXmlReaderWriter obrParser = new ObrXmlReaderWriter();
        final Repository obr = obrXML.exists() ? obrParser.read(obrXML.toURI()) : new Repository();
        dir.listFiles(new FileFilter(){
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                } else {
                    if(isBundle(pathname)) {
                        processBundle(obr, pathname);
                    }
                }
                return false;
            }
        });
        obrParser.write(obr, obrXML);
    }

    private static void processBundle(Repository obr, File bundle) {
        System.out.println("bundle = " + bundle.getAbsolutePath());
        // TODO(Sahoo): Add bundle details to obr
    }

    private static boolean isBundle(File file) {
        // Existence of any of these artifact is considered a bundle
        String headersToCheck[] = {"Bundle-ManifestVersion",
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
                if (attrs.getValue(header) != null) return true;
            }
            jar.close();
        } catch (IOException e) {
            // ignore and continue
        }
        return false;
    }

}
