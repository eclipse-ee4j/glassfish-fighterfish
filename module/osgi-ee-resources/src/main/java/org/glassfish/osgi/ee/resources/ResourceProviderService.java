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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.osgi.framework.BundleContext;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import static org.jvnet.hk2.config.Changed.TYPE.ADD;
import static org.jvnet.hk2.config.Changed.TYPE.CHANGE;
import static org.jvnet.hk2.config.Changed.TYPE.REMOVE;

/**
 * A service to export resources in GlassFish to OSGi's service-registry.<br>
 * OSGi applications can use <i>ServiceReference</i> to get access to these resources. OSGi applications can do lookup
 * of appropriate type of<br>
 * <i>ServiceReference</i> with the filter <i>"jndi-name"</i> <br>
 * <br>
 * For JDBC Resources, additional filter <i>"osgi.jdbc.driver.class"</i> that indicates the<br>
 * driver-class-name/datasource-class-name will work.
 * <p/>
 * JDBC Resources, JMS Connection Factories, JMS Destinations are exported with following <i>ServiceReference</i>
 * names<br>
 * For JDBC Resources : <i>javax.sql.DataSource</i> <br>
 * For JMS Resources : <i>javax.jms.ConnectionFactory / javax.jms.QueueConnectionFactory /
 * javax.jms.TopicConnectionFactory</i> <br>
 * For JMS Destinations : <i>javax.jms.Queue / javax.jms.Topic</i> <br>
 *
 */
