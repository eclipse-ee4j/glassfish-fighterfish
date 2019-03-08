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

import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data source factory implementation.
 */
public final class DataSourceFactoryImpl implements DataSourceFactory {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            DataSourceFactoryImpl.class.getPackage().getName());

    /**
     * Default locale.
     */
    private static final Locale LOCALE = Locale.getDefault();

    /**
     * Represents the driver implementation class names for various types.
     * E.g. java.sql.Driver, javax.sql.DataSource,
     * javax.sql.ConnectionPoolDataSource, javax.sql.XADataSource
     */
    private final Dictionary header;

    /**
     * Bundle context of the driver.
     */
    private final BundleContext driverBundleContext;

    /**
     * Create a new instance.
     * @param context the driver bundle context
     */
    public DataSourceFactoryImpl(final BundleContext context) {
        this.header = context.getBundle().getHeaders();
        this.driverBundleContext = context;
    }

    @Override
    public DataSource createDataSource(final Properties props)
            throws SQLException {

        String dataSourceClass = (String) header
                .get(Constants.DS.replace('.', '_'));
        try {
            Class dsClass = driverBundleContext.getBundle()
                    .loadClass(dataSourceClass);
            DataSource ds = (DataSource) dsClass.newInstance();
            populateBean(props, dsClass, ds);
            return ds;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public ConnectionPoolDataSource createConnectionPoolDataSource(
            final Properties props) throws SQLException {

        String cpdsClassName = (String) header
                .get(Constants.CPDS.replace('.', '_'));
        try {
            Class cpdsClass = driverBundleContext.getBundle()
                    .loadClass(cpdsClassName);
            ConnectionPoolDataSource cpds = (ConnectionPoolDataSource)
                    cpdsClass.newInstance();
            populateBean(props, cpdsClass, cpds);
            return cpds;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public XADataSource createXADataSource(final Properties props)
            throws SQLException {

        String xadsClassName = (String) header
                .get(Constants.XADS.replace('.', '_'));
        try {
            Class xadsClass = driverBundleContext.getBundle()
                    .loadClass(xadsClassName);
            XADataSource xads = (XADataSource) xadsClass.newInstance();
            populateBean(props, xadsClass, xads);
            return xads;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Driver createDriver(final Properties props) throws SQLException {

        String driverClassName = (String) header
                .get(Constants.DRIVER.replace('.', '_'));
        try {
            Class driverClass = driverBundleContext.getBundle()
                    .loadClass(driverClassName);
            Driver driver = (Driver) driverClass.newInstance();
            populateBean(props, driverClass, driver);
            //register the driver with JDBC Driver Manager
            Class.forName(driverClassName, false,
                    driverClass.getClassLoader());
            return driver;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Populate the given bean.
     * @param properties config properties
     * @param clazz the bean class
     * @param object the bean instance
     * @throws IntrospectionException if an error occurs during reflection
     * @throws IllegalAccessException if an error occurs during reflection
     * @throws InvocationTargetException if an error occurs during reflection
     * @throws SQLException if some setter are not found / available
     */
    private void populateBean(final Properties properties, final Class clazz,
            final Object object) throws IntrospectionException,
            IllegalAccessException, InvocationTargetException, SQLException {

        if (properties == null) {
            // nothing to do. DSF.createXXX allows null value
            return;
        }
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);

        PropertyDescriptor[] propertyDescriptors = beanInfo
                .getPropertyDescriptors();

        Set keys = properties.keySet();
        Iterator keyIterator = keys.iterator();

        while (keyIterator.hasNext()) {

            String propertyName = (String) keyIterator.next();
            String value = properties.getProperty(propertyName);
            boolean propertyFound = false;

            for (PropertyDescriptor desc : propertyDescriptors) {
                if (desc.getName().equalsIgnoreCase(propertyName)) {
                    String type = desc.getPropertyType().getName();
                    Object result = null;

                    if (type != null) {
                        type = type.toUpperCase(LOCALE);
                        try {
                            if (type.endsWith("INT")
                                    || type.endsWith("INTEGER")) {
                                result = Integer.valueOf(value);
                            } else if (type.endsWith("LONG")) {
                                result = Long.valueOf(value);
                            } else if (type.endsWith("DOUBLE")) {
                                result = Double.valueOf(value);
                            } else if (type.endsWith("FLOAT")) {
                                result = Float.valueOf(value);
                            } else if (type.endsWith("CHAR")
                                    || type.endsWith("CHARACTER")) {
                                result = value.charAt(0);
                            } else if (type.endsWith("SHORT")) {
                                result = Short.valueOf(value);
                            } else if (type.endsWith("BYTE")) {
                                result = Byte.valueOf(value);
                            } else if (type.endsWith("BOOLEAN")) {
                                result = Boolean.valueOf(value);
                            } else if (type.endsWith("STRING")) {
                                result = value;
                            }
                        } catch (Exception e) {
                            throw new SQLException(e);
                        }
                    } else {
                        throw new SQLException(
                                "Unable to find the type of property [ "
                                + propertyName + " ]");
                    }

                    Method setter = desc.getWriteMethod();
                    if (setter != null) {
                        propertyFound = true;
                        debug("invoking setter method [" + setter.getName()
                                + "], value [" + result + "]");
                        setter.invoke(object, result);
                    } else {
                        throw new SQLException(
                                "Unable to find the setter method for"
                                + " property [ " + propertyName + " ]");
                    }
                    break;
                }
            }
            if (!propertyFound) {
                throw new SQLException("No such property (" + propertyName
                        + ") in " + clazz.getName());
            }
        }
    }

    /**
     * Hook for pre destroy action.
     */
    public void preDestroy() {
        debug("predestroy() called");
    }

    /**
     * Log a {@code FINE} message.
     * @param msg message to log
     */
    private static void debug(final String msg) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-jdbc] : {0}", msg);
        }
    }
}
