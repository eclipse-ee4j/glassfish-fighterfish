/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.jvnet.hk2.config.*;
import org.osgi.framework.BundleContext;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A service to export resources in GlassFish to OSGi's service-registry.<br>
 * OSGi applications can use <i>ServiceReference</i> to get access to these
 * resources. OSGi applications can do lookup of appropriate type of<br>
 * <i>ServiceReference</i> with the filter <i>"jndi-name"</i> <br><br>
 * For JDBC Resources, additional filter <i>"osgi.jdbc.driver.class"</i> that
 * indicates the<br>
 * driver-class-name/datasource-class-name will work.
 * <p/>
 * JDBC Resources, JMS Connection Factories, JMS Destinations are exported with
 * following <i>ServiceReference</i> names<br>
 * For JDBC Resources : <i>javax.sql.DataSource</i>  <br>
 * For JMS Resources : <i>javax.jms.ConnectionFactory /
 * javax.jms.QueueConnectionFactory / javax.jms.TopicConnectionFactory</i> <br>
 * For JMS Destinations : <i>javax.jms.Queue / javax.jms.Topic</i> <br>
 *
 * @author Jagadish Ramu
 */
public class ResourceProviderService implements ConfigListener {

    private static final Logger LOGGER = Logger.getLogger(
            ResourceProviderService.class.getPackage().getName());

    private final Habitat habitat;
    private final Resources resources;
    private final Servers servers;
    //config-bean proxy objects so as to listen to changes to these configuration.
    private ObservableBean serverConfigBean;
    private ObservableBean resourcesConfigBean;
    private final BundleContext bundleContext;
    private final ResourceHelper resourceHelper;
    private final Collection<ResourceManager> resourceManagers;


    public ResourceProviderService(Habitat habitat, BundleContext bundleContext) {
        this.habitat = habitat;
        this.bundleContext = bundleContext;
        servers = habitat.getComponent(Servers.class);
        resources = habitat.getComponent(Domain.class).getResources();
        resourceHelper = new ResourceHelper(habitat);
        resourceManagers = new ArrayList<ResourceManager>();
        initializeResourceManagers();
        postConstruct();
    }

    private void initializeResourceManagers() {
        resourceManagers.add(new JDBCResourceManager(habitat));
        if (runtimeSupportsJMS()) {
            registerJMSResources(resourceManagers, habitat);
        }
    }

    public void registerResources() {
        for (ResourceManager rm : resourceManagers) {
            rm.registerResources(bundleContext);
        }
    }

    public void unRegisterResources() {
        for (ResourceManager rm : resourceManagers) {
            rm.unRegisterResources(bundleContext);
        }
        preDestroy();
    }

    /**
     * un-register config bean proxy change listeners
     */
    public void preDestroy() {
        if (serverConfigBean != null) {
            serverConfigBean.removeListener(this);
        }
        if (resourcesConfigBean != null) {
            resourcesConfigBean.removeListener(this);
        }
    }

    /**
     * register config bean proxy change listeners
     */
    public final void postConstruct() {
        List<Server> serversList = servers.getServer();
        ServerContext context = habitat.getComponent(ServerContext.class);
        String instanceName = context.getInstanceName();
        for (Server server : serversList) {
            if (server.getName().equals(instanceName)) {
                serverConfigBean = (ObservableBean) ConfigSupport
                        .getImpl((ConfigBeanProxy) server);
                serverConfigBean.addListener(this);
            }
        }

        resourcesConfigBean = (ObservableBean) ConfigSupport
                .getImpl((ConfigBeanProxy) resources);
        resourcesConfigBean.addListener(this);

    }

