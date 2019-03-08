/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom servlet config.
 */
public final class OSGiServletConfig implements ServletConfig {

    /**
     * Servlet name.
     */
    private final String servletName;

    /**
     * The servlet context.
     */
    private final ServletContext servletContext;

    /**
     * The servlet init parameters.
     */
    private final Map<String, String> initParams =
            new HashMap<String, String>();

    /**
     * Create a new instance.
     * @param sName the servlet name
     * @param sCtx the servlet context
     * @param sInitparams the servlet init parameters
     */
    public OSGiServletConfig(final String sName, final ServletContext sCtx,
            final Dictionary sInitparams) {

        this.servletName = sName;
        this.servletContext = sCtx;
        if (sInitparams != null) {
            Enumeration e = sInitparams.keys();
            while (e.hasMoreElements()) {
                final Object key = e.nextElement();
                this.initParams.put((String) key,
                        (String) sInitparams.get(key));
            }
        }
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(final String paramName) {
        return initParams.get(paramName);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    /**
     * Get the servlet init parameters.
     * @return an unmodifiable map of {@link #initParams}
     */
    Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParams);
    }
}
