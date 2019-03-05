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
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiResourceServlet extends HttpServlet {

    private final String alias;
    private final String name;
    private final HttpContext httpContext;

    public OSGiResourceServlet(String alias, String name,
            HttpContext httpContext) {

        this.alias = alias;
        this.name = name;
        this.httpContext = httpContext;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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
            mimeType = getServletConfig().getServletContext()
                    .getMimeType(resPath);
        }
        resp.setContentType(mimeType);
        URLConnection conn = url.openConnection();
        int writeCount = writeToStream(conn, resp.getOutputStream());
        resp.setContentLength(writeCount);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private String getResourcePath(HttpServletRequest req) {
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
        String internalName = name == "/" ? "" : name;
        mappedPath.replace(0, servletPath.length(), internalName);
//        System.out.println("Mapped [" + requestedPath
//                + "] to [" + mappedPath + "]");
        return mappedPath.toString();
    }

    private int writeToStream(URLConnection connection, OutputStream os)
            throws IOException {

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
