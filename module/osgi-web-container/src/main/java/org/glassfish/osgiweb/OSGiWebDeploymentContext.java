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
package org.glassfish.osgiweb;

import com.sun.enterprise.module.common_impl.CompositeEnumeration;
import org.apache.naming.resources.WebDirContext;
import org.glassfish.osgijavaeebase.OSGiArchiveHandler;
import org.glassfish.osgijavaeebase.OSGiDeploymentContext;
import org.glassfish.osgijavaeebase.BundleClassLoader;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.OpsParams;
import org.osgi.framework.Bundle;

import java.io.FileFilter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is at the heart of WAB support. It is responsible for setting up a class
 * loader for the WAB. In theory, a WAB's class loader should just be a simple
 * wrapper around the Bundle object, but in truth we need to take care of all
 * the special requirements mostly around resource finding logic to ensure a WAB
 * behaves like a WAR in our web container. So, we create a special class loader
 * called {@link org.glassfish.osgiweb.OSGiWebDeploymentContext.WABClassLoader}
 * and set that in the deployment context.
 */
class OSGiWebDeploymentContext extends OSGiDeploymentContext {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            OSGiWebDeploymentContext.class.getPackage().getName());

    /**
     * We don't package our custom providers as a META-INF/services/, for doing
     * so will make them visible to non hybrid applications as well. So, we
     * package it at a different location and punch in our class-loader
     * appropriately. This map holds the key name that client is looking for and
     * the value is where we have placed it in our bundle.
     */
    private static final Map<String, String> HIDDEN_SERVICES_MAP;

    /**
     * Since mojarra uses thread's context class loader to look up custom
     * providers and our custom providers are not available via APIClassLoader's
     * META-INF/service punch-in mechanism, we need to make them visible
     * specially. This field maintains a list of such service class names. As
     * much as we would like to hide
     * {@link org.glassfish.osgiweb.OSGiWebModuleDecorator}, we can't, because
     * that's looked up via habitat, which means it has to be either present as
     * META-INF/services in the bundle itself or added as an existing
     * inhabitant. We have gone for the latter approach for the decorator. The
     * other providers that are looked up by mojarra are hidden using the
     * technique implemented here.
     */
    private static final Collection<String> HIDDEN_SERVICES;

    //CHECKSTYLE:OFF
    static {
        Map<String, String> map = new HashMap<String, String>();

        // This is for the custom AnnotationProvider. Note that Mojarra
        // surprising uses different nomenclature than what is used by JDK SPI.
        // The service type is AnnotationProvider, yet it looks for
        // annotationprovider.
        map.put("META-INF/services/com.sun.faces.spi.annotationprovider",
                "META-INF/hiddenservices/com.sun.faces.spi.annotationprovider");

        // This is for our custom faces-config.xml discoverer
        map.put("META-INF/services/com.sun.faces.spi.FacesConfigResourceProvider",
                "META-INF/hiddenservices/com.sun.faces.spi.FacesConfigResourceProvider");

        // This is for our custom taglib.xml discoverer
        map.put("META-INF/services/com.sun.faces.spi.FaceletConfigResourceProvider",
                "META-INF/hiddenservices/com.sun.faces.spi.FaceletConfigResourceProvider");
        HIDDEN_SERVICES_MAP = Collections.unmodifiableMap(map);

        HIDDEN_SERVICES = Collections.unmodifiableList(Arrays.asList(
                OSGiFacesAnnotationScanner.class.getName(),
                OSGiFaceletConfigResourceProvider.class.getName(),
                OSGiFacesConfigResourceProvider.class.getName()
        ));
    }
    //CHECKSTYLE:ON

    /**
     * Create a new instance.
     * @param actionReport GlassFish command reporter
     * @param logger logger
     * @param source  Application archive
     * @param params Deploy command parameters
     * @param env GlassFish server environment
     * @param bundle Application bundle
     * @throws Exception if an error occurs
     */
    OSGiWebDeploymentContext(final ActionReport actionReport,
            final Logger logger, final ReadableArchive source,
            final OpsParams params, final ServerEnvironment env,
            final Bundle bundle) throws Exception {

        super(actionReport, logger, source, params, env, bundle);
        // ArchiveHandler must correctly return the ArchiveType for DOL
        // processing to succeed,
        setArchiveHandler(new OSGiArchiveHandler() {
            @Override
            public List<URI> getClassPathURIs(final ReadableArchive archive) {
                final List<URI> uris = new ArrayList<URI>();
                File base = getSourceDir();
                assert (base != null && base.isDirectory());
                uris.add(new File(base, "WEB-INF/classes/").toURI());
                new File(base, "WEB-INF/lib/").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(final File pathname) {
                        if (pathname.isFile()
                                && pathname.getName().endsWith(".jar")) {
                            uris.add(pathname.toURI());
                        }
                        return false;
                    }
                });
                OSGiWebDeploymentContext.LOGGER.logp(Level.INFO,
                        "OSGiWebDeploymentContext", "getClassPathURIs",
                        "uris = {0}", new Object[]{uris});
                return uris;
            }

            @Override
            public String getArchiveType() {
                // Since I am not able to reference GF 4.0 APIs as they are not
                // yet staged in a maven repo,
                // I am accessing the value in a round about way.
                // WarType.ARCHIVE_TYPE;
                return javax.enterprise.deploy.shared.ModuleType.WAR
                        .toString();
            }
        });
    }

    @Override
    protected void setupClassLoader() throws Exception {
        ClassLoader cl = new WABClassLoader(null);
        setFinalClassLoader(cl);
        setShareableTempClassLoader(cl);
        WebappClassLoader.class.cast(cl).start();
    }

    /**
     * We need this class loader for variety of reasons explained below: a)
     * GlassFish default servlet (DefaultServlet.java), the servlet responsible
     * for serving static content fails to serve any static content from
     * META-INF/resources/ of WEB-INF/lib/*.jar, if the class-loader is not an
     * instance of WebappClassLoader. b) DefaultServlet also expects
     * WebappClassLoader's resourceEntries to be properly populated. c) JSPC
     * relies on getURLs() methods so that it can discover TLDs in the web app.
     * Setting up repositories and jar files ensures that WebappClassLoader's
     * getURLs() method will return appropriate URLs for JSPC to work. d) set a
     * specialized FileDirContext object that restricts access to OSGI-INF and
     * OSGI-OPT resources of a WAB as required by the OSGi WAB spec.
     *
     * It overrides loadClass(), getResource() and getResources() as opposed to
     * their findXYZ() equivalents so that the OSGi export control mechanism is
     * enforced even for classes and resources available in the system/boot
     * class loader. The only time this class loader is defining class loader
     * for some classes is when this class loader is used by containers like CDI
     * or EJB to define generated classes.
     */
    private class WABClassLoader extends WebappClassLoader {

        /**
         * First delegate class-loader.
         */
        private final BundleClassLoader delegate1 =
                new BundleClassLoader(getBundle());

        /**
         * Second delegate class-loader.
         */
        private final ClassLoader delegate2 = Globals
                .get(ClassLoaderHierarchy.class).getAPIClassLoader();

        @Override
        public Class<?> loadClass(final String name)
                throws ClassNotFoundException {

            return loadClass(name, false);
        }

        @Override
        protected synchronized Class<?> loadClass(final String name,
                final boolean resolve) throws ClassNotFoundException {

            // this class loader may be the defining loader for a proxy or
            // generated class
            Class c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // mojarra uses Thread's context class loader (which is us) to look
            // up custom annotation provider.
            // since we don't export our package and in fact hide our provider,
            // we need to load them using current loader.
            if (HIDDEN_SERVICES.contains(name)) {
                return Class.forName(name);
            }
            try {
                return delegate1.loadClass(name, resolve);
            } catch (ClassNotFoundException cnfe) {
                return delegate2.loadClass(name);
            }
        }

        @Override
        public URL getResource(final String name) {
            URL url = delegate1.getResource(name);
            if (url == null) {
                url = delegate2.getResource(name);
            }
            return url;
        }

        @Override
        public Enumeration<URL> getResources(final String name)
                throws IOException {

            List<Enumeration<URL>> enumerators
                    = new ArrayList<Enumeration<URL>>();
            final String mappedResourcePath = HIDDEN_SERVICES_MAP.get(name);
            if (mappedResourcePath != null) {
                return getClass().getClassLoader()
                        .getResources(mappedResourcePath);
            }
            enumerators.add(delegate1.getResources(name));
            enumerators.add(delegate2.getResources(name));
            return new CompositeEnumeration(enumerators);
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            // We need to override this method because of the stupid
            // WebappClassLoader that for some reason not only overrides
            // getResourceAsStream, it also does not delegate to getResource
            // method.
            URL url = getResource(name);
            try {
                if (url != null) {
                    return url.openStream();
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * Create a new instance.
         * @param parent the parent class-loader
         */
        WABClassLoader(final ClassLoader parent) {
            super(parent);
            // we always delegate. The default is false in WebappClassLoader!
            setDelegate(true);

            File base = getSourceDir();
            // Let's install a customized dir context that does not allow static
            // contents from OSGI-OPT and OSGI-INF directories as required by
            // the OSGi WAB spec.
            WebDirContext r = new OSGiWebDirContext();
            r.setDocBase(base.getAbsolutePath());
            setResources(r);

            // add WEB-INF/classes/ and WEB-INF/lib/*.jar to repository list,
            // because many legacy code path like DefaultServlet, JSPC,
            // StandardContext rely on them.
            // See WebappLoader.setClassPath() for example.
            addRepository("WEB-INF/classes/", new File(base,
                    "WEB-INF/classes/"));
            File libDir = new File(base, "WEB-INF/lib");
            if (libDir.exists()) {
                int baseFileLen = base.getPath().length();
                for (File file : libDir.listFiles(
                        new FileFilter() {
                    @Override
                    public boolean accept(final File pathname) {
                        String fileName = pathname.getName();
                        return (fileName.endsWith(".jar")
                                && pathname.isFile());
                    }
                })) {
                    try {
                        addJar(file.getPath().substring(baseFileLen),
                                new JarFile(file), file);
                        // jarFile is closed by WebappClassLoader.stop()
                    } catch (Exception e) {
                        // Catch and ignore any exception in case the JAR file
                        // is empty.
                    }
                }
            }
            // We set the same working dir as set in WarHandler
            setWorkDir(getScratchDir("jsp"));
        }
    }
}
