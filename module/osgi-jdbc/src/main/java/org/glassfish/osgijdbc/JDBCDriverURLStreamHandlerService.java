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

package org.glassfish.osgijdbc;

import org.glassfish.osgijavaeebase.JarHelper;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.*;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JDBCDriverURLStreamHandlerService extends
        AbstractURLStreamHandlerService {

    private static final Logger LOGGER = Logger.getLogger(
            JDBCDriverURLStreamHandlerService.class.getPackage().getName());

    private final ClassLoader apiClassLoader;

    public JDBCDriverURLStreamHandlerService(ClassLoader apiClassLoader){
        this.apiClassLoader = apiClassLoader;
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
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
    protected void setURL(URL u, String protocol, String host, int port,
            String auth, String user, String path, String query, String ref) {

        super.setURL(u, protocol, host, port, auth, user, path, query, ref);
        debug("jdbc driver setURL()");
    }

    private void debug(String s) {
        if(LOGGER.isLoggable(Level.FINEST)){
            LOGGER.log(Level.FINEST, "[osgi-jdbc] : {0}", s);
        }
    }
}