public final class ResourceProviderService implements ConfigListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ResourceProviderService.class.getPackage().getName());

    /**
     * Component locator.
     */
    private final Habitat habitat;

    /**
     * Resources.
     */
    private final Resources resources;

    /**
     * GlassFish servers config bean.
     */
    private final Servers servers;

    // config-bean proxy objects so as to listen to changes to these
    // configuration.

    /**
     * Server config bean proxy object.
     */
    private ObservableBean serverConfigBean;

    /**
     * Resources config bean proxy object.
     */
    private ObservableBean resourcesConfigBean;

    /**
     * Bundle context.
     */
    private final BundleContext bundleContext;

    /**
     * Resource helper.
     */
    private final ResourceHelper resourceHelper;

    /**
     * Resource manager.
     */
    private final Collection<ResourceManager> resourceManagers;

    /**
     * Create a new instance.
     * 
     * @param hab component locator
     * @param bndCtx bundle context
     */
    public ResourceProviderService(final Habitat hab, final BundleContext bndCtx) {

        this.habitat = hab;
        this.bundleContext = bndCtx;
        servers = hab.getComponent(Servers.class);
        resources = hab.getComponent(Domain.class).getResources();
        resourceHelper = new ResourceHelper(hab);
        resourceManagers = new ArrayList<ResourceManager>();
        initializeResourceManagers();
        postConstruct();
    }

    /**
     * Initialize the resource managers.
     */
    private void initializeResourceManagers() {
        resourceManagers.add(new JDBCResourceManager(habitat));
        if (runtimeSupportsJMS()) {
            registerJMSResources(resourceManagers, habitat);
        }
    }

    /**
     * Register the resources.
     */
    public void registerResources() {
        for (ResourceManager rm : resourceManagers) {
            rm.registerResources(bundleContext);
        }
    }

    /**
     * Unregister the resources.
     */
    public void unRegisterResources() {
        for (ResourceManager rm : resourceManagers) {
            rm.unRegisterResources(bundleContext);
        }
        preDestroy();
    }

    /**
     * Unregister config bean proxy change listeners.
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
     * Register config bean proxy change listeners.
     */
    public void postConstruct() {
        List<Server> serversList = servers.getServer();
        ServerContext context = habitat.getComponent(ServerContext.class);
        String instanceName = context.getInstanceName();
        for (Server server : serversList) {
            if (server.getName().equals(instanceName)) {
                serverConfigBean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy) server);
                serverConfigBean.addListener(this);
            }
        }

        resourcesConfigBean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy) resources);
        resourcesConfigBean.addListener(this);

    }

    /**
     * Notification that {@code @Configured} objects that were injected have changed.
     * 
     * @return UnprocessedChangeEvents
     * @param events list of changes
     */
    @Override
    public UnprocessedChangeEvents changed(final PropertyChangeEvent[] events) {

        return ConfigSupport.sortAndDispatch(events, new PropertyChangeHandler(events, this), LOGGER);
    }

    /**
     * Config change listener.
     */
    private static final class PropertyChangeHandler implements Changed {

        /**
         * Config change events.
         */
        private final PropertyChangeEvent[] events;

        /**
         * Resource provider service.
         */
        private final ResourceProviderService rps;

        /**
         * Create a new instance.
         * 
         * @param evts config change events
         * @param resProviderSvc resource provider service
         */
        PropertyChangeHandler(final PropertyChangeEvent[] evts, final ResourceProviderService resProviderSvc) {

            this.events = evts;
            this.rps = resProviderSvc;
        }

        @Override
        @SuppressWarnings("checkstyle:EmptyBlock")
        public <T extends ConfigBeanProxy> NotProcessed changed(final Changed.TYPE type, final Class<T> changedType, final T changedInstance) {

            NotProcessed np;
            try {
                switch (type) {
                case ADD:
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "A new {0} was added : {1}", new Object[] { changedType.getName(), changedInstance });
                    }
                    np = handleAddEvent(changedInstance);
                    break;

                case CHANGE:
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "A {0} was changed : {1}", new Object[] { changedType.getName(), changedInstance });
                    }
                    np = handleChangeEvent(changedInstance);
                    break;

                case REMOVE:
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "A {0} was removed : {1}", new Object[] { changedType.getName(), changedInstance });
                    }
                    np = handleRemoveEvent(changedInstance);
                    break;

                default:
                    np = new NotProcessed("Unrecognized type of change: " + type);
                    break;
                }
                return np;
            } finally {
            }
        }

        /**
         * Handle a remove event.
         * 
         * @param <T> type of the removed instance
         * @param removedInstance removed instance
         * @return NotProcessed
         */
        private <T extends ConfigBeanProxy> NotProcessed handleRemoveEvent(final T removedInstance) {

            if (removedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) removedInstance;
                String resourceName = resourceRef.getRef();
                BindableResource resource = (BindableResource) rps.resources.getResourceByName(BindableResource.class, resourceName);
                unRegisterResource(resource);
            } else if (removedInstance instanceof BindableResource) {
                // since delete resource-ref event will not work
                // (resource related configuration
                // information won't be available during resource-ref
                // deletion event), handling
                // un-register of service here also.
                unRegisterResource((BindableResource) removedInstance);
            }
            return null;
        }

        /**
         * Handle a change event.
         * 
         * @param <T> type of the changed object
         * @param changedInstance changed object
         * @return NotProcessed
         */
        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(final T changedInstance) {

            // TODO Handle other attribute changes (jndi-name)
            if (changedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) changedInstance;
                String refName = resourceRef.getRef();

                for (PropertyChangeEvent event : events) {
                    String propertyName = event.getPropertyName();
                    if ("enabled".equalsIgnoreCase(propertyName)) {
                        boolean newValue = Boolean.parseBoolean(event.getNewValue().toString());
                        boolean oldValue = Boolean.parseBoolean(event.getOldValue().toString());
                        // make sure that there is state change
                        if (!(newValue && oldValue)) {
                            BindableResource bindableResource = (BindableResource) rps.resources.getResourceByName(BindableResource.class, refName);
                            if (newValue) {
                                registerResource(bindableResource, resourceRef);
                            } else {
                                unRegisterResource(bindableResource);
                            }
                        }
                    }
                }
            } else if (changedInstance instanceof BindableResource) {
                BindableResource bindableResource = (BindableResource) changedInstance;
                for (PropertyChangeEvent event : events) {
                    String propertyName = event.getPropertyName();
                    if ("enabled".equalsIgnoreCase(propertyName)) {
                        boolean newValue = Boolean.parseBoolean(event.getNewValue().toString());
                        boolean oldValue = Boolean.parseBoolean(event.getOldValue().toString());
                        // make sure that there is state change
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
            } else if (changedInstance instanceof JdbcConnectionPool || changedInstance instanceof ConnectorConnectionPool) {
                // this block handles any configuration change under connection
                // pool,
                // it's re-registering all resources which uses that connection
                // pool.
                String poolName = ((ResourcePool) changedInstance).getName();
                Resources resources = rps.habitat.getComponent(Domain.class).getResources();
                Collection<BindableResource> bindableResources = ConnectorsUtil.getResourcesOfPool(resources, poolName);
                reRegisterResource(bindableResources);
            }
            return null;
        }

        /**
         * This method un-register and register resource again.
         *
         * @param bindableResources resources to register
         */
        private void reRegisterResource(final Collection<BindableResource> bindableResources) {

            for (BindableResource resource : bindableResources) {
                if (Boolean.valueOf(resource.getEnabled())) {
                    ResourceRef resRef = rps.resourceHelper.getResourceRef(resource.getJndiName());
                    if (resRef != null && Boolean.valueOf(resRef.getEnabled())) {
                        unRegisterResource(resource);
                        registerResource(resource);
                    }
                }
            }
        }

        /**
         * Unregister the given resource.
         * 
         * @param bindableResource resource to unregister
         */
        private void unRegisterResource(final BindableResource bindableResource) {

            Collection<ResourceManager> resourceManagers = rps.getResourceManagers(bindableResource);
            for (ResourceManager rm : resourceManagers) {
                ResourceRef ref = rps.resourceHelper.getResourceRef(bindableResource.getJndiName());
                rm.unRegisterResource(bindableResource, ref, rps.bundleContext);
            }
        }

        /**
         * Register the given resource.
         * 
         * @param bindableResource resource config bean
         * @param ref resource reference
         */
        private void registerResource(final BindableResource bindableResource, final ResourceRef ref) {
            Collection<ResourceManager> resourceManagers = rps.getResourceManagers(bindableResource);
            for (ResourceManager rm : resourceManagers) {
                rm.registerResource(bindableResource, ref, rps.bundleContext);
            }
        }

        /**
         * Register the given resource.
         * 
         * @param bindableResource resource config bean
         */
        private void registerResource(final BindableResource bindableResource) {

            ResourceRef ref = rps.resourceHelper.getResourceRef(bindableResource.getJndiName());
            registerResource(bindableResource, ref);
        }

        /**
         * Handle a add event.
         * 
         * @param <T> type of the added object
         * @param addedInstance added object
         * @return NotProcessed
         */
        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent(final T addedInstance) {

            if (addedInstance instanceof ResourceRef) {
                ResourceRef resourceRef = (ResourceRef) addedInstance;
                String resourceName = resourceRef.getRef();
                BindableResource resource = (BindableResource) rps.resources.getResourceByName(BindableResource.class, resourceName);
                if (resource != null) {
                    registerResource(resource, resourceRef);
                }
            }
            return null;
        }
    }

    /**
     * Get the list of resource-managers that can handle the resource.
     * 
     * @param resource resource
     * @return list of resource-managers
     */
    private Collection<ResourceManager> getResourceManagers(final BindableResource resource) {

        Collection<ResourceManager> rms = new ArrayList<ResourceManager>();
        for (ResourceManager rm : resourceManagers) {
            if (rm.handlesResource(resource)) {
                rms.add(rm);
            }
        }
        return rms;
    }

    /**
     * Test if the runtime support JMS.
     * 
     * @return {@code true} if the runtime supports JMS, {@code false} otherwise
     */
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

    /**
     * Register the given JMS resources.
     * 
     * @param resManagers manager of the resources to register
     * @param hab component locator
     */
    private void registerJMSResources(final Collection<ResourceManager> resManagers, final Habitat hab) {

        resManagers.add(new JMSResourceManager(hab));
        resManagers.add(new JMSDestinationResourceManager(hab));
    }
}
