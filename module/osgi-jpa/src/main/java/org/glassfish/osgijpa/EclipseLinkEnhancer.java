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

package org.glassfish.osgijpa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.osgijavaeebase.BundleClassLoader;
import org.glassfish.osgijavaeebase.JarHelper;
import org.glassfish.osgijavaeebase.OSGiArchiveHandler;
import org.glassfish.osgijavaeebase.OSGiBundleArchive;
import org.glassfish.osgijpa.dd.Persistence;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;

/**
 * Enhancer for EclipseLink.
 */
final class EclipseLinkEnhancer implements JPAEnhancer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EclipseLinkEnhancer.class.getPackage().getName());

    /**
     * GlassFish archive factory.
     */
    private final ArchiveFactory archiveFactory = Globals.get(ArchiveFactory.class);

    /**
     * Eclipse package.
     */
    private static final String EL_PKG = "org.eclipse.persistence.*";

    @Override
    public InputStream enhance(final Bundle bnd, final List<Persistence> persistenceXMLs) throws IOException {

        // We need to explode the bundle if it is not a directory based
        // deployment.
        // This is because, eclipselink enhancer can only scan file system
        // artifacts.
        File explodedDir = makeFile(bnd);
        boolean dirDeployment;
        if (explodedDir != null) {
            dirDeployment = explodedDir.isDirectory();
        } else {
            dirDeployment = false;
        }
        try {
            if (!dirDeployment) {
                explodedDir = explode(bnd);
            }

            // We need to make a copy of the exploded direactory where the
            // enhanced bytes will be written to.
            final File enhancedDir = makeTmpDir("enhanced-osgiapp");
            FileUtils.copyTree(explodedDir, enhancedDir);

            ClassLoader cl = new BundleClassLoader(bnd);

            for (Persistence persistenceXML : persistenceXMLs) {
                String puRoot = persistenceXML.getPURoot();
                File source = new File(explodedDir, puRoot);
                File target = new File(enhancedDir, puRoot);
                try {
                    enhance(source, target, cl, persistenceXML);
                    // TODO(Sahoo): Update persistence.xml
                    // with eclipselink.weaving=static
                } catch (URISyntaxException e) {
                    // TODO(Sahoo): Proper Exception Handling
                    throw new RuntimeException(e);
                }
            }
            updateManifest(new File(enhancedDir, JarFile.MANIFEST_NAME));
            return JarHelper.makeJar(enhancedDir, new Runnable() {
                @Override
                public void run() {
                    if (FileUtils.whack(enhancedDir)) {
                        LOGGER.logp(Level.INFO, "EclipseLinkEnhancer", "enhance", "Deleted {0} ", new Object[] { enhancedDir });
                    } else {
                        LOGGER.logp(Level.INFO, "EclipseLinkEnhancer", "enhance", "Unable to delete {0} ", new Object[] { enhancedDir });
                    }
                }
            });
        } finally {
            if (!dirDeployment) {
                if (FileUtils.whack(explodedDir)) {
                    LOGGER.logp(Level.INFO, "EclipseLinkEnhancer", "enhance", "Deleted {0} ", new Object[] { explodedDir });
                } else {
                    LOGGER.logp(Level.WARNING, "EclipseLinkEnhancer", "enhance", "Unable to delete " + explodedDir);
                }
            }
        }
    }

    /**
     * Do the actual enhancement work.
     *
     * @param source the file to enhance
     * @param target the target file to create
     * @param cl the class-loader to use
     * @param persistenceXML the JPA persistence.xml
     * @throws IOException if an IO error occurs
     * @throws URISyntaxException if an error occurs
     */
    private void enhance(final File source, final File target, final ClassLoader cl, final Persistence persistenceXML) throws IOException, URISyntaxException {

        LOGGER.logp(Level.INFO, "EclipseLinkEnhancer", "enhance", "Source = {0}, Target = {1}", new Object[] { source, target });
        StaticWeaveProcessor proc = new StaticWeaveProcessor(source, target);
        proc.setClassLoader(cl);
        proc.performWeaving();
    }

    /**
     * Update the given manifest file.
     *
     * @param mf manifest file
     * @throws IOException if an error occurs
     */
    private void updateManifest(final File mf) throws IOException {
        Manifest m = new Manifest();
        FileInputStream is = new FileInputStream(mf);
        try {
            m.read(is);
        } finally {
            is.close();
        }
        String value = m.getMainAttributes().getValue(Constants.DYNAMICIMPORT_PACKAGE);
        if (value != null) {
            // TODO(Sahoo): Don't add if org.eclipselink.* is already specified
            value = value.concat(", " + EL_PKG);
        } else {
            value = EL_PKG;
        }
        m.getMainAttributes().putValue(Constants.DYNAMICIMPORT_PACKAGE, value);

        // Mark the bundle as weaved to avoid infinite updates
        m.getMainAttributes().putValue(JPABundleProcessor.STATICALLY_WEAVED, "true");
        FileOutputStream os = new FileOutputStream(mf);
        try {
            m.write(os);
        } finally {
            os.close();
        }
    }

    /**
     * Creates a temporary directory with the given prefix. It marks the directory for deletion upon shutdown of the JVM.
     *
     * @param prefix prefix for the temporary directory
     * @return File representing the directory just created
     * @throws IOException if it fails to create the directory
     */
    public static File makeTmpDir(final String prefix) throws IOException {
        File tmpDir = File.createTempFile(prefix, "");

        // create a directory in place of the tmp file.
        tmpDir.delete();
        tmpDir = new File(tmpDir.getAbsolutePath());
        tmpDir.deleteOnExit();
        if (tmpDir.mkdirs()) {
            return tmpDir;
        } else {
            throw new IOException("Not able to create tmpdir " + tmpDir);
        }
    }

    /**
     * Return a File object that corresponds to this bundle. return null if it can't determine the underlying file object.
     *
     * @param bnd the bundle
     * @return File
     */
    public static File makeFile(final Bundle bnd) {
        try {
            return new File(new OSGiBundleArchive(bnd).getURI());
        } catch (Exception e) {
            // Ignore if we can't convert
        }
        return null;
    }

    /**
     * Explode the given bundle to a directory.
     *
     * @param bnd bundle
     * @return File
     * @throws IOException if an error occurs
     */
    private File explode(final Bundle bnd) throws IOException {
        File explodedDir = makeTmpDir("osgiapp");
        WritableArchive targetArchive = archiveFactory.createArchive(explodedDir);
        new OSGiArchiveHandler().expand(new OSGiBundleArchive(bnd), targetArchive, null);
        LOGGER.logp(Level.INFO, "EclipseLinkEnhancer", "explode", "Exploded bundle {0} at {1} ", new Object[] { bnd, explodedDir });
        return explodedDir;
    }
}
