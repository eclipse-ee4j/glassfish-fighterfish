/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.embeddedgf.webosgibridge;


import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sanjeeb.sahoo@oracle.com
 */
public class OSGiFrameworkProvisioner {

    private Framework framework;
    private final ServletContext servletContext;

    /**
     * JNDI name used to (re)bind the OSGi framework instance
     * Default value is {@value #FW_JNDI_NAME_DEFAULT}.
     * It can be overridden using an environment value injection or deployment plan.
     *
     * @see WebappMain#setOsgiFrameworkConfigFilePath(String)
     */
    private String osgiFrameWorkJndiName = FW_JNDI_NAME_DEFAULT;

    /**
     * Name of the properties file used for OSGi framework configuration.
     * The default value is {@value #FW_CONFIG_FILE_DEFAULT} relative the current working directory.
     * It can be overridden using an environment value injection or deployment plan.
     * If the configured file is not found, then the properties file that's embedded inside this war
     * at {@value #EMBEDDED_FW_PROP_FILE} is used.
     *
     * @see WebappMain#setOsgiFrameworkConfigFilePath(String)
     */
    private String osgiFrameworkConfigFilePath = FW_CONFIG_FILE_DEFAULT;

    /**
     * Default JNDI name used to (re)bind OSGi framework
     */
    static final String FW_JNDI_NAME_DEFAULT = "java:global/glassfish-osgi-framework";

    /**
     * default location of external OSGi configuration file.
     */
    static final String FW_CONFIG_FILE_DEFAULT = "config/osgi.properties";

    /**
     * Location of embedded OSGi configuration file.
     */
    private static final String EMBEDDED_FW_PROP_FILE = "/WEB-INF/osgi.properties";

    /**
     * Location inside the WAR file where framework jar is located.
     */
    private static final String FW_DIR = "/WEB-INF/osgi-framework/";

    /**
     * Location inside the WAR where initial bundles are located.
     */
    private static final String BUNDLES_DIR = "/WEB-INF/bundles/";

    public OSGiFrameworkProvisioner(ServletContext servletContext, String osgiFrameworkConfigFilePath, String osgiFrameWorkJndiName) {
        this.servletContext = servletContext;
        log("OSGiFramework:init(" + osgiFrameworkConfigFilePath + ", " + osgiFrameWorkJndiName + ")");
        setOsgiFrameworkJndiName(osgiFrameWorkJndiName);
        setOsgiFrameworkConfigPath(osgiFrameworkConfigFilePath);
    }

    public void start() {
        log(Bundle.class + " is loaded by " + Bundle.class.getClassLoader());
        try {
            createFramework();
            assert (framework != null);
            framework.init();
            installBundles();
            framework.start();
            // bind to JNDI only after starting the framework so that clients don't have to worry about start level
            // while using the framework.
            bindFrameworkInJndi();
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
        }
    }

    public void stop() {
        try {
            log("OSGiFramework.stop " + "going to stop");
            if (framework != null) {
                framework.stop();
                framework.waitForStop(0);
                framework = null;
                unbindFrameworkFromJndi();
            }
            log("OSGiFramework.stop " + "Stopped");
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
        } catch (BundleException e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
        }
    }

    private void createFramework() {
        final ClassLoader cl = createFrameworkLoader();
        FrameworkFactory ff = null;
        Iterator<FrameworkFactory> frameworkFactoryIterator = ServiceLoader.load(FrameworkFactory.class, cl).iterator();
        while (frameworkFactoryIterator.hasNext()) {
            try {
                ff = frameworkFactoryIterator.next();
                break;
            } catch (ServiceConfigurationError sce) {
                log("This is expected when there are multiple framework factories in classpath.", sce);
            }
        }
        if (ff == null) {
            throw new RuntimeException("Unable to find suitable OSGi framework, so aborting...");
        }
        log("OSGiFramework.start " + ff);
        framework = ff.newFramework(getConfig());
        log("OSGiFramework.start " + framework + " is loaded by " + framework.getClass().getClassLoader());
        // Since WLS 12.1.2 bundles OSGi framework, this assertion is no longer valid
//        assert(cl == framework.getClass().getClassLoader());
    }

    private void bindFrameworkInJndi() {
        try {
            getInitialContext().rebind(osgiFrameWorkJndiName, framework);
            log("The OSGi framework is available under JNDI name: " + osgiFrameWorkJndiName);
        } catch (NamingException e) {
            log("Failed to bind OSGi framework to JNDI", e);
        }
    }

