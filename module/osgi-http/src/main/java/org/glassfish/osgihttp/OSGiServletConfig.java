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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiServletConfig implements ServletConfig {

    private String servletName;
    private ServletContext servletContext;
    private Map<String, String> initParams = new HashMap<String, String>();

    public OSGiServletConfig(String servletName,
                             ServletContext servletContext,
                             Dictionary initParams) {
        this.servletName = servletName;
        this.servletContext = servletContext;
        if (initParams != null) {
            Enumeration e = initParams.keys();
            while (e.hasMoreElements()) {
                final Object key = e.nextElement();
                this.initParams.put((String) key,
                        (String) initParams.get(key));
            }
        }
    }

    public String getServletName() {
        return servletName;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    /**
     * @return an unmodifiable map of {@link #initParams}
     */
    /* package */Map<String,String> getInitParameters() {
        return Collections.unmodifiableMap(initParams);
    }
}
