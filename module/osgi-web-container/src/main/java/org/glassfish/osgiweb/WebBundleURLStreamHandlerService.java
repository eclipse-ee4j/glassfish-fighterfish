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

import org.glassfish.osgijavaeebase.JarHelper;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.osgiweb.Constants.WEB_BUNDLE_SCHEME;

/**
 * A {@link org.osgi.service.url.URLStreamHandlerService} for webbundle scheme. It is responsible for not only adding
 * necessary OSGi headers to transform a plain vanilla web app to a Web App Bundle (WAB), but also setting appropriate
 * parameters in the URL object to meet spec's requirement as described below:
 * <p/>
 * The java.net.URL object for a webbundle URL must return the String webbundle when the getProtocol method is called.
 * The embedded URL must be returned in full from the getPath method. The parameters for processing manifest must be
 * returned from the getQuery() method.
 * <p/>
 * Some form of embedded URL also contain query parameters and this must be supported. Thus the value returned from
 * getPath may contain a URL query. Any implementation must take care to preserve both the query parameters for the
 * embedded URL as well as the webbundle URL. A question mark must always follow the embedded URL to simplify this
 * processing. The following example shows an HTTP URL with some query parameter:
 * <p/>
 * webbundle:/some/path/?war=example.war?Web-ContextPath=/foo
 * <p/>
 * In this case getPath method of the webbundle URL must return: /some/path/?war=example.war
 * <p/>
 * All the parameters in the webbundle: URL are optional except for the Web-ContextPath parameter. The parameter names
 * are case insensitive, but their values must be treated as case sensitive. Since Web-ContextPath url parameter is
 * mandatory, the query component can never be empty: webbundle:http://www.acme.com:8021/sales.war?
 */
public final class WebBundleURLStreamHandlerService extends AbstractURLStreamHandlerService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(WebBundleURLStreamHandlerService.class.getPackage().getName());

    @Override
    public URLConnection openConnection(final URL u) throws IOException {

        assert (WEB_BUNDLE_SCHEME.equals(u.getProtocol()));
        final URL embeddedURL = new URL(u.getPath());
        final URLConnection con = embeddedURL.openConnection();
        return new URLConnection(embeddedURL) {
            private Manifest m;

            @Override
            public void connect() throws IOException {
                con.connect();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                m = WARManifestProcessor.processManifest(embeddedURL, u.getQuery());
                final PipedOutputStream pos = new PipedOutputStream();
                final PipedInputStream pis = new PipedInputStream(pos);

                // It is a common practice to spawn a separate thread
                // to write to PipedOutputStream so that the reader
                // and writer are not deadlocked.
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            writeWithoutSignedFiles(con, pos, m);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }.start();
                return pis;
            }
        };
    }

    @Override
    @SuppressWarnings("checkstyle:parameterNumber")
    protected void setURL(final URL u, final String proto, final String host, final int port, final String auth, final String user, final String path,
            final String query, final String ref) {

        LOGGER.logp(Level.INFO, "WebBundleURLStreamHandlerService", "setURL() called with",
                "u = [{0}], proto = [{1}], host = [{2}], port = [{3}], " + "auth = [{4}], user = [{5}], path = [{6}], " + "query = [{7}], ref = [{7}]",
                new Object[] { u, proto, host, port, auth, user, path, query, ref });

        // Since Web-ContextPath url param must be present, the query
        // can't be null.
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("The query component can't be null." + " It must at least contain Web-ContextPath parameter.");
        }
        // Let's see if there are two parts in the query. If there are two
        // parts, then the first part belongs to embedded URL, else
        // the query entirely belongs to the outer URL.
        int sep = query.indexOf("?");
        String zeQuery;
        String zePath;
        if (sep != -1) { // two parts in the query
            String query1 = query.substring(0, sep);
            String query2 = query.substring(sep + 1);
            zePath = path.concat("?").concat(query1);
            zeQuery = query2;
        } else {
            zeQuery = query;
            zePath = path;
        }
        LOGGER.logp(Level.INFO, "WebBundleURLStreamHandlerService", "setURL ", "new path = [{0}], new query = [{1}]", new Object[] { path, query });

        // Let's also validate here that query contains Web-ContextPath param
        Map<String, String> queryParameters = WARManifestProcessor.readQueryParams(zeQuery);
        boolean containsContextPathKey = false;
        for (String key : queryParameters.keySet()) {
            if (Constants.WEB_CONTEXT_PATH.equalsIgnoreCase(key)) {
                containsContextPathKey = true;
                break;
            }
        }
        if (!containsContextPathKey) {
            throw new IllegalArgumentException("Query [" + zeQuery + "] does not contain Web-ContextPath parameter.");
        }
        super.setURL(u, proto, host, port, auth, user, zePath, zeQuery, ref);
    }

    /**
     * Write the content of the JAR file available at the given URL connection into the given output stream.
     *
     * @param con the URL connection
     * @param os the output stream
     * @param m the manifest
     * @throws IOException if an error occurs
     */
    private void writeWithoutSignedFiles(final URLConnection con, final PipedOutputStream os, final Manifest m) throws IOException {

        JarInputStream jis = null;
        JarOutputStream jos = null;
        try {
            jis = new JarInputStream(con.getInputStream());
            jos = new JarOutputStream(os, m);
            writeWithoutSignedFiles(jis, jos);
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException e) {
                }
            }
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Write the given JAR input stream to the given JAR output stream and omit signatures.
     *
     * @param jis the JAR input stream
     * @param jos the JAR output stream
     * @throws IOException if an error occurs
     */
    private void writeWithoutSignedFiles(final JarInputStream jis, final JarOutputStream jos) throws IOException {

        // Ideally we should enhance JarHelper.write() method to accept a
        // filter that it could use to exclude files
        // from being written out. But, since such a method does not exist,
        // we will write it here itself.
        // JarHelper.write(con, pos, m);
        JarHelper.accept(jis, new JarVisitorImpl(jis, jos));
    }

    /**
     * JarVisitor implementation.
     */
    private static final class JarVisitorImpl implements JarHelper.Visitor {

        /**
         * Input stream.
         */
        private final JarInputStream jis;

        /**
         * Output stream.
         */
        private final JarOutputStream jos;

        /**
         * Buffer.
         */
        private ByteBuffer buffer;

        /**
         * Create a new instance.
         * 
         * @param is input stream
         * @param os output stream
         */
        @SuppressWarnings("checkstyle:magicnumber")
        JarVisitorImpl(final JarInputStream is, final JarOutputStream os) {
            this.jis = is;
            this.jos = os;
            buffer = ByteBuffer.allocate(1024);
        }

        @Override
        public void visit(final JarEntry je) {
            try {
                final String name = je.getName();
                if (name.startsWith("META-INF/") && !name.substring("META-INF/".length()).contains("/")
                        && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.startsWith("SIG-"))) {
                    LOGGER.logp(Level.INFO, "WebBundleURLStreamHandlerService", "visit", "Skipping writing of singature file {0}", new Object[] { name });
                    return;
                }
                LOGGER.logp(Level.FINE, "WebBundleURLStreamHandlerService", "visit", "Writing jar entry = {0}", new Object[] { je });
                jos.putNextEntry(je);
                JarHelper.copy(jis, jos, buffer);
                jos.closeEntry();
            } catch (IOException e) {
                // TODO(Sahoo): Proper Exception Handling
                throw new RuntimeException(e);
            }
        }
    }
}
