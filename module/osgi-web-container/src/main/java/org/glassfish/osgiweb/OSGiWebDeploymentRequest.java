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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.glassfish.osgiweb.Constants.BUNDLE_CONTEXT_ATTR;
import static org.glassfish.osgiweb.Constants.OSGI_WEB_CONTEXTPATH;
import static org.glassfish.osgiweb.Constants.OSGI_WEB_SYMBOLIC_NAME;
import static org.glassfish.osgiweb.Constants.OSGI_WEB_VERSION;
import static org.glassfish.osgiweb.Constants.VIRTUAL_SERVERS;
import static org.glassfish.osgiweb.Constants.WEB_CONTEXT_PATH;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.osgijavaeebase.DeploymentException;
import org.glassfish.osgijavaeebase.OSGiApplicationInfo;
import org.glassfish.osgijavaeebase.OSGiDeploymentContext;
import org.glassfish.osgijavaeebase.OSGiDeploymentRequest;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.web.WebContainer;
import com.sun.enterprise.web.WebModule;

import jakarta.servlet.ServletContext;

/**
 * This is the class responsible for deploying a WAB in the Java EE container.
 */
public final class OSGiWebDeploymentRequest extends OSGiDeploymentRequest {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiWebDeployer.class.getPackage().getName());

    /**
     * Set the current bundle context in a thread local for use during web module decoration.
     */
    private static final ThreadLocal<BundleContext> CURRENT_BUNDLE_CTX = new ThreadLocal<>();

    /**
     * Create a new instance.
     *
     * @param deployer GlassFish deployer
     * @param archiveFactory GlassFish archive factory
     * @param env GlassFish server environment
     * @param reporter GlassFish command reporter
     * @param bnd application bundle
     */
    public OSGiWebDeploymentRequest(Deployment deployer, ArchiveFactory archiveFactory, ServerEnvironmentImpl env, ActionReport reporter, Bundle bnd) {
        super(deployer, archiveFactory, env, reporter, bnd);
    }

    @Override
    protected OSGiDeploymentContext getDeploymentContextImpl(ActionReport reporter, Logger logger, ReadableArchive archive, OpsParams opsParams, ServerEnvironmentImpl env, final Bundle bnd) throws Exception {
        return new OSGiWebDeploymentContext(reporter, logger, archive, opsParams, env, bnd);
    }

    @Override
    protected WAB makeArchive() {
        Bundle host = getBundle();
        return new WAB(host, getFragments(host));
    }

    /**
     * Get the bundle fragments for the given host bundle.
     *
     * @param host the host bundle
     * @return array of Bundle
     */
    private static Bundle[] getFragments(final Bundle host) {
        List<Bundle> fragments = new ArrayList<>();
        BundleWiring hostWiring = host.adapt(BundleWiring.class);
        for (BundleWire wire : hostWiring.getProvidedWires(HOST_NAMESPACE)) {
            fragments.add(wire.getRequirerWiring().getBundle());
        }
        
        return fragments.toArray(new Bundle[fragments.size()]);
    }

    @Override
    protected DeployCommandParameters getDeployParams() throws Exception {
        DeployCommandParameters parameters = super.getDeployParams();
        // Set the contextroot explicitly, else it defaults to name.
        try {
            // We expect WEB_CONTEXT_PATH to be always present.
            // This is mandated in the spec.
            parameters.contextroot = getArchive().getManifest().getMainAttributes().getValue(WEB_CONTEXT_PATH);
        } catch (IOException e) {
            // ignore and continue
        }
        
        if (parameters.contextroot == null || parameters.contextroot.length() == 0) {
            throw new Exception(WEB_CONTEXT_PATH + " manifest header is mandatory");
        }
        if (!parameters.contextroot.startsWith("/")) {
            // We prefix '/' for reasons mentioned in Uitil.getContextRoot().
            parameters.contextroot = "/".concat(parameters.contextroot);
        }
        parameters.virtualservers = getVirtualServers();
        
        return parameters;
    }

    /**
     * Get the GlassFish virtual server name.
     *
     * @return server name
     */
    private String getVirtualServers() {
        String virtualServers = null;
        try {
            virtualServers = getArchive().getManifest().getMainAttributes().getValue(VIRTUAL_SERVERS);
        } catch (Exception e) {
            // ignore
        }
        
        if (virtualServers == null) {
            virtualServers = getDefaultVirtualServer();
        }
        
        StringTokenizer st = new StringTokenizer(virtualServers);
        if (st.countTokens() > 1) {
            throw new IllegalArgumentException("Currently, we only support deployment to one" + " virtual server.");
        }
        
        return virtualServers;
    }

    /**
     * Get the default virtual server name.
     *
     * @return the default virtual server
     */
    @SuppressWarnings("unchecked")
    private String getDefaultVirtualServer() {
        // Grizzly renamed its package name from com.sun.grizzly to
        // org.glassfish.grizzly in Grizzly 2.1. Since Grizzly 2.1 is only
        // integrated into GF3.2 only and we expect our module to work with
        // GF 3.1.1 as well, we are not relying on Grizzly classes statically.
        // So, the code below does what the following line would have done.
        // return Globals.get(com.sun.grizzly.config.dom.NetworkListener.class)
        // .findHttpProtocol().getHttp().getDefaultVirtualServer();
        Class<?> netWorkListenerClass;
        try {
            netWorkListenerClass = Class.forName("com.sun.grizzly.config.dom.NetworkListener");
        } catch (ClassNotFoundException cnfe) {
            try {
                netWorkListenerClass = Class.forName("org.glassfish.grizzly.config.dom.NetworkListener");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        Object networkListenerObj = Globals.get(netWorkListenerClass);
        try {
            Object httpProtocolObj = netWorkListenerClass.getMethod("findHttpProtocol").invoke(networkListenerObj);
            Object httpObj = httpProtocolObj.getClass().getMethod("getHttp").invoke(httpProtocolObj);
            String defaultVirtualServer = (String) httpObj.getClass().getMethod("getDefaultVirtualServer").invoke(httpObj);
            
            LOGGER.logp(FINE, "OSGiWebDeploymentRequest", "getDefaultVirtualServer", "defaultVirtualServer = {0}", new Object[] { defaultVirtualServer });
            
            return defaultVirtualServer;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preDeploy() throws DeploymentException {
        detectCollisions();
        CURRENT_BUNDLE_CTX.set(getBundle().getBundleContext());
    }

    /**
     * Detect bundle collision.
     *
     * @throws ContextPathCollisionException if a collision is detected
     */
    private void detectCollisions() throws ContextPathCollisionException {
        ContextPathCollisionDetector.get().preDeploy(getBundle());
    }

    @Override
    public void postDeploy() {
        CURRENT_BUNDLE_CTX.set(null);
        OSGiApplicationInfo osgiAppInfo = getResult();
        if (osgiAppInfo == null) {
            ContextPathCollisionDetector.get().cleanUp(getBundle());
            return;
        }
        
        ServletContext sc = getServletContext(osgiAppInfo.getAppInfo());
        assert sc.getAttribute(BUNDLE_CONTEXT_ATTR) == osgiAppInfo.getBundle().getBundleContext();

        try {
            ServiceRegistration<?> scReg = registerService(osgiAppInfo.getBundle(), sc);
            // TODO(Sahoo): Unregister scReg when we go down
        } catch (IllegalStateException e) {
            // See issue #15398 as to why this can happen
            LOGGER.logp(WARNING, "OSGiWebDeploymentRequest", "postDeploy",
                    "Failed to register ServletContext for bundle " + osgiAppInfo.getBundle().getBundleId() + " because of following exception:", e);
        }
    }

    /**
     * Get the servlet context for a deployed application.
     *
     * @param appInfo deployed application
     * @return ServletContext
     */
    private ServletContext getServletContext(final ApplicationInfo appInfo) {
        if (appInfo.getModuleInfos().size() == 1) {
            ModuleInfo moduleInfo = appInfo.getModuleInfos().iterator().next();
            EngineRef engineRef = moduleInfo.getEngineRefForContainer(WebContainer.class);
            assert engineRef != null;
            
            WebApplication webApplication = (WebApplication) engineRef.getApplicationContainer();
            Set<WebModule> webModules = webApplication.getWebModules();
            
            // we only deploy to default virtual server
            assert webModules.size() == 1;
            
            if (webModules.size() == 1) {
                return webModules.iterator().next().getServletContext();
            }
        }
        return null;
    }

    /**
     * Register the servlet context as a service.
     *
     * @param bnd the application bundle
     * @param sc the servlet context
     * @return ServiceRegistration
     */
    @SuppressWarnings("unchecked")
    private ServiceRegistration<?> registerService(final Bundle bnd, final ServletContext sc) {

        Dictionary props = new Properties();
        props.put(OSGI_WEB_SYMBOLIC_NAME, bnd.getSymbolicName());
        String cpath = Util.getContextPath(bnd);
        props.put(OSGI_WEB_CONTEXTPATH, cpath);
        String version = bnd.getHeaders().get(BUNDLE_VERSION);
        if (version != null) {
            props.put(OSGI_WEB_VERSION, version);
        }
        BundleContext bctx = bnd.getBundleContext();
        if (bctx != null) {
            // This null check is required until we upgrade to Felix 1.8.1.
            // Felix 1.8.0 returns null when bundle is in starting state.
            ServiceRegistration<?> scReg = bctx.registerService(ServletContext.class.getName(), sc, props);
            LOGGER.logp(INFO, "OSGiWebContainer", "registerService", "Registered ServletContext as a service with" + " properties: {0} ",
                    new Object[] { props });
            return scReg;
        } else {
            LOGGER.logp(WARNING, "OSGiWebContainer", "registerService", "Not able to register ServletContext as a service as" + " bctx is null");
        }
        
        return null;
    }

    /**
     * Get the current bundle context (thread local).
     *
     * @return BundleContext
     */
    static BundleContext getCurrentBundleContext() {
        return CURRENT_BUNDLE_CTX.get();
    }
}
