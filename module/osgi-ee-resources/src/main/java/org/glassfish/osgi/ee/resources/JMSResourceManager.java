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
import com.sun.enterprise.config.serverbeans.ResourceRef;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.osgi.framework.BundleContext;

import javax.jms.ConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Properties;

/**
 * Resource-Manager to export jms-connection-factories (JMS-RA
 * Connector-Resources) in GlassFish to OSGi's service-registry
 *
 * @author Jagadish Ramu
 */
public class JMSResourceManager extends BaseResourceManager
        implements ResourceManager {

    public JMSResourceManager(Habitat habitat) {
        super(habitat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResources(BundleContext context) {
        registerJmsResources(context);
    }

    /**
     * Iterates through all of the configured connector-resources of jms-ra<br>
     * Exposes them as OSGi service by appropriate contract which can be one of
     * the following :<br>
     * <i>javax.jms.ConnectionFactory</i><br>
     * <i>javax.jms.QueueConnectionFactory</i><br>
     * <i>javax.jms.TopicConnectionFactory</i><br><br>
     *
     * @param context bundle-context
     */
    public void registerJmsResources(BundleContext context) {
        Collection<ConnectorResource> connectorResources = getResources()
                .getResources(ConnectorResource.class);
        for (ConnectorResource resource : connectorResources) {
            if (isJmsResource(resource)) {
                ResourceRef resRef = getResourceHelper()
                        .getResourceRef(resource.getJndiName());
                registerResource(resource, resRef, context);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void registerResource(BindableResource resource, ResourceRef resRef,
            BundleContext bundleContext) {

        ConnectorResource connectorResource = (ConnectorResource) resource;
        if (connectorResource.getEnabled().equalsIgnoreCase("true")) {
            if (resRef != null && resRef.getEnabled()
                    .equalsIgnoreCase("true")) {
                String poolName = connectorResource.getPoolName();
                ConnectorConnectionPool pool
                        = (ConnectorConnectionPool) getResources()
                                .getResourceByName(ConnectorConnectionPool.class,
                                        poolName);
                String defnName = pool.getConnectionDefinitionName();
                Class claz = null;
                Class intf[] = null;

                if (defnName.equals(Constants.QUEUE_CF)) {
                    claz = QueueConnectionFactory.class;
                    intf = new Class[]{QueueConnectionFactory.class,
                        Invalidate.class};
                } else if (defnName.equals(Constants.TOPIC_CF)) {
                    claz = TopicConnectionFactory.class;
                    intf = new Class[]{TopicConnectionFactory.class,
                        Invalidate.class};
                } else if (defnName.equals(Constants.UNIFIED_CF)) {
                    claz = ConnectionFactory.class;
                    intf = new Class[]{ConnectionFactory.class, Invalidate.class};
                } else {
                    throw new RuntimeException(
                            "Invalid connection-definition [ " + defnName + " ]"
                            + " for jms-resource [ "
                                    + resource.getJndiName() + " ]");
                }
                Dictionary properties = new Properties();
                properties.put(Constants.JNDI_NAME, connectorResource
                        .getJndiName());
                Object o = getProxy(connectorResource.getJndiName(), intf,
                        getClassLoader());

                registerResourceAsService(bundleContext, connectorResource,
                        claz.getName(), properties, o);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handlesResource(BindableResource resource) {
        boolean result = false;
        if (resource instanceof ConnectorResource) {
            result = isJmsResource((ConnectorResource) resource);
        }
        return result;
    }

    /**
     * determines whether the resource is a JMS-RA's resource
     *
     * @param resource connector-resource
     * @return boolean
     */
    private boolean isJmsResource(ConnectorResource resource) {
        boolean result = false;
        String poolName = resource.getPoolName();
        ConnectorConnectionPool pool = (ConnectorConnectionPool) getResources()
                .getResourceByName(ConnectorConnectionPool.class, poolName);
        String raName = pool.getResourceAdapterName();
        if (raName.equals(Constants.DEFAULT_JMS_ADAPTER)) {
            result = true;
        }
        return result;
    }
}
