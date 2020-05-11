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
package org.glassfish.osgiweb;

import static com.sun.enterprise.web.Constants.DEFAULT_WEB_MODULE_PREFIX;
import static org.glassfish.osgiweb.Constants.BUNDLE_CONTEXT_ATTR;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.osgijavaeebase.BundleResource;
import org.glassfish.osgijavaeebase.OSGiBundleArchive;
import org.glassfish.web.loader.WebappClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.sun.enterprise.web.WebModule;
import com.sun.enterprise.web.WebModuleDecorator;
import com.sun.faces.spi.ConfigurationResourceProvider;

/**
 * This is where we customize the StandardContext (or WebModule as you call it) object created for a WAB.
 *
 * This class is responsible for the following customizations:
 *
 * a) an attribute called {@link Constants#BUNDLE_CONTEXT_ATTR} in ServletContext of the web app associated with the
 * current OSGi bundle.
 *
 * b) set a specialized FileDirContext object that restricts access to OSGI-INF and OSGI-OPT resources of a WAB as
 * required by the OSGi WAB spec.
 *
 * c) discovering JSF faces config resources and setting them in an attribute called
 * {@link Constants#FACES_CONFIG_ATTR}.
 *
 * d) discovering JSF facelet config resources and setting them in an attribute called
 * {@link Constants#FACELET_CONFIG_ATTR}.
 *
 * e) discovering faces annotations in a WAB and setting them in an attribute called {
 *
 * @@link Constants#FACES_ANNOTATED_CLASSES}
 *
 * The faces related attributes are used by various OSGiFacesXXXProviders that we install for a WAB. Mojarra discovers
 * and calls those providers as part of its initialization.
 *
 * @see org.glassfish.osgiweb.OSGiFacesConfigResourceProvider
 * @see org.glassfish.osgiweb.OSGiFaceletConfigResourceProvider
 * @see org.glassfish.osgiweb.OSGiWebDeploymentContext.WABClassLoader#getResources
 */
