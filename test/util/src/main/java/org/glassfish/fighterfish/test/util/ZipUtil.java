/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple utility to extract a zip input stream. This is used to install
 * GlassFish when user does not have an installation.
 */
public final class ZipUtil {

    /**
     * Cannot be instanciated.
     */
    private ZipUtil() {
    }

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            ZipUtil.class.getPackage().getName());

    /**
     * Test if a file needs to be a exploded in a directory.
     * @param dest file to test
     * @return {@code true} if needs to be exploded, {@code false} otherwise
     * @throws Exception if an error occurs
     */
    private static boolean needToExplode(final File dest) throws Exception {
        if (new File(dest, "glassfish5").isDirectory()) {
            return false;
        }
        if (dest.isFile()) {
            throw new Exception(dest.getAbsolutePath() + " is a file");
        }
        return true;
    }

    /**
     * Retrieve the content at the given URI and exploded it in the given
     * directory.
     * @param in input
     * @param out output
     * @throws Exception if an error occurs
     */
    public static void explode(final URI in, final File out) throws Exception {
        assert (in != null);
        LOGGER.entering("ZipUtil", "explode", new Object[]{in, out});
        if (!needToExplode(out)) {
            LOGGER.logp(Level.FINE, "ZipUtil", "explode",
                    "Skipping exploding at {0}", new Object[]{out});
        }
        ZipInputStream zis = new ZipInputStream(in.toURL().openStream());
        try {
            extractZip(zis, out);
        } finally {
            zis.close();
        }
    }

    /**
     * Extract the given zip input stream into the given directory.
     * @param zis input stream
     * @param destDir destination directory
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static void extractZip(final ZipInputStream zis, final File destDir)
            throws IOException {

        LOGGER.logp(Level.FINE, "ZipUtil", "extractZip", "destDir = {0}",
                new Object[]{destDir});
        ZipEntry ze;
        int n = 0;
        int size = 0;
        while ((ze = zis.getNextEntry()) != null) {
            LOGGER.logp(Level.FINER, "ZipUtil", "extractZip",
                    "ZipEntry name = {0}, size = {1}",
                    new Object[]{ze.getName(), ze.getSize()});
            java.io.File f = new File(destDir, ze.getName());
            if (!f.toPath().normalize().startsWith(destDir.toPath().normalize())) {
                throw new IOException("Bad zip entry");
            }
            if (ze.isDirectory()) {
                if (!f.exists()) {
                    if (!f.mkdirs()) {
                        throw new IOException("Unable to create dir "
                                + f.getAbsolutePath());
                    }
                } else if (f.isFile()) {
                    // exists, but not a file. not sure how this can happen
                    throw new IOException("Unable to create dir "
                            + f.getAbsolutePath()
                            + " because it already exists as a file.");
                }
                continue;
            } else if (f.exists()) {
                continue;
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                int totalcount = 0;
                int count = 0;
                byte[] buffer = new byte[8192];
                while ((count = zis.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, count);
                    totalcount += count;
                }
                LOGGER.logp(Level.FINER, "ZipUtil", "extractZip",
                        "totalcount for this zip entry = {0}",
                        new Object[]{totalcount});
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            zis.closeEntry();
            n++;
            size += ze.getSize();
        }
        LOGGER.logp(Level.INFO, "ZipUtil", "extractZip",
                "Extracted {0} of entries of total size {1} bytes to {2}.",
                new Object[]{n, size, destDir.getAbsolutePath()});
    }
}
