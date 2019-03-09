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

import com.sun.enterprise.util.SystemPropertyConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import static org.glassfish.osgijdbc.Constants.DRIVER;

/**
 * Utility to load JDBC drivers.
 */
public final class JDBCDriverLoader {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            JDBCDriverLoader.class.getPackage().getName());

    /**
     * Constant for JDBC driver SPI file.
     */
    private static final String META_INF_SERVICES_DRIVER_FILE =
            "META-INF/services/java.sql.Driver";

    /**
     * Constant for GlassFish vendor mapping file location.
     */
    private static final String DBVENDOR_MAPPINGS_ROOT = System.getProperty(
            SystemPropertyConstants.INSTALL_ROOT_PROPERTY) + File.separator
            + "lib" + File.separator + "install" + File.separator + "databases"
            + File.separator + "dbvendormapping" + File.separator;

    /**
     * Constant for the data source properties file name.
     */
    private static final String DS_PROPERTIES = "ds.properties";

    /**
     * Constant for the connection pool data source properties file name.
     */
    private static final String CPDS_PROPERTIES = "cpds.properties";

    /**
     * Constant for the transaction properties file name.
     */
    private static final String XADS_PROPERTIES = "xads.properties";

    /**
     * Constant for the driver properties file name.
     */
    private static final String DRIVER_PROPERTIES = "driver.properties";

    /**
     * Constant for the db vendor properties file name.
     */
    private static final String VENDOR_PROPERTIES = "dbvendor.properties";

    /**
     * Default locale.
     */
    private static final Locale LOCALE = Locale.getDefault();

    /**
     * Db vendor mappings.
     */
    public static final Map<String, Map<String, String>> DB_VENDOR_MAPPINGS =
            new HashMap<String, Map<String, String>>();

    /**
     * Load all mappings.
     */
    static {
        loadMappings();
    }

    /**
     * Class-loader to use for loading the driver.
     */
    private final ClassLoader cl;

    /**
     * Create a new instance.
     * @param cloader class-loader to use
     */
    public JDBCDriverLoader(final ClassLoader cloader) {
        this.cl = cloader;
    }

    /**
     * Load the configuration properties from all files.
     */
    @SuppressWarnings("unchecked")
    private static void loadMappings() {
        DB_VENDOR_MAPPINGS.put(Constants.DS,
                ((Map) loadProperties(DS_PROPERTIES)));
        DB_VENDOR_MAPPINGS.put(Constants.CPDS,
                ((Map) loadProperties(CPDS_PROPERTIES)));
        DB_VENDOR_MAPPINGS.put(Constants.XADS,
                ((Map) loadProperties(XADS_PROPERTIES)));
        DB_VENDOR_MAPPINGS.put(Constants.DRIVER,
                ((Map) loadProperties(DRIVER_PROPERTIES)));
        DB_VENDOR_MAPPINGS.put(Constants.DBVENDOR,
                ((Map) loadProperties(VENDOR_PROPERTIES)));
    }

    /**
     * Load config properties for a config source.
     * @param type the config source
     * @return Properties
     */
    private static Properties loadProperties(final String type) {
        Properties fileProperties = new Properties();
        String fileName = DBVENDOR_MAPPINGS_ROOT + type;
        File mappingFile = new File(fileName);

        if (mappingFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(mappingFile);
                try {
                    fileProperties.load(fis);
                } finally {
                    fis.close();
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.FINE,
                        "IO Exception while loading properties file [ {0} ]"
                                + " : {1}",
                        new Object[]{mappingFile.getAbsolutePath(), ioe});
            }
        } else {
            LOGGER.log(Level.WARNING, "File not found : {0}",
                    mappingFile.getAbsolutePath());
        }
        return fileProperties;
    }

    /**
     * Get a set of common database vendor names supported in GlassFish.
     *
     * @return database vendor names set.
     */
    @SuppressWarnings("unchecked")
    private Set<String> getDatabaseVendorNames() {
        Map vendors = DB_VENDOR_MAPPINGS.get(Constants.DBVENDOR);
        return vendors.keySet();
    }

    /**
     * Get the db vendor mapping for the given class name and type.
     * @param className requested class name
     * @param type requested type
     * @return vendor mapping
     */
    private String getDBVendor(final String className, final String type) {
        String dbVendor = null;
        Map map = DB_VENDOR_MAPPINGS.get(type);
        Set entrySet = map.entrySet();
        Iterator entryIterator = entrySet.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIterator.next();
            if (((String) entry.getValue()).equalsIgnoreCase(className)) {
                dbVendor = (String) entry.getKey();
                break;
            }
        }
        return dbVendor;
    }

    /**
     * Get the implementation class from the given mapping.
     * @param dbVendor the request mapping
     * @param resType the request type
     * @return the implementation class name
     */
    private String getImplClassNameFromMapping(final String dbVendor,
            final String resType) {

        Map fileProperties = DB_VENDOR_MAPPINGS.get(resType);
        if (fileProperties == null) {
            throw new IllegalStateException(
                    "Unknown resource type [ " + resType + " ]");
        }
        return (String) fileProperties.get(dbVendor.toUpperCase(LOCALE));
    }

    /**
     * Load driver info from file.
     * @param file input file
     * @return Properties
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public Properties loadDriverInformation(final File file) {
        String implClass;
        JarFile jarFile = null;

        Properties properties = new Properties();
        try {
            jarFile = new JarFile(file);
            Enumeration e = jarFile.entries();
            while (e.hasMoreElements()) {

                ZipEntry zipEntry = (ZipEntry) e.nextElement();

                if (zipEntry != null) {

                    String entry = zipEntry.getName();
                    if (META_INF_SERVICES_DRIVER_FILE.equals(entry)) {

                        InputStream metaInf = jarFile.getInputStream(zipEntry);
                        implClass = processMetaInfServicesDriverFile(metaInf);
                        if (implClass != null) {
                            if (isLoaded(implClass, Constants.DRIVER, cl)) {
                                String vendor = getVendorFromManifest(file);

                                Set<String> dbVendorNames =
                                        getDatabaseVendorNames();
                                if (vendor != null
                                        && dbVendorNames.contains(vendor)) {

                                    String dsClassName =
                                            getImplClassNameFromMapping(vendor,
                                                    Constants.DS);
                                    String cpdsClassName =
                                            getImplClassNameFromMapping(vendor,
                                                    Constants.CPDS);
                                    String xadsClassName =
                                            getImplClassNameFromMapping(vendor,
                                                    Constants.XADS);
                                    // String driverClassName =
                                    // getImplClassNameFromMapping(vendor,
                                    // DRIVER);
                                    String driverClassName = implClass;

                                    properties.put(Constants.DS, dsClassName);
                                    properties.put(Constants.CPDS,
                                            cpdsClassName);
                                    properties.put(Constants.XADS,
                                            xadsClassName);
                                    properties.put(Constants.DRIVER,
                                            driverClassName);

                                    return properties;

                                } else if (vendor != null) {

                                    Set<String> dsClasses =
                                            getImplClassesByIteration(file,
                                                    Constants.DS, vendor, cl);
                                    if (dsClasses.size() == 1) {
                                        properties.put(Constants.DS,
                                                dsClasses.toArray()[0]);
                                    }

                                    Set<String> cpdsClasses =
                                            getImplClassesByIteration(file,
                                                    Constants.CPDS, vendor, cl);
                                    if (cpdsClasses.size() == 1) {
                                        properties.put(Constants.CPDS,
                                                cpdsClasses.toArray()[0]);
                                    }

                                    Set<String> xadsClasses =
                                            getImplClassesByIteration(file,
                                                    Constants.XADS, vendor, cl);
                                    if (xadsClasses.size() == 1) {
                                        properties.put(Constants.XADS,
                                                xadsClasses.toArray()[0]);
                                    }
                                    properties.put(Constants.DRIVER,
                                            implClass);

                                    return properties;
                                }
                            }
                        }
                        LOGGER.log(Level.FINEST,
                                "Driver loader : implClass = {0}", implClass);
                    }
                    if (entry.endsWith(".class")) {
                        //Read from MANIFEST.MF file for all jdbc40 drivers and
                        // resType
                        //java.sql.Driver.TODO : this should go outside .class
                        // check.
                        //TODO : Some classnames might not have these strings
                        //in their classname. Logic should be flexible in such
                        // cases.
                        if (entry.toUpperCase(LOCALE).contains("DATASOURCE")) {
                            implClass = getClassName(entry);
                            if (implClass != null) {
                                if (isLoaded(implClass, Constants.XADS, cl)) {
                                    String dbVendor = getDBVendor(implClass,
                                            Constants.XADS);
                                    if (dbVendor != null) {
                                        detectImplClasses(properties, dbVendor);
                                        return properties;
                                    } else {
                                        properties.put(Constants.XADS,
                                                implClass);
                                    }
                                } else if (isLoaded(implClass, Constants.CPDS,
                                        cl)) {
                                    String dbVendor = getDBVendor(implClass,
                                            Constants.CPDS);
                                    if (dbVendor != null) {
                                        detectImplClasses(properties,
                                                dbVendor);
                                        return properties;
                                    } else {
                                        properties.put(Constants.CPDS,
                                                implClass);
                                    }
                                } else if (isLoaded(implClass, Constants.DS,
                                        cl)) {
                                    String dbVendor = getDBVendor(implClass,
                                            Constants.DS);
                                    if (dbVendor != null) {
                                        detectImplClasses(properties, dbVendor);
                                        return properties;
                                    } else {
                                        properties.put(Constants.DS, implClass);
                                    }
                                }
                            }
                        } else if (entry.toUpperCase(LOCALE)
                                .contains("DRIVER")) {
                            implClass = getClassName(entry);
                            if (implClass != null) {
                                if (isLoaded(implClass, Constants.DRIVER,
                                        cl)) {
                                    String dbVendor = getDBVendor(implClass,
                                            Constants.DRIVER);
                                    if (dbVendor != null) {
                                        detectImplClasses(properties,
                                                dbVendor);
                                        return properties;
                                    } else {
                                        properties.put(Constants.DRIVER,
                                                implClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Error while getting Jdbc driver classnames ", ex);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "Exception while closing JarFile '"
                            + jarFile.getName() + "' :", ex);
                }
            }
        }
        if (properties.get(Constants.DRIVER) != null) {
            return properties;
        } else {
            throw new RuntimeException("Unable to introspect jar [ "
                    + file.getName() + " ] for Driver Class, "
                    + "no implementation for java.sql.Driver is found");
        }
    }

    /**
     * Detect implementation classes.
     * @param properties result properties
     * @param dbVendor the requested vendor
     */
    private void detectImplClasses(final Properties properties,
            final String dbVendor) {

        String xads = getImplClassNameFromMapping(dbVendor, Constants.XADS);
        String cpds = getImplClassNameFromMapping(dbVendor, Constants.CPDS);
        String ds = getImplClassNameFromMapping(dbVendor, Constants.DS);
        String driver = getImplClassNameFromMapping(dbVendor,
                Constants.DRIVER);

        properties.put(Constants.XADS, xads);
        properties.put(Constants.CPDS, cpds);
        properties.put(Constants.DS, ds);
        properties.put(Constants.DRIVER, driver);
    }

    /**
     * Get the implementation classes iteratively.
     * @param file the input file
     * @param resType the requested type
     * @param dbVendor the requested vendor
     * @param cloader the class-loader to use
     * @return list of implementation class names
     */
    private Set<String> getImplClassesByIteration(final File file,
            final String resType, final String dbVendor,
            final ClassLoader cloader) {

        SortedSet<String> implClassNames = new TreeSet<String>();
        String implClass;
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);
            Enumeration e = jarFile.entries();
            while (e.hasMoreElements()) {

                ZipEntry zipEntry = (ZipEntry) e.nextElement();
                if (zipEntry != null) {

                    String entry = zipEntry.getName();
                    if (DRIVER.equals(resType)) {
                        if (META_INF_SERVICES_DRIVER_FILE.equals(entry)) {

                            InputStream inputStream = jarFile
                                    .getInputStream(zipEntry);
                            implClass = processMetaInfServicesDriverFile(
                                    inputStream);
                            if (implClass != null) {
                                if (isLoaded(implClass, resType, cloader)) {
                                    //Add to the implClassNames only if vendor
                                    // name matches.
                                    if (isVendorSpecific(file, dbVendor,
                                            implClass)) {
                                        implClassNames.add(implClass);
                                    }
                                }
                            }
                            LOGGER.log(Level.FINEST,
                                    "Driver loader : implClass = {0}",
                                    implClass);

                        }
                    }
                    if (entry.endsWith(".class")) {
                        //Read from metainf file for all jdbc40 drivers and
                        // resType
                        //java.sql.Driver.TODO : this should go outside .class
                        // check.
                        //TODO : Some classnames might not have these strings
                        //in their classname. Logic should be flexible in such
                        // cases.
                        if (entry.toUpperCase(LOCALE).contains("DATASOURCE")
                                || entry.toUpperCase(LOCALE)
                                        .contains("DRIVER")) {
                            implClass = getClassName(entry);
                            if (implClass != null) {
                                if (isLoaded(implClass, resType, cloader)) {
                                    if (isVendorSpecific(file, dbVendor,
                                            implClass)) {
                                        implClassNames.add(implClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Error while getting Jdbc driver classnames ", ex);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "Exception while closing JarFile '"
                            + jarFile.getName() + "' :", ex);
                }
            }
        }
        // Could be one or many depending on the connection definition class
        // name
        return implClassNames;
    }

    /**
     * Test if the given class is not abstract.
     * @param cls the class to check
     * @return {@code true} if the class is not abstract, {@code false}
     * otherwise
     */
    private boolean isNotAbstract(final Class cls) {
        int modifier = cls.getModifiers();
        return !Modifier.isAbstract(modifier);
    }

    /**
     * Reads the META-INF/services/java.sql.Driver file contents and returns the
     * driver implementation class name. In case of JDBC-4.0 drivers or above,
     * the META-INF/services/java.sql.Driver file contains the name of the
     * driver class.
     *
     * @param inputStream input stream
     * @return driver implementation class name
     */
    private String processMetaInfServicesDriverFile(
            final InputStream inputStream) {

        String driverClassName = null;
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                driverClassName = line;
            }
        } catch (IOException ioex) {
            LOGGER.log(Level.FINEST,
                    "DriverLoader : exception while processing META-INF"
                    + " directory for DriverClassName {0}",
                    ioex);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE,
                        "Error while closing file handle after reading"
                        + " META-INF file : ",
                        ex);
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE,
                        "Error while closing file handle after reading"
                        + " META-INF file : ",
                        ex);
            }
        }
        return driverClassName;
    }

    /**
     * Check if the class has been loaded and if it is a Driver or a DataSource
     * impl.
     *
     * @param classname class
     * @param resType request type
     * @param loader class-loader to use
     * @return boolean indicating whether the class is loaded or not
     */
    private boolean isLoaded(final String classname, final String resType,
            final ClassLoader loader) {
        Class cls;
        try {
            cls = loader.loadClass(classname);
        } catch (Throwable t) {
            cls = null;
        }
        return (isResType(cls, resType));
    }

    /**
     * Find if the particular class has any implementations of java.sql.Driver
     * or javax.sql.DataSource or any other resTypes passed.
     *
     * @param cls class to be introspected
     * @param resType resource-type
     * @return boolean indicating the status
     */
    private boolean isResType(final Class cls, final String resType) {
        boolean isResType = false;
        if (cls != null) {
            if (Constants.DS.equals(resType)) {
                if (javax.sql.DataSource.class.isAssignableFrom(cls)) {
                    isResType = isNotAbstract(cls);
                }
            } else if (Constants.CPDS.equals(resType)) {
                if (javax.sql.ConnectionPoolDataSource.class
                        .isAssignableFrom(cls)) {
                    isResType = isNotAbstract(cls);
                }
            } else if (Constants.XADS.equals(resType)) {
                if (javax.sql.XADataSource.class.isAssignableFrom(cls)) {
                    isResType = isNotAbstract(cls);
                }
            } else if (Constants.DRIVER.equals(resType)) {
                if (java.sql.Driver.class.isAssignableFrom(cls)) {
                    isResType = isNotAbstract(cls);
                }
            }
        }
        return isResType;
    }

    /**
     * Get the class name from a JAR entry name.
     * @param entryName the JAR entry name
     * @return class name
     */
    private String getClassName(final String entryName) {
        String zeClassName =  entryName.replaceAll("/", ".");
        zeClassName = zeClassName.substring(0,
                entryName.lastIndexOf(".class"));
        return zeClassName;
    }

    /**
     * Test if a class name is vendor specific.
     * @param file JAR file
     * @param dbVendor request vendor
     * @param className class name to test
     * @return {@code true} if the class is vendor specific, {@code false}
     * otherwise
     */
    private boolean isVendorSpecific(final File file, final String dbVendor,
            final String className) {

        //File could be a jdbc jar file or a normal jar file
        boolean isVendorSpecific = false;
        String vendor = getVendorFromManifest(file);

        if (vendor == null) {
            //might have to do this part by going through the class names or
            // some other method.
            //dbVendor might be used in this portion
            if (isVendorSpecific(dbVendor, className)) {
                isVendorSpecific = true;
            }
        } else {
            //Got from Manifest file.
            if (vendor.equalsIgnoreCase(dbVendor)
                    || vendor.toUpperCase(LOCALE).contains(dbVendor
                            .toUpperCase(LOCALE))) {
                isVendorSpecific = true;
            }
        }
        return isVendorSpecific;
    }

    /**
     * Utility method that checks if a classname is vendor specific. This method
     * is used for jar files that do not have a manifest file to look up the
     * classname.
     *
     * @param dbVendor DB vendor name
     * @param className class name
     * @return true if the className in question is vendor specific.
     */
    private boolean isVendorSpecific(final String dbVendor,
            final String className) {

        return className.toUpperCase(LOCALE)
                .contains(dbVendor.toUpperCase(LOCALE));
    }

    /**
     * Get a vendor name from a Manifest entry in the file.
     *
     * @param f file
     * @return null if no manifest entry found.
     */
    @SuppressWarnings("deprecation")
    private String getVendorFromManifest(final File f) {
        String vendor = null;
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(f);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes != null) {
                    vendor = mainAttributes.getValue(Attributes.Name
                            .IMPLEMENTATION_VENDOR.toString());
                    if (vendor == null) {
                        vendor = mainAttributes.getValue(Attributes.Name
                                .IMPLEMENTATION_VENDOR_ID.toString());
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Exception while reading manifest file : ", ex);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "Exception while closing JarFile '"
                            + jarFile.getName() + "' :", ex);
                }
            }
        }
        return vendor;
    }
}