public final class OSGiWebModuleDecorator implements WebModuleDecorator {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiWebModuleDecorator.class.getPackage().getName());

    /**
     * Flag to indicate if the module is active.
     */
    private boolean active = true;

    @Override
    public void decorate(final WebModule module) {
        if (isActive()) {
            BundleContext bundleContext = OSGiWebDeploymentRequest.getCurrentBundleContext();
            
            // We can be here when there are no web apps deployed and the first
            // webapp that gets deployed
            // is a WAB. In that case, the default_web_app gets loaded in the
            // same thread that's trying to load
            // the WAB and we end up getting here, because our thread local
            // object contains the WAB's bundle context
            // at this point of time. That's one of the many ugly side effects
            // of using thread locals.
            // So, we need to make sure that we are not customizing the default
            // web modules.
            // Hence we are calling isDefaultWebModule()
            if (bundleContext != null && !isDefaultWebModule(module)) {
                ServletContext servletContext = module.getServletContext();
                servletContext.setAttribute(BUNDLE_CONTEXT_ATTR, bundleContext);
                if (isMojarraPresent()) {
                    populateFacesInformation(module, bundleContext, servletContext);
                }

                // For whatever reason, web container sets resources inside
                // StandardContext.start() if
                // resources is null. So, we have to set it to
                // OSGiWebDirContext in OSGiWebModuleDecorator
                // in addition to setting it in WABClassLoader.
                module.setResources(new OSGiWebDirContext());
            }
        }
    }

    /**
     * Is this a default web web module that's configured in the virtual server to handle '/' context path?
     *
     * @param module the GlassFish web module
     * @return {@code true} if the module is default, {@code false} otherwise
     */
    private boolean isDefaultWebModule(final WebModule module) {
        // Although default web module has a fixed name called
        // {@link com.sun.enterprise.web.Constants#DEFAULT_WEB_MODULE_NAME}
        // that name is not used when user configures a different web app as the
        // default web module. So, we check for the prefix.
        return module.getWebModuleConfig().getName().startsWith(DEFAULT_WEB_MODULE_PREFIX);
    }

    /**
     * Test if mojarra is present. E.g. web profile VS full profile
     *
     * @return {@code true} if mojarra is present, {@code false} otherwise
     */
    private boolean isMojarraPresent() {
        // We don't have a hard dependency on JSF or mojarra in our
        // Import-Package. So, we need to test if mojarra is available or not.
        try {
            return Class.forName(ConfigurationResourceProvider.class.getName()) != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Populate the faces information in the given servlet context.
     *
     * @param module the GlassFish web application module
     * @param bctx the bundle context
     * @param sc the servlet context
     */
    private void populateFacesInformation(final WebModule module, final BundleContext bctx, final ServletContext sc) {

        Collection<URI> facesConfigs = new ArrayList<>();
        Collection<URI> faceletConfigs = new ArrayList<>();
        discoverJSFConfigs(bctx.getBundle(), facesConfigs, faceletConfigs);
        sc.setAttribute(Constants.FACES_CONFIG_ATTR, facesConfigs);
        sc.setAttribute(Constants.FACELET_CONFIG_ATTR, faceletConfigs);
        Map<Class<? extends Annotation>, Set<Class<? extends Object>>> facesAnnotatedClasses = scanFacesAnnotations(module);
        sc.setAttribute(Constants.FACES_ANNOTATED_CLASSES, facesAnnotatedClasses);
    }

    /**
     * Test if the module is active.
     *
     * @return {@code true} if active, {@code false} otherwise
     */
    private synchronized boolean isActive() {
        return active;
    }

    /**
     * De-activate this module.
     */
    synchronized void deActivate() {
        this.active = false;
    }

    /**
     * JSF has two kinds of configuration files, viz: faces configs and facelet configs. While faces configs are identified
     * by a file name faces-config.xml or a file ending with .faces-config.xml in META-INF/, facelet configs are identified
     * by files in META-INF/ having suffix .taglib.xml. Note that facelet configs are never named simply taglib.xml, they
     * must end with .taglib.xml, where as faces configs can be named as faces-config.xml as well as ending with
     * .faces-config.xml.
     *
     * As you can see from the above description, it is a pattern based search. The default config resource providers in
     * mojarra (our JSF implementation layer) is not OSGi aware, so it does not know how to iterate over bundle entries.
     * More over, it does not even know about Archive abstraction that GlassFish deployment backend uses. It relies on web
     * app classloader to return jar or file type urls for resources so that they can walk through the resource hierarchy to
     * find matching resource files. Since, {@link org.glassfish.osgiweb.OSGiWebDeploymentContext.WABClassLoader} does not
     * provide jar or file type URLs for resources, the default providers of mojarra are insufficient for our needs as
     * mentioned in https://glassfish.dev.java.net/issues/show_bug.cgi?id=11606. So, we need to augment the providers
     * discovered by mojarra providers. This method discovers JSF resources packaged in a bundle. It returns the results in
     * the two collections passed to this method. These two collections are then set as ServletContext attributes which are
     * used by {@link org.glassfish.osgiweb.OSGiFacesConfigResourceProvider} and
     * {@link org.glassfish.osgiweb.OSGiFaceletConfigResourceProvider}.
     *
     * Since mojarra can discover faces-config.xmls, in order to avoid duplicate resource situation as reported in
     * https://glassfish.dev.java.net/issues/show_bug.cgi?id=12914, we only find faces config resources that ends with
     * .faces-config.xml.
     *
     * @param bnd application bundle
     * @param facesConfigs faces config
     * @param faceletConfigs facelet config
     */
    private void discoverJSFConfigs(final Bundle bnd, final Collection<URI> facesConfigs, final Collection<URI> faceletConfigs) {

        OSGiBundleArchive archive = new OSGiBundleArchive(bnd);
        for (BundleResource r : archive) {
            final String path = r.getPath();
            if (path.startsWith("META-INF/")) {
                final URI uri = r.getUri();
                if (path.endsWith(".taglib.xml")) {
                    faceletConfigs.add(uri);
                } else if (path.endsWith(".faces-config.xml")) {
                    // this check automatically excludes
                    // META-INF/faces-config.xml
                    facesConfigs.add(uri);
                }
            }
        }
    }

    /**
     * Scan the JSF annotations for a given module.
     *
     * @param wm the GlassFish web module
     * @return map of scanned annotations
     */
    private Map<Class<? extends Annotation>, Set<Class<? extends Object>>> scanFacesAnnotations(final WebModule wm) {

        final DeploymentContext dc = wm.getWebModuleConfig().getDeploymentContext();
        if (dc == null) {
            // Now that we check for default web module in decorate(),
            // it is not clear why we will ever be called with null
            // deployment context.
            // Just log a message and move on.
            LOGGER.fine("Can't process annotations as deployment context is" + " not set.");
            return Collections.emptyMap();
        }
        final Types types = dc.getTransientAppMetaData(Types.class.getName(), Types.class);
        return OSGiFacesAnnotationScanner.scan(getURIs(wm), types, getClassLoader(wm));
    }

    /**
     * Get the resource URIs for a given web module.
     *
     * @param wm the GlassFish web module
     * @return collection of URI
     */
    private Collection<URI> getURIs(final WebModule wm) {
        WebappClassLoader cl = getClassLoader(wm);
        Collection<URI> uris = new ArrayList<>();
        for (URL url : cl.getURLs()) {
            try {
                uris.add(url.toURI());
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Unable to process " + url, e);
            }
        }
        return uris;
    }

    /**
     * Get the application class-loader.
     *
     * @param wm the GlassFish web module
     * @return WebappClassLoader
     */
    private WebappClassLoader getClassLoader(final WebModule wm) {
        WebappClassLoader cl = WebappClassLoader.class.cast(wm.getWebModuleConfig().getDeploymentContext().getClassLoader());
        return cl;
    }

}
