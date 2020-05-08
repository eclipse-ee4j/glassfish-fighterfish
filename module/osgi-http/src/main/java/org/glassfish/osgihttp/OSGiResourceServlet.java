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

import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Servlet to serve resources through OSGi.
 */
public final class OSGiResourceServlet extends HttpServlet {

    /**
     * Servlet alias.
     */
    private final String alias;

    /**
     * Servlet name.
     */
    private final String name;

    /**
     * OSGi HTTP context.
     */
    private final HttpContext httpContext;

    /**
     * Create a new instance.
     * 
     * @param sAlias the servlet alias
     * @param sName the servlet name
     * @param sHttpCtx the OSGi HTTP context
     */
    public OSGiResourceServlet(final String sAlias, final String sName, final HttpContext sHttpCtx) {

        this.alias = sAlias;
        this.name = sName;
        this.httpContext = sHttpCtx;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        final String resPath = getResourcePath(req);
        URL url = httpContext.getResource(resPath);
        if (url == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // contentType must be set before writing anything to the stream
        // as for long data,m stream gets flushed before we have finished
        // writing everything.
        String mimeType = httpContext.getMimeType(resPath);
        if (mimeType == null) {
            mimeType = getServletConfig().getServletContext().getMimeType(resPath);
        }
        resp.setContentType(mimeType);
        URLConnection conn = url.openConnection();
        int writeCount = writeToStream(conn, resp.getOutputStream());
        resp.setContentLength(writeCount);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Get the resource from the request URI.
     * 
     * @param req the incoming request
     * @return mapped resource path
     */
    private String getResourcePath(final HttpServletRequest req) {
        String servletPath = req.getServletPath();
        assert (alias.equals(servletPath));
        String contextPath = req.getContextPath();
        final String requestURI;
        try {
            requestURI = new URI(req.getRequestURI()).normalize().toString();
        } catch (URISyntaxException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
        String requestedPath = requestURI.substring(contextPath.length());
        StringBuilder mappedPath = new StringBuilder(requestedPath);
        String internalName;
        if ("/".equals(name)) {
            internalName = "";
        } else {
            internalName = name;
        }
        mappedPath.replace(0, servletPath.length(), internalName);
        return mappedPath.toString();
    }

    /**
     * Write the connection input stream to the given output stream.
     * 
     * @param connection the connection to use
     * @param os the output stream
     * @return number of byte written
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("checkstyle:magicnumber")
    private int writeToStream(final URLConnection connection, final OutputStream os) throws IOException {

        InputStream is = connection.getInputStream();
        try {
            byte[] buf = new byte[8192];
            int readCount = is.read(buf);
            int writeCount = 0;
            while (readCount != -1) {
                os.write(buf, 0, readCount);
                writeCount += readCount;
                readCount = is.read(buf);
            }
            return writeCount;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
