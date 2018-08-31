/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgihttp;

import com.sun.enterprise.web.WebModule;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardWrapper;
import org.glassfish.web.valve.GlassFishValve;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Set;

/**
 * Unlike Java EE Web Application model, there is no notion of "context path"
 * in OSGi HTTP service spec. Here the servlets can specify which context they
 * belong to by passing a {@link org.osgi.service.http.HttpContext} object.
 * Those HttpContext objects don't have any "path" attribute. As a result,
 * all the OSGi/HTTP servlets belonging to the same servlet context may not
 * have any of the path common to them. Internally, we register all the OSGi
 * servlets (actually we register {@link OSGiServletWrapper}
 * with the same {@link org.apache.catalina.Context} object. So we need a way to
 * demultiplex the OSGi servlet context.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiServletWrapper extends StandardWrapper implements Wrapper {

    // TODO(Sahoo): Logging

    private Servlet servlet;
    private OSGiServletConfig config;
    private WebModule webModule;

    public OSGiServletWrapper(String name, Servlet servlet, OSGiServletConfig config, String urlMapping, WebModule webModule) {
        this.servlet = servlet;
        this.config = config;
        this.webModule = webModule;
        // Set init params in the wrapper itself to avoid issues as reported in GLASSFISH-18492
        Set<String> conflicts = setInitParameters(config.getInitParameters());
        assert(conflicts.isEmpty());
        setOSGi(true);
        setServlet(servlet);
        setName(name);
        addMapping(urlMapping);
    }

    public Servlet getServlet() {
        return servlet;
    }

    /* package */ void initializeServlet() throws ServletException {
        servlet.init(config);
    }

    /* package */ void destroyServlet() {
        servlet.destroy();
    }

    // BEGIN: Override ServletConfig methods

    @Override
    public String getServletName() {
        return config.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        // We can't use super.getServletContext, as that would get us the
        // ServletContext that's common for all OSGi/HTTP servlets, where as
        // we need the servlet context registered for each HttpContext.
        return config.getServletContext();
    }

    // no need to override initiParams related methods as we already set the params in super inside our constructor.

    // END: Override ServletConfig methods

    // BEGIN: Override lifecycle methods of StandardWrapper...

    @Override
    public Servlet allocate() throws ServletException {
        return servlet;
    }

    @Override
    public synchronized void load() throws ServletException {
        // NOOP: We already have the Servlet instance.
    }

    @Override
    public synchronized void unload() throws ServletException {
        // NOOP: We don't have to do anything, as HttpService calls
        // destroServlet method directly from unregister() method.
        return;
    }
    // END: Override lifecycle methods of StandardWrapper...

    // Override addValve as StandardWrapper has put in an optimisation
    // and does not support adding any valve (see issue #1343).
    @Override
    public synchronized void addValve(GlassFishValve valve) {
        getPipeline().addValve(valve);
    }

    public WebModule getWebModule() {
        return webModule;
    }
}