    /**
     * Notification that @Configured objects that were injected have changed
     * @return
     * @param events list of changes
     */
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events,
                new PropertyChangeHandler(events, this), LOGGER);
    }

    private static class PropertyChangeHandler implements Changed {

        private final PropertyChangeEvent[] events;
        private final ResourceProviderService rps;

        private PropertyChangeHandler(PropertyChangeEvent[] events,
                ResourceProviderService rps) {

            this.events = events;
            this.rps = rps;
        }

        @Override
        public <T extends ConfigBeanProxy> NotProcessed changed(
                Changed.TYPE type, Class<T> changedType, T changedInstance) {

            NotProcessed np;
            try {
                switch (type) {
                    case ADD:
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST,
                                    "A new {0} was added : {1}",
                                    new Object[]{changedType.getName(),
                                        changedInstance});
                        }
                        np = handleAddEvent(changedInstance);
                        break;

                    case CHANGE:
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST,
                                    "A {0} was changed : {1}",
                                    new Object[]{changedType.getName(),
                                        changedInstance});
                        }
                        np = handleChangeEvent(changedInstance);
                        break;

                    case REMOVE:
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST,
                                    "A {0} was removed : {1}",
                                    new Object[]{changedType.getName(),
                                        changedInstance});
                        }
                        np = handleRemoveEvent(changedInstance);
                        break;

                    default:
                        np = new NotProcessed(
                                "Unrecognized type of change: " + type);
                        break;
                }
                return np;
            } finally {
            }

        }

        private <T extends ConfigBeanProxy> NotProcessed handleRemoveEvent(
                T removedInstance) {

            if (removedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) removedInstance;
                String resourceName = resourceRef.getRef();
                BindableResource resource = (BindableResource) rps.resources
                        .getResourceByName(BindableResource.class,
                                resourceName);
                unRegisterResource(resource);
            } else if (removedInstance instanceof BindableResource) {
                //since delete resource-ref event will not work
                // (resource related configuration
                //information won't be available during resource-ref
                // deletion event), handling
                // un-register of service here also.
                unRegisterResource((BindableResource) removedInstance);
            }
            return null;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(
                T changedInstance) {

            //TODO Handle other attribute changes (jndi-name)
            if (changedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) changedInstance;
                String refName = resourceRef.getRef();

                for (PropertyChangeEvent event : events) {
                    String propertyName = event.getPropertyName();
                    if ("enabled".equalsIgnoreCase(propertyName)) {
                        boolean newValue = Boolean.parseBoolean(
                                event.getNewValue().toString());
                        boolean oldValue = Boolean.parseBoolean(
                                event.getOldValue().toString());
                        //make sure that there is state change
                        if (!(newValue && oldValue)) {
                            BindableResource bindableResource
                                    = (BindableResource) rps.resources
                                            .getResourceByName(
                                                    BindableResource.class,
                                                    refName);
                            if (newValue) {
                                registerResource(bindableResource, resourceRef);
                            } else {
                                unRegisterResource(bindableResource);
                            }
                        }
                    }
                }
            } else if (changedInstance instanceof BindableResource) {
                BindableResource bindableResource = (BindableResource)
                        changedInstance;
                for (PropertyChangeEvent event : events) {
                    String propertyName = event.getPropertyName();
                    if ("enabled".equalsIgnoreCase(propertyName)) {
                        boolean newValue = Boolean.parseBoolean(
                                event.getNewValue().toString());
                        boolean oldValue = Boolean.parseBoolean(
                                event.getOldValue().toString());
                        //make sure that there is state change
                        if (!(newValue && oldValue)) {
                            if (newValue) {
                                registerResource(bindableResource);
                            } else {
                                unRegisterResource(bindableResource);
                            }
                        }
                    } else {

                        // this block handles any change under resource
                        // configuration apart from enable/disable.
                        Object newValueObject = event.getNewValue();
                        Object oldValueObject = event.getOldValue();
                        String newValue = "";
                        String oldValue = "";

                        if (newValueObject != null) {
                            newValue = newValueObject.toString();
                        }

                        if (oldValueObject != null) {
                            oldValue = oldValueObject.toString();
                        }

                        if (!newValue.equals(oldValue)) {
                            unRegisterResource(bindableResource);
                            registerResource(bindableResource);
                        }

                    }
                }
            } else if (changedInstance instanceof JdbcConnectionPool
                    || changedInstance instanceof ConnectorConnectionPool) {
                // this block handles any configuration change under connection
                // pool,
                // it's re-registering all resources which uses that connection
                // pool.
                String poolName = ((ResourcePool) changedInstance).getName();
                Resources resources = rps.habitat.getComponent(Domain.class)
                        .getResources();
                Collection<BindableResource> bindableResources = ConnectorsUtil
                        .getResourcesOfPool(resources, poolName);
                reRegisterResource(bindableResources);
            }
            return null;
        }

        /**
         * This method un-register and register resource again.
         *
         * @param bindableResources
         */
        private void reRegisterResource(
                Collection<BindableResource> bindableResources) {

            for (BindableResource resource : bindableResources) {
                if (Boolean.valueOf(resource.getEnabled())) {
                    ResourceRef resRef = rps.resourceHelper
                            .getResourceRef(resource.getJndiName());
                    if (resRef != null && Boolean.valueOf(resRef.getEnabled())) {
                        unRegisterResource(resource);
                        registerResource(resource);
                    }
                }
            }
        }

        private void unRegisterResource(BindableResource bindableResource) {
            Collection<ResourceManager> resourceManagers =
                    rps.getResourceManagers(bindableResource);
            for (ResourceManager rm : resourceManagers) {
                ResourceRef ref = rps.resourceHelper
                        .getResourceRef(bindableResource.getJndiName());
                rm.unRegisterResource(bindableResource, ref, rps.bundleContext);
            }
        }

        private void registerResource(BindableResource bindableResource,
                ResourceRef ref) {
            Collection<ResourceManager> resourceManagers =
                    rps.getResourceManagers(bindableResource);
            for (ResourceManager rm : resourceManagers) {
                rm.registerResource(bindableResource, ref, rps.bundleContext);
            }
        }

        private void registerResource(BindableResource bindableResource) {
            ResourceRef ref = rps.resourceHelper
                    .getResourceRef(bindableResource.getJndiName());
            registerResource(bindableResource, ref);
        }

        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent(
                T addedInstance) {

            if (addedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) addedInstance;
                String resourceName = resourceRef.getRef();
                BindableResource resource = (BindableResource) rps.resources
                        .getResourceByName(BindableResource.class,
                                resourceName);
                if (resource != null) {
                    registerResource(resource, resourceRef);
                }
            }
            return null;
        }
    }

    /**
     * get the list of resource-managers that can handle the resource
     *
     * @param resource resource
     * @return list of resource-managers
     */
    private Collection<ResourceManager> getResourceManagers(
            BindableResource resource) {

        Collection<ResourceManager> rms = new ArrayList<ResourceManager>();
        for (ResourceManager rm : resourceManagers) {
            if (rm.handlesResource(resource)) {
                rms.add(rm);
            }
        }
        return rms;
    }

    private boolean runtimeSupportsJMS() {
        boolean supports = false;
        try {
            Class.forName("javax.jms.QueueConnectionFactory");
            supports = true;
        } catch (Throwable e) {
            LOGGER.log(Level.FINEST, "Exception while loading JMS API {0}", e);
        }
        return supports;
    }

    private void registerJMSResources(
            Collection<ResourceManager> resourceManagers, Habitat habitat) {

        resourceManagers.add(new JMSResourceManager(habitat));
        resourceManagers.add(new JMSDestinationResourceManager(habitat));
    }
}
