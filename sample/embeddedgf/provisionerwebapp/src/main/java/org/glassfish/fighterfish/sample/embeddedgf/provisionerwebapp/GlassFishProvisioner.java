/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.embeddedgf.provisionerwebapp;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Main entry point.
 */
@WebListener
public final class GlassFishProvisioner implements ServletContextListener {

    /**
     * Servlet context.
     */
    private ServletContext servletContext;

    /**
     * OSGi framework.
     */
    private volatile Framework framework;

    /**
     * GlassFish service.
     */
    private volatile GlassFish glassfish;

    /**
     * Executor service.
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * OSGi framework JNDI name.
     */
    private String fwJndiName = FW_JNDI_NAME_DEFAULT;

    /**
     * GlassFish default JNDI name.
     */
    private String gfJndiName = GF_JNDI_NAME_DEFAULT;

    /**
     * GlassFish install home.
     */
    private String gfHome;

    /**
     * OSGi framework JNDI default name.
     */
    private static final String FW_JNDI_NAME_DEFAULT = "java:global/glassfish-osgi-framework";

    /**
     * GlassFish default JNDI name.
     */
    private static final String GF_JNDI_NAME_DEFAULT = "java:global/glassfish-instance";

    /**
     * Property name for the GlassFish install root directory.
     */
    private static final String GLASSFISH_INSTALL_ROOT_PROP = "com.sun.aas.installRoot";

    /**
     * GlassFish main bundle.
     */
    private Bundle gfMainBundle;

    /**
     * Set the GlassFish JNDI name.
     *
     * @param jndiName JNDI name
     */
    @Resource
    public void setGfJndiName(final String jndiName) {
        this.gfJndiName = jndiName;
    }

    /**
     * Set the OSGi framework JNDI name.
     *
     * @param jndiName JNDI name
     */
    @Resource
    public void setFwJndiName(final String jndiName) {
        this.fwJndiName = jndiName;
    }

    /**
     * Set the GlassFish install home.
     *
     * @param home new install home
     */
    @Resource
    public void setGfHome(final String home) {
        this.gfHome = home;
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {

        this.servletContext = servletContextEvent.getServletContext();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    waitForFramework();
                    provisionGlassFish();
                    waitForGlassFish();
                    new InitialContext().rebind(gfJndiName, glassfish);
                    log("bound " + glassfish + " in JNDI location: " + gfJndiName);
                } catch (InterruptedException e) {
                    log("got interrupted: ", e);
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    log("Something has gone wrong while provisioning" + " GlassFish.", e);
                }
            }
        });
        log("Submitted task");
    }

    /**
     * Wait for the OSGi framework to come up.
     *
     * @throws ExecutionException if an error occurs
     * @throws InterruptedException if an error occurs
     */
    private void waitForFramework() throws ExecutionException, InterruptedException {

        log("waiting for OSGi framework");
        framework = new WaitForOSGiFrameworkTask().call();
    }

    /**
     * Wait for GlassFish to be up.
     *
     * @throws InterruptedException if an error occurs
     * @throws ExecutionException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void waitForGlassFish() throws InterruptedException, ExecutionException {

        log("waiting for GlassFish");
        ServiceTracker st = new ServiceTracker(framework.getBundleContext(), GlassFish.class.getName(), null);
        st.open();
        try {
            glassfish = (GlassFish) st.waitForService(0);
        } finally {
            st.close();
        }
        new WaitForGlassFishToStart(glassfish).call();
    }

    /**
     * Provision GlassFish.
     *
     * @throws Exception if an error occurs
     */
    private void provisionGlassFish() throws Exception {
        BundleContext bctx = framework.getBundleContext();
        if (gfHome == null) {
            gfHome = bctx.getProperty("com.sun.aas.installRoot");
        }
        if (gfHome == null) {
            throw new RuntimeException("Please set GlassFish home either by setting a property" + " called " + GLASSFISH_INSTALL_ROOT_PROP
                    + " either in the system or in OSGi properties file.\n" + "Alternatively, you can set it using runtime deployment"
                    + " descriptor or deployment plan while deploying" + " this war file.");
        }
        log("Going to provision GlassFish bundles from " + gfHome);
        File jar = new File(gfHome, "modules" + File.separator + "glassfish.jar");
        if (!jar.exists()) {
            throw new Exception(jar.getAbsolutePath() + " does not exist. Check what you have set as " + GLASSFISH_INSTALL_ROOT_PROP);
        }
        URL url = jar.toURI().toURL();
        this.log("Installing bundle [" + url + "]");
        gfMainBundle = bctx.installBundle(url.toExternalForm());
        try {
            // start transiently to have more control over the lifecycle
            gfMainBundle.start(Bundle.START_TRANSIENT);
        } catch (BundleException e) {
            log("Failed to start " + gfMainBundle, e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {

        log("Shutting down the executor so that it can cancel any" + " pending tasks");
        executorService.shutdownNow();
        if (gfMainBundle != null) {
            try {
                gfMainBundle.stop();
            } catch (BundleException e) {
                log("Error while stopping glassfish main bundle " + gfMainBundle, e);
            }
        }
        glassfish = null;
    }

    /**
     * Waits for OSGi framework to be available in JNDI.
     */
    private class WaitForOSGiFrameworkTask implements Callable<Framework> {

        @Override
        @SuppressWarnings("checkstyle:MagicNumber")
        public Framework call() throws InterruptedException {
            while (true) {
                try {
                    Framework fw = (Framework) new InitialContext().lookup(fwJndiName);
                    log("obtained " + fw);
                    return fw;
                } catch (NamingException e) {
                    log("This is sometimes expected: ", e);
                }
                Thread.sleep(1000);
            }
        }

    }

    /**
     * Waits for GlassFish to start.
     */
    private final class WaitForGlassFishToStart implements Callable<Void> {

        /**
         * GlassFish service.
         */
        private final GlassFish gf;

        /**
         * Create a new instance.
         *
         * @param glassFish glassFish service
         */
        WaitForGlassFishToStart(final GlassFish glassFish) {
            this.gf = glassFish;
        }

        @Override
        @SuppressWarnings("checkstyle:MagicNumber")
        public Void call() throws InterruptedException {
            try {
                // Poll for GlassFish to start. GlassFish service might have
                // been registered by
                // GlassFishRuntime.newGlassFish() and hence might not be
                // ready to use.
                GlassFish.Status status = gf.getStatus();
                while (status != GlassFish.Status.STARTED && status != GlassFish.Status.DISPOSED) {
                    Thread.sleep(1000);
                }
                if (status != GlassFish.Status.STARTED) {
                    log("status = " + status);
                    throw new RuntimeException("GlassFish didn't start properly");
                }

            } catch (GlassFishException e) {
                // TODO(Sahoo): Proper Exception Handling
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    /**
     * Log a message using the servlet context.
     *
     * @param msg message to log
     */
    private void log(final String msg) {
        servletContext.log("provisionerwebapp: " + msg);
    }

    /**
     * Log a message using the servlet context.
     *
     * @param msg message to log
     * @param ex exception
     */
    private void log(final String msg, final Throwable ex) {
        servletContext.log("provisionerwebapp: " + msg, ex);
    }
}
