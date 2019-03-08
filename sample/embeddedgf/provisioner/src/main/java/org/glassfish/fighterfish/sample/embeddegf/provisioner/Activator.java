/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.embeddegf.provisioner;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple bundle that can download GlassFish zip from a URL specified using {@link GLASSFISH_ARCHIVE_URL}
 * into a location specified in {@link INSTALLATION_DIR} property. It then bootstraps GlassFish in the same
 * JVM.
 */
public class Activator implements BundleActivator {

    private final String GLASSFISH_ARCHIVE_URL = "fighterfish.provisioner.url";
    private final String INSTALLATION_DIR = "fighterfish.provisioner.destination";

    private File dest;
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            String out = context.getProperty(INSTALLATION_DIR);
            if (out != null) {
                dest = new File(out);
            } else {
                dest = context.getDataFile(""); // get base dir
            }
            if (needToExplode()) {
                if(dest.mkdirs());
                explode(context);
            }
            startGlassFishBundle(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void startGlassFishBundle(BundleContext context) throws BundleException {
        Bundle bundle = context.installBundle(new File(dest, "glassfish5/glassfish/modules/glassfish.jar").toURI().toString());
        System.setProperty("com.sun.aas.installRoot", new File(dest, "glassfish5/glassfish/").getAbsolutePath());
        System.setProperty("com.sun.aas.instanceRoot", new File(dest, "glassfish5/glassfish/domains/domain1/").getAbsolutePath());
        bundle.start(Bundle.START_TRANSIENT);
    }

    private boolean needToExplode() throws Exception {
        if (new File(dest, "glassfish3").isDirectory()) {
            return false;
        }
        if (dest.isFile()) {
            throw new Exception(dest.getAbsolutePath() + " is a file");
        }
        return true;
    }

    private void explode(BundleContext context) throws Exception {
        String in = context.getProperty(GLASSFISH_ARCHIVE_URL);
        logger.info("Provisioning URL = " + in);
        if (in != null) {
            URL url = new URL(in);
            logger.info("Opening stream");
            InputStream is = url.openStream();
            ZipInputStream zis = new ZipInputStream(url.openStream());
            try {
                extractZip(zis);
            } finally {
                zis.close();
            }
        } else {
            throw new Exception("Pl specify GlassFish archive URL in a property called " + GLASSFISH_ARCHIVE_URL);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    public void extractZip(ZipInputStream zis) throws IOException {
        logger.logp(Level.INFO, "Activator", "extractZip", "dest = {0}", new Object[]{dest});
        ZipEntry ze;
        int n = 0;
        int size = 0;
        while ((ze = zis.getNextEntry()) != null) {
            logger.logp(Level.FINER, "Activator", "extractZip", "ZipEntry name = {0}, size = {1}", new Object[]{ze.getName(), ze.getSize()});
            java.io.File f = new java.io.File(dest + java.io.File.separator + ze.getName());
            if (ze.isDirectory()) {
                if (!f.exists()) {
                    if (!f.mkdirs()) {
                        throw new IOException("Unable to create dir " + f.getAbsolutePath());
                    }
                } else if (f.isFile()) { // exists, but not a file. not sure how this can happen
                    throw new IOException("Unable to create dir " + f.getAbsolutePath() + " because it already exists as a file.");
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
                logger.logp(Level.FINER, "Activator", "extractZip", "totalcount for this zip entry = {0}", new Object[]{totalcount});
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
        logger.logp(Level.INFO, "Activator", "extractZip", "Extracted {0} of entries of total size {1} bytes.", new Object[]{n, size});

    }

}
