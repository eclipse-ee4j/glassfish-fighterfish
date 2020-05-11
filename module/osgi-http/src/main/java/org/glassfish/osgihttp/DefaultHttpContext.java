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

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Default implementation of {@link HttpContext}. 
 * 
 * <p>
 * As per the spec (OSGi R4 Compendium, section #102.4): 1. the default
 * implementation of getResource() must map the resource request to the Bundle.getResource(String). 2. the
 * getMime(String) implementation of the default HttpContext object should return a reasonable mapping. 3. Its
 * handleSecurity(HttpServlet Request,HttpServletResponse) may implement an authentication mecha-nism that is
 * implementation-dependent.
 * <p/>
 * {@code org.osgi.service.http.HttpService#createDefaultHttpContext()}
 */
public final class DefaultHttpContext implements HttpContext {

    // TODO(Sahoo): getMimeType() to use default-web.xml.

    /**
     * Bundle for which this context is created.
     */
    private final Bundle registeringBundle;

    /**
     * Create a new instance.
     *
     * @param bnd the registering bundle
     */
    public DefaultHttpContext(Bundle bnd) {
        this.registeringBundle = bnd;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // TODO(Sahoo):
        return true;
    }

    /**
     * @param s
     * @return URL
     */
    @Override
    public URL getResource(String s) {
        /*
         * As per the spec (OSGi R4 Compendium, section #102.4): the default implementation must map the resource request to the
         * bundle's resource, using Bundle.getResource(String). The internal name must specify the full path to the directory
         * containing the resource files in the bundle. No automatic prefixing of the package name is done.
         */
        // TODO(Sahoo): doPrivileged()
        return registeringBundle.getResource(s);
    }

    @Override
    public String getMimeType(final String s) {
        return null;
    }
}
