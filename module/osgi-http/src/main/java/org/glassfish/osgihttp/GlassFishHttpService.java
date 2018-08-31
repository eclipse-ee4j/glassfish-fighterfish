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
import org.apache.catalina.Container;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.glassfish.web.valve.GlassFishValve;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * This contains most of the implementation of {@link org.osgi.service.http.HttpService}.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class GlassFishHttpService {

    // TODO(Sahoo): Improve synchronization of this object

    /**
     * Root context with which all wrappers are registered.
     */
    private WebModule context;

    private Map<HttpContext, OSGiServletContext> servletContextMap =
            new HashMap<HttpContext, OSGiServletContext>();

    public GlassFishHttpService(WebModule context) {
        this.context = context;
    }

    /**
     * This method behaves the same way as
     * {@link org.osgi.service.http.HttpService#registerServlet}
     * except that it expects a non-null HttpContext object.
     */
    public synchronized void registerServlet(String alias, Servlet servlet, Dictionary initParams, HttpContext httpContext) throws NamespaceException, ServletException {
        validateAlias(alias);
        validateServlet(servlet);

        OSGiServletContext servletContext =
                servletContextMap.get(httpContext);
        if (servletContext == null) {
            servletContext = new OSGiServletContext(context,
                    httpContext);
            servletContextMap.put(httpContext, servletContext);
        }

        // Unlike web.xml, here there is no notion of Servlet name,
        // so we use the alias as the servlet name. It is unique, so no issues.
        String wrapperName = alias;
        OSGiServletConfig servletConfig =
                new OSGiServletConfig(wrapperName, servletContext.getServletContext(), initParams);
        OSGiServletWrapper wrapper = new OSGiServletWrapper(
                wrapperName, servlet, servletConfig, convert(alias), servletContext);
        wrapper.addValve((GlassFishValve)new OSGiSecurityValve(httpContext));
        context.addChild(wrapper);
        try {
            wrapper.initializeServlet();
        } catch (Exception e) {
            throw new ServletException(e); // spec requires this behavior.
        }
    }

    public synchronized void registerResources(String alias, String name, HttpContext httpContext) throws NamespaceException {
        validateAlias(alias);
        validateName(name);
        OSGiResourceServlet servlet = new OSGiResourceServlet(alias, name, httpContext);
        OSGiServletContext servletContext =
                servletContextMap.get(httpContext);
        if (servletContext == null) {
            servletContext = new OSGiServletContext(context,
                    httpContext);
            servletContextMap.put(httpContext, servletContext);
        }

        // Unlike web.xml, here there is no notion of Servlet name,
        // so we use the alias as the servlet name. It is unique, so no issues.
        String wrapperName = alias;
        OSGiServletConfig servletConfig =
                new OSGiServletConfig(wrapperName, servletContext.getServletContext(), null);
        OSGiServletWrapper wrapper = new OSGiServletWrapper(
                wrapperName, servlet, servletConfig, convert(alias), servletContext);
        wrapper.addValve((GlassFishValve)new OSGiSecurityValve(httpContext));
        context.addChild(wrapper);
        try {
            wrapper.initializeServlet();
        } catch (Exception e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    public synchronized void unregister(String alias, boolean callDestroy) {
        OSGiServletWrapper wrapper = getWrapper(alias);
        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "No registration exists for " + alias);
        }
        context.removeChild(wrapper);
        if (callDestroy) {
            wrapper.destroyServlet();
        }
    }

    private OSGiServletWrapper getWrapper(String alias) {
        // The code below does not work, because we call
        // addMapping() inside OSGiServletWrapper's constructor
        // and by that time the wrapper has obviously not been added
        // to the context. When subsequently, wrapper is added to context,
        // for some reason, context's mapping information is not updated.
        // So, we always get null.
        // We must add the mapping in constructor of OSGiServletWrapper, because
        // at the time the MapperListener received the JMX event for the
        // Wrapper's mbean, urlPatterns must be configured on the Wrapper.

//        String wrapperName = context.findServletMapping(convert(alias));
//        if (wrapperName == null) {
//            throw new IllegalArgumentException(
//                    "No registration exists for " + alias);
//        }
//        return wrapperName;
        String wrapperName = alias;// we internally use alias as wrapper name.
        OSGiServletWrapper wrapper =
                (OSGiServletWrapper) context.findChild(wrapperName);
        return wrapper;
    }

    /**
     * Validate that the servlet is not already registered.
     *
     * @param servlet Servlet to validate
     * @throws ServletException if it is already registered.
     */
    private void validateServlet(Servlet servlet) throws ServletException {
        for (Container c : context.findChildren()) {
            if (!(c instanceof OSGiServletWrapper)) continue;
            if (servlet == OSGiServletWrapper.class.cast(c).getServlet()) {
                throw new ServletException("servlet is already registered");
            }
        }
    }

    /**
     * Check if the alias s valid as per the spec. The spec requires that:
     * an alias must begin with slash ('/') and must not end with slash ('/'),
     * with the exception that an alias of the form "/" is used to denote
     * the root alias.
     *
     * @param alias The alias is the name in the URI namespace of
     *              the Http Service at which the registration will be mapped
     * @throws IllegalArgumentException if the alias is malformed.
     * @throws NamespaceException       if the alias is already registered.
     */
    private void validateAlias(String alias) throws NamespaceException {
        if (!alias.equals("/") && (!alias.startsWith("/") || alias.endsWith("/"))) {
            throw new IllegalArgumentException("malformed alias");
        }
        if (getWrapper(alias) != null) {
            throw new NamespaceException("alias already registered");
        }
    }

    /**
     * Converts an OSGi alias to servlet pattern used by GlassFish/Tomcat
     *
     * @param alias alias used by OSGi HTTP Service users
     * @return servlet pattern used by Tomcat/GlassFish
     */
    private String convert(String alias) {
        if (alias.equals("/")) return "/*";
        else return alias + "/*";
    }

    /**
     * Check if the internal name of a resource is valid or not.
     * The spec requires that the name parameter in registerResources method
     * must not end with slash ('/').
     *
     * @param name the base name of the resource as used in registerResources method
     * @throws IllegalArgumentException if the alias is malformed.
     */
    private void validateName(String name) {
        if (name.endsWith("/")) {
            throw new IllegalArgumentException("name ends with '/'");
        }
    }
}
