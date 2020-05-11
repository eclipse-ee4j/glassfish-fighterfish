/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Properties;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.TopicConnectionFactory;

import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.osgi.framework.BundleContext;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.ResourceRef;

/**
 * Resource-Manager to export jms-connection-factories (JMS-RA Connector-Resources) in GlassFish to OSGi's
 * service-registry.
 */
public final class JMSResourceManager extends BaseResourceManager implements ResourceManager {

    /**
     * Create a new instance.
     *
     * @param habitat component locator
     */
    public JMSResourceManager(final Habitat habitat) {
        super(habitat);
    }

    @Override
    public void registerResources(final BundleContext context) {
        registerJmsResources(context);
    }

    /**
     * Iterates through all of the configured connector-resources of jms-ra and exposes them as OSGi service. The service
     * contract can be one of the following :<br>
     * <i>jakarta.jms.ConnectionFactory</i><br>
     * <i>jakarta.jms.QueueConnectionFactory</i><br>
     * <i>jakarta.jms.TopicConnectionFactory</i><br>
     * <br>
     *
     * @param context bundle-context
     */
    public void registerJmsResources(final BundleContext context) {
        Collection<ConnectorResource> connectorResources = getResources().getResources(ConnectorResource.class);
        for (ConnectorResource resource : connectorResources) {
            if (isJmsResource(resource)) {
                ResourceRef resRef = getResourceHelper().getResourceRef(resource.getJndiName());
                registerResource(resource, resRef, context);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerResource(final BindableResource resource, final ResourceRef resRef, final BundleContext bundleContext) {

        ConnectorResource connectorResource = (ConnectorResource) resource;
        if (connectorResource.getEnabled().equalsIgnoreCase("true")) {
            if (resRef != null && resRef.getEnabled().equalsIgnoreCase("true")) {
                String poolName = connectorResource.getPoolName();
                ConnectorConnectionPool pool = (ConnectorConnectionPool) getResources().getResourceByName(ConnectorConnectionPool.class, poolName);
                String defnName = pool.getConnectionDefinitionName();
                Class claz = null;
                Class[] intf = null;

                if (defnName.equals(Constants.QUEUE_CF)) {
                    claz = QueueConnectionFactory.class;
                    intf = new Class[] { QueueConnectionFactory.class, Invalidate.class };
                } else if (defnName.equals(Constants.TOPIC_CF)) {
                    claz = TopicConnectionFactory.class;
                    intf = new Class[] { TopicConnectionFactory.class, Invalidate.class };
                } else if (defnName.equals(Constants.UNIFIED_CF)) {
                    claz = ConnectionFactory.class;
                    intf = new Class[] { ConnectionFactory.class, Invalidate.class };
                } else {
                    throw new RuntimeException("Invalid connection-definition [ " + defnName + " ] for jms-resource [ " + resource.getJndiName() + " ]");
                }
                Dictionary properties = new Properties();
                properties.put(Constants.JNDI_NAME, connectorResource.getJndiName());
                Object o = getProxy(connectorResource.getJndiName(), intf, getClassLoader());

                registerResourceAsService(bundleContext, connectorResource, claz.getName(), properties, o);
            }
        }
    }

    @Override
    public boolean handlesResource(final BindableResource resource) {
        boolean result = false;
        if (resource instanceof ConnectorResource) {
            result = isJmsResource((ConnectorResource) resource);
        }
        return result;
    }

    /**
     * Test if the given resource is a JMS-RA resource.
     *
     * @param resource connector-resource
     * @return {@code true} if a JMS resource, {@code false} otherwise
     */
    private boolean isJmsResource(final ConnectorResource resource) {
        boolean result = false;
        String poolName = resource.getPoolName();
        ConnectorConnectionPool pool = (ConnectorConnectionPool) getResources().getResourceByName(ConnectorConnectionPool.class, poolName);
        String raName = pool.getResourceAdapterName();
        if (raName.equals(Constants.DEFAULT_JMS_ADAPTER)) {
            result = true;
        }
        return result;
    }
}
