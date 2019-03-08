/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgi.ee.resources;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.config.JdbcResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Properties;

/**
 * Resource-Manager to export JDBC resources in GlassFish to OSGi's
 * service-registry
 *
 * @author Jagadish Ramu
 */
public class JDBCResourceManager extends BaseResourceManager
        implements ResourceManager {

    public JDBCResourceManager(Habitat habitat) {
        super(habitat);
    }

    @Override
    public void registerResources(BundleContext context) {
        registerJdbcResources(context);
    }

    /**
     * Iterates through all of the configured jdbc-resources <br>
     * Exposes them as OSGi service by contract "javax.sql.DataSource"
     */
    private void registerJdbcResources(BundleContext context) {
        Resources resources = getHabitat().getComponent(Domain.class)
                .getResources();
        Collection<JdbcResource> jdbcResources = resources
                .getResources(JdbcResource.class);
        for (JdbcResource resource : jdbcResources) {
            ResourceRef resRef = getResourceHelper()
                    .getResourceRef(resource.getJndiName());
            registerJdbcResource(resource, resRef, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResource(BindableResource resource, ResourceRef resRef,
            BundleContext bundleContext) {

        registerJdbcResource((JdbcResource) resource, resRef, bundleContext);
    }

    /**
     * Retrieves driver-class-name information so as to register the service
     * with parameter <i>osgi.jdbc.driver.class</i><br>
     *
     * @param resource jdbc-resource
     * @param resRef resource-ref
     */
    @SuppressWarnings("unchecked")
    private void registerJdbcResource(JdbcResource resource, ResourceRef resRef,
            BundleContext bundleContext) {

        if (resource.getEnabled().equalsIgnoreCase("true")) {
            if (resRef != null && resRef.getEnabled().equalsIgnoreCase("true")) {
                String poolName = resource.getPoolName();
                JdbcConnectionPool pool = (JdbcConnectionPool) getResources()
                        .getResourceByName(JdbcConnectionPool.class, poolName);
                String className = pool.getDatasourceClassname();
                // no need to use res-type to get driver/datasource-classname
                // as either datasource-classname or driver-classname
                // must be not null.
                if (className == null) {
                    className = pool.getDriverClassname();
                }
                Class[] intf = new Class[]{javax.sql.DataSource.class,
                    Invalidate.class};
                Object proxy = getProxy(resource.getJndiName(), intf,
                        getClassLoader());
                Dictionary properties = new Properties();
                properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS,
                        className);
                properties.put(Constants.JNDI_NAME, resource.getJndiName());

                registerResourceAsService(bundleContext, resource,
                        Constants.DS, properties, proxy);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handlesResource(BindableResource resource) {
        return resource instanceof JdbcResource;
    }
}
