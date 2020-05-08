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
package org.glassfish.osgihttp;

import com.sun.enterprise.web.ContextFacade;
import com.sun.enterprise.web.WebModule;
import org.apache.catalina.session.StandardManager;
import org.osgi.service.http.HttpContext;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unlike Java EE Web Application model, there is no notion of "context path" in OSGi HTTP service spec. Here the
 * servlets can specify which context they belong to by passing a {@link org.osgi.service.http.HttpContext} object.
 * Those HttpContext objects don't have any "path" attribute. As a result, all the OSGi/HTTP servlets belonging to the
 * same servlet context may not have any of the path common to them. Internally, we register all the OSGi servlets
 * (actually we register {@link OSGiServletWrapper} with the same {@link org.apache.catalina.Context} object. So we need
 * a way to demultiplex the OSGi servlet context. This class also delegates to {@link HttpContext} for resource
 * resolutions and security.
 */
public final class OSGiServletContext extends ContextFacade {

    /**
     * The OSGi HTTP context.
     */
    private final HttpContext httpContext;

    /**
     * Context attributes.
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    /**
     * Create a new instance.
     * 
     * @param webModule the delegate web module
     * @param ctx the OSGi HTTP context
     */
    public OSGiServletContext(final WebModule webModule, final HttpContext ctx) {

        super(new File(webModule.getDocBase()), webModule.getContextPath(), webModule.getClassLoader());
        setUnwrappedContext(webModule);
        setName(webModule.getName());
        setPath(webModule.getPath());
        setWebContainer(webModule.getWebContainer());
        setJ2EEServer(webModule.getJ2EEServer());
        setWebModuleConfig(webModule.getWebModuleConfig());
        setParentClassLoader(webModule.getParentClassLoader());
        setRealm(webModule.getRealm());
        setParent(webModule.getParent());
        // Set a new manager to have a different HttpSession for this context
        StandardManager mgr = new StandardManager();
        // we switch off Session Persistence due to issues in deserialization
        mgr.setPathname(null);
        setManager(mgr);
//        mgr.setMaxActiveSessions(100);
        this.httpContext = ctx;
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    @Override
    public String getMimeType(final String file) {
        String mimeType = httpContext.getMimeType(file);
        if (mimeType != null) {
            return mimeType;
        }
        return super.getMimeType(file);
    }

    @Override
    public URL getResource(final String path) throws MalformedURLException {
        return httpContext.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(final String path) {
        try {
            URL url = getResource(path);
            if (url != null) {
                return url.openStream();
            }
            return null;
        } catch (Exception e) {
        }
        return null;
    }
}
