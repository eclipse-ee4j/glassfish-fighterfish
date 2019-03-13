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

import org.apache.catalina.HttpRequest;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.security.common.PrincipalImpl;
import org.osgi.service.http.HttpContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * This valve is used to implement security in OSGi/HTTP service.
 */
public final class OSGiSecurityValve extends ValveBase {

    /**
     * The OSGi HTTP context.
     */
    private final HttpContext httpContext;

    /**
     * Create a new instance.
     * @param ctx the OSGi HTTP context
     */
    public OSGiSecurityValve(final HttpContext ctx) {
        this.httpContext = ctx;
    }

    @Override
    public int invoke(final Request request, final Response response)
            throws IOException, ServletException {

        if (httpContext.handleSecurity(HttpServletRequest.class.cast(request),
                HttpServletResponse.class.cast(response))) {

            // Issue #13283: If user has set username and auth type, we need to
            // map it to appropriate catalina apis
            // so that HttpServletRequest.getRemoteUser() and
            // getAuthenticationType() will return appropriate values.
            mapUser((HttpRequest) request);
            mapAuthType((HttpRequest) request);

            return INVOKE_NEXT;
        } else {
//            HttpServletResponse.class.cast(response).sendError(
//                    HttpServletResponse.SC_FORBIDDEN);
            return END_PIPELINE;
        }
    }

    /**
     * Map authentication type for a given request.
     * @param httpRequest the request to map
     */
    private void mapAuthType(final HttpRequest httpRequest) {
        String authType = (String) httpRequest.getRequest()
                .getAttribute(HttpContext.AUTHENTICATION_TYPE);
        if (authType != null) {
            httpRequest.setAuthType(authType);
        }
    }

    /**
     * Map a user for a given request.
     * @param httpRequest the request to map
     */
    private void mapUser(final HttpRequest httpRequest) {
        String userName = (String) httpRequest.getRequest()
                .getAttribute(HttpContext.REMOTE_USER);
        if (userName != null) {
            Principal principal = new PrincipalImpl(userName);
            httpRequest.setUserPrincipal(principal);
        }
    }
}
