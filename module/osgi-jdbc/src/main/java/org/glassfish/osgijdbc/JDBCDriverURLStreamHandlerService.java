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


public class JDBCDriverURLStreamHandlerService extends AbstractURLStreamHandlerService {

    private static final Logger logger = Logger.getLogger(
            JDBCDriverURLStreamHandlerService.class.getPackage().getName());

    private ClassLoader apiClassLoader;

    public JDBCDriverURLStreamHandlerService(ClassLoader apiClassLoader){
        this.apiClassLoader = apiClassLoader;
    }

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
            final URLClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
                public URLClassLoader run() {
                    return new URLClassLoader(new URL[]{embeddedURL}, apiClassLoader);
                }
            });
            return new URLConnection(embeddedURL) {
                private Manifest m;

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
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setURL(URL u, String protocol, String host, int port, String auth, String user,
                          String path, String query, String ref) {
        super.setURL(u, protocol, host, port, auth, user, path, query, ref);
        debug("jdbc driver setURL()");
    }

    private void debug(String s) {
        if(logger.isLoggable(Level.FINEST)){
            logger.finest("[osgi-jdbc] : " + s);
        }
    }

/*
    private URL getUberJarURL(URL[] urls) throws IOException, URISyntaxException {

        if (urls.length == 1) {
            URL u = urls[0];
            URI embeddedURI = new URI(u.toURI().getSchemeSpecificPart());
            return embeddedURI.toURL();
        }

        if (urls.length > 1) {
            File file = new File(new URI(urls[0].toURI().getSchemeSpecificPart()));
            JarFile jar = new JarFile(file);
            File uberFile = File.createTempFile("jdbc-driver-", ".jar");
            uberFile.deleteOnExit();

            try {
                FileOutputStream fos = new FileOutputStream(uberFile);
                JarOutputStream uberJarOS = new JarOutputStream(fos);

                byte buffer[] = new byte[1024];
                int bytesRead;

                try {
                    Enumeration entries = jar.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        InputStream is = jar.getInputStream(entry);
                        uberJarOS.putNextEntry(entry);
                        while ((bytesRead = is.read(buffer)) != -1) {
                            uberJarOS.write(buffer, 0, bytesRead);
                        }
                    }

                    // Add new file(s) to the end
                    for (int i = 1; i < urls.length; i++) {
                        URL url = urls[i];
                        File f = new File(url.toURI());
                        FileInputStream fis = new FileInputStream(f);

                        try {
                            JarEntry entry = new JarEntry(f.getName());
                            uberJarOS.putNextEntry(entry);

                            while ((bytesRead = fis.read(buffer)) != -1) {
                                uberJarOS.write(buffer, 0, bytesRead);
                            }
                        } finally {
                            fis.close();
                        }
                    }
                } catch (IOException ex) {
                    System.err.println
                            ("Operation aborted due to : " + ex);
                } finally {
                    try {
                        uberJarOS.close();
                    } catch (IOException ioe) {
                    }
                }
            } catch (IOException ex) {
                System.err.println(
                        "Can't access new file : " + ex);
            } finally {
                try {
                    jar.close();
                } catch (IOException ioe) {
                }

            }
            return uberFile.toURI().toURL();
        }
        throw new IllegalStateException("Atleast one jar file need to be specified");
    }



    private URL[] getURLs(URL u) throws MalformedURLException {
        String urls = u.toString();
        StringTokenizer tokenizer = new StringTokenizer(urls, ",");
        ArrayList<URL> urlList = new ArrayList<URL>();
        while (tokenizer.hasMoreElements()) {
            String s = (String) tokenizer.nextElement();
            URL url = new URL(s);
            urlList.add(url);
        }
        return urlList.toArray(new URL[urlList.size()]);
    }
*/

}
