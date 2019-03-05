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

import org.osgi.service.jdbc.DataSourceFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.osgi.framework.Constants.*;

public class JDBCJarManifestProcessor {

    private static final Logger LOGGER = Logger.getLogger(
            JDBCJarManifestProcessor.class.getPackage().getName());
    private static final String DEFAULT_MAN_VERSION = "2";
    private static final String IMPLEMENTATION_VERSION =
            "Implementation-Version";
    private static final Locale LOCALE = Locale.getDefault();

    public static final String OSGI_RFC_122 = "OSGI_RFC_122";

    /**
     * Reads content of the given URL, uses it to come up with a new Manifest.
     *
     * @param url URL which is used to read the original Manifest and other data
     * @return a new Manifest
     * @throws java.io.IOException
     */
    public static Manifest processManifest(URL url, ClassLoader cl)
            throws IOException {

        final JarInputStream jis = new JarInputStream(url.openStream());

        try {

            File file = new File(url.toURI());

            List<String> embeddedJars = getEmbeddedJarsList(file);
            StringBuffer bundleClassPath = deriveBundleClassPath(embeddedJars);

            JDBCDriverLoader loader = new JDBCDriverLoader(cl);
            Properties properties = loader.loadDriverInformation(file);

            Properties queryParams = readQueryParams(url);
            Manifest oldManifest = jis.getManifest();
            Manifest newManifest = new Manifest(oldManifest);
            Attributes attrs = newManifest.getMainAttributes();

            for (Map.Entry entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                key = key.replace('.', '_');
                attrs.putValue(key, value);
            }

            attrs.putValue((DataSourceFactory.OSGI_JDBC_DRIVER_CLASS
                    .replace('.', '_')),
                    (String) properties.get(Constants.DRIVER));

            attrs.putValue(OSGI_RFC_122, "TRUE");

            process(queryParams, attrs, BUNDLE_MANIFESTVERSION,
                    DEFAULT_MAN_VERSION);

            String defaultSymName = properties.getProperty(Constants.DRIVER);
            process(queryParams, attrs, BUNDLE_SYMBOLICNAME,
                    defaultSymName);

            String version = oldManifest.getMainAttributes()
                    .getValue(IMPLEMENTATION_VERSION);
            if (isOSGiCompatibleVersion(version)) {
                process(queryParams, attrs, BUNDLE_VERSION, version);
            }

            process(queryParams, attrs, BUNDLE_CLASSPATH,
                    bundleClassPath.toString());

            //process(queryParams, attrs, IMPORT_PACKAGE,
            // DEFAULT_IMPORT_PACKAGE);
            process(queryParams, attrs, EXPORT_PACKAGE, "*");

            // We add this attribute until we have added support for
            // scanning class bytes to figure out import dependencies.
            attrs.putValue(DYNAMICIMPORT_PACKAGE, "*");
            return newManifest;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            jis.close();
        }
    }

    private static boolean isOSGiCompatibleVersion(String version) {
        boolean isCompatible = false;
        try {
            if (version != null) {
                Double.parseDouble(version);
                isCompatible = true;
            }
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST,
                        "Not a OSGi compatible bundle-version [{0}] : {1}",
                        new Object[]{version, nfe});
            }
        }
        return isCompatible;
    }

    private static StringBuffer deriveBundleClassPath(
            List<String> embeddedJars) {

        StringBuffer bundleClasspath = new StringBuffer(".");
        for (int i = 0; i < embeddedJars.size(); i++) {
            bundleClasspath = bundleClasspath.append(",");
            bundleClasspath = bundleClasspath.append(embeddedJars.get(i));
        }
        return bundleClasspath;
    }

    private static List<String> getEmbeddedJarsList(File file)
            throws IOException {

        List<String> jarsList = new ArrayList<String>();
        JarFile f = new JarFile(file);
        Enumeration<JarEntry> entries = f.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName()
                    .toLowerCase(LOCALE).endsWith(".jar")) {
                jarsList.add(entry.getName());
            }
        }
        return jarsList;
    }

    private static Properties readQueryParams(URL url) {
        Properties queryParams = new Properties();
        String query = url.getQuery();
        if (query != null) {
            // "&" separates query paremeters
            StringTokenizer st = new StringTokenizer(query, "&");
            while (st.hasMoreTokens()) {
                String next = st.nextToken();
                int eq = next.indexOf("=");
                String name = next, value = null;
                if (eq != -1) {
                    name = next.substring(0, eq);
                    if ((eq + 1) < next.length()) {
                        value = next.substring(eq + 1);
                    }
                }
                queryParams.put(name, value);
            }
            LOGGER.logp(Level.INFO, "JDBCJarManifestProcessor",
                    "readQueryParams", "queryParams = {0}",
                    new Object[]{queryParams});
        }
        return queryParams;
    }

    private static void process(Properties deployerOptions,
            Attributes developerOptions,
            String key,
            String defaultOption) {
        String deployerOption = deployerOptions.getProperty(key);
        String developerOption = developerOptions.getValue(key);
        String finalOption = defaultOption;
        if (deployerOption != null) {
            finalOption = deployerOption;
        } else if (developerOption != null) {
            finalOption = developerOption;
        }
        if (finalOption == null ? developerOption != null : !finalOption
                .equals(developerOption)) {
            developerOptions.putValue(key, finalOption);
        }
    }
}
