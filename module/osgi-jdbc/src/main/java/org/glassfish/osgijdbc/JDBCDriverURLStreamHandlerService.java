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

package org.glassfish.osgijdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import org.glassfish.osgijavaeebase.JarHelper;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * URL handler service for JDBC driver.
 */
public final class JDBCDriverURLStreamHandlerService extends
        AbstractURLStreamHandlerService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            JDBCDriverURLStreamHandlerService.class.getPackage().getName());

    /**
     * API class-loader.
     */
    private final ClassLoader apiClassLoader;

    /**
     * Create a new instance.
     * @param cl class-loader
     */
    public JDBCDriverURLStreamHandlerService(final ClassLoader cl) {
        this.apiClassLoader = cl;
    }

    @Override
    public URLConnection openConnection(final URL u) throws IOException {
        assert (Constants.JDBC_DRIVER_SCHEME.equals(u.getProtocol()));
        try {
            debug("jdbc driver openConnection()");
            //final URL[] urls = getURLs(u);
            //final URL uberJarURL = getUberJarURL(urls);
            //final URLConnection con = uberJarURL.openConnection();
            //return new URLConnection(uberJarURL) {


            URI embeddedURI = new URI(u.toURI().getSchemeSpecificPart());
            final URL embeddedURL = embeddedURI.toURL();
            final URLConnection con = embeddedURL.openConnection();
            final URLClassLoader cl = AccessController.doPrivileged(
                    new PrivilegedAction<URLClassLoader>() {

                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(new URL[]{embeddedURL},
                            apiClassLoader);
                }
            });
            return new URLConnection(embeddedURL) {
                private Manifest m;

                @Override
                public void connect() throws IOException {
                    con.connect();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    connect();
                    m = JDBCJarManifestProcessor.processManifest(url, cl);
                    final PipedOutputStream pos = new PipedOutputStream();
                    final PipedInputStream pis = new PipedInputStream(pos);

                    // It is a common practice to spawn a separate thread
                    // to write to PipedOutputStream so that the reader
                    // and writer are not deadlocked.
                    new Thread() {
                        @Override
                        public void run() {
                            JarHelper.write(con, pos, m);
                        }
                    }.start();

                    return pis;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected void setURL(final URL u, final String protocol, final String host,
            final int port, final String auth, final String user,
            final String path, final String query, final String ref) {

        super.setURL(u, protocol, host, port, auth, user, path, query, ref);
        debug("jdbc driver setURL()");
    }

    /**
     * Log a {@code FINE} message.
     * @param msg message to log
     */
    private void debug(final String msg) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-jdbc] : {0}", msg);
        }
    }
}