    private void unbindFrameworkFromJndi() {
        try {
            getInitialContext().unbind(osgiFrameWorkJndiName);
        } catch (NamingException e) {
            log("Failed to unbind OSGi framework from JNDI", e);
        }
    }

    private Context getInitialContext() throws NamingException {
        Properties ht = new Properties();
        ht.put("weblogic.jndi.replicateBindings", "false"); // we want this object to be local only.
        return new InitialContext(ht);
    }

    /**
     * @return a class loader capable of finding OSGi frameworks.
     */
    private ClassLoader createFrameworkLoader() {
        // We need to create a URLClassLoader for Felix to be able to attach framework extensions
        List<URL> urls = new ArrayList<URL>();
        for (String s : servletContext.getResourcePaths(FW_DIR)) {
            if (s.endsWith(".jar")) {
                try {
                    urls.add(servletContext.getResource(s));
                } catch (MalformedURLException e) {
                    log("OSGiFramework.createFrameworkLoader got exception while trying to get URL for resource " + s, e);
                }
            }
        }
        log("OSGiFramework.createFrameworkLoader " + urls);
        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
        log("Classloader to find & load framework is: " + cl + " whose parent is: " + cl.getParent());
        return cl;
    }

    private void installBundles() throws Exception {
        BundleContext bctx = framework.getBundleContext();
        ArrayList<Bundle> installed = new ArrayList<Bundle>();
        for (URL url : findBundles()) {
            this.log("Installing bundle [" + url + "]");
            Bundle bundle = bctx.installBundle(url.toExternalForm());
            installed.add(bundle);
        }
        for (Bundle bundle : installed) {
            try {
                bundle.start();
            } catch (BundleException e) {
                log("Failed to start " + bundle, e);
            }
        }

    }

    private List<URL> findBundles()
            throws Exception {
        ArrayList<URL> list = new ArrayList<URL>();
        for (Object o : this.servletContext.getResourcePaths(BUNDLES_DIR)) {
            String name = (String) o;
            if (name.endsWith(".jar")) {
                URL url = this.servletContext.getResource(name);
                if (url != null) {
                    list.add(url);
                }
            }
        }
        return list;
    }

    private void setOsgiFrameworkJndiName(String osgiFrameWorkJndiName) {
        if (osgiFrameWorkJndiName != null) {
            this.osgiFrameWorkJndiName = osgiFrameWorkJndiName;
        }
    }

    private void setOsgiFrameworkConfigPath(String osgiFrameworkConfigFilePath) {
        if (osgiFrameworkConfigFilePath != null) {
            File file = new File(osgiFrameworkConfigFilePath);
            if (file.exists()) {
                osgiFrameworkConfigFilePath = file.getAbsolutePath();
                this.osgiFrameworkConfigFilePath = osgiFrameworkConfigFilePath;
                log("Will use " + osgiFrameworkConfigFilePath + " for reading OSGi configuration.");
            }
        }
    }

    private Map<String, String> getConfig() {
        Properties props = new Properties();
        InputStream in = null;
        if (osgiFrameworkConfigFilePath != null) {
            final File file = new File(osgiFrameworkConfigFilePath);
            try {
                in = new FileInputStream(file);
                log("Reading osgi configuration from : " + file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                log(e.getMessage());
            }
        }
        if (in == null) { // external file is not found, let's default to embedded resource
            in = servletContext.getResourceAsStream(EMBEDDED_FW_PROP_FILE);
            log("Reading osgi configuration from embedded resource: " + EMBEDDED_FW_PROP_FILE);
        }
        try {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
        Util.substVars(props);
        HashMap<String, String> map = new HashMap<String, String>();
        for (Object key : props.keySet()) {
            map.put(key.toString(), (String) props.get(key));
        }
        log("framework configuration: {" + prettyString(map) + "}");
        return map;
    }

    private String prettyString(HashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("\n[") .append(entry).append("]\n");
        }
        return sb.toString();
    }

    private static Logger logger = Logger.getLogger(OSGiFrameworkProvisioner.class.getPackage().getName());

    private void log(String msg) {
        logger.logp(Level.INFO, "OSGiFramework", "log", "OSGiFrameworkProvisioner: " + msg);
    }

    private void log(String msg, Throwable e) {
        logger.logp(Level.WARNING, "OSGiFramework", "log", "OSGiFrameworkProvisioner: " + msg, e);
    }

}
