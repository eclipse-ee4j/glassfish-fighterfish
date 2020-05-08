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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;

/**
 * Base class for resource-managers that export resources in GlassFish to OSGi service-registry.
 */
public class BaseResourceManager {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(BaseResourceManager.class.getPackage().getName());

    /**
     * Service registrations.
     */
    private final List<ServiceRegistration> services = new ArrayList<>();

    /**
     * Resource helper.
     */
    private final ResourceHelper resourceHelper;

    /**
     * Habitat / locator.
     */
    private final Habitat habitat;

    /**
     * Create a new instance.
     *
     * @param hab habitat
     */
    public BaseResourceManager(final Habitat hab) {
        this.habitat = hab;
        this.resourceHelper = new ResourceHelper(hab);
    }

    /**
     * Unregister the given service.
     *
     * @param sReg registration of the service to unregister
     * @param context bundle context
     */
    @SuppressWarnings("unchecked")
    protected void unRegisterResource(final ServiceRegistration sReg, final BundleContext context) {

        debug("unregistering resource [" + sReg.getReference().getProperty(Constants.JNDI_NAME) + "]");
        Invalidate proxy = (Invalidate) sReg.getReference().getBundle().getBundleContext().getService(sReg.getReference());
        sReg.unregister();
        proxy.invalidate();
    }

    /**
     * Unregister the service for the given resource.
     *
     * @param resource resource to unregister
     * @param resRef resource reference
     * @param bundleContext bundle context
     */
    public void unRegisterResource(final BindableResource resource, final ResourceRef resRef, final BundleContext bundleContext) {

        String jndiName = resource.getJndiName();
        ServiceRegistration toRemove = null;
        for (ServiceRegistration serviceRegistration : services) {
            if (serviceRegistration.getReference().getProperty(Constants.JNDI_NAME).equals(jndiName)) {
                unRegisterResource(serviceRegistration, bundleContext);
                toRemove = serviceRegistration;
                break;
            }
        }
        if (toRemove != null) {
            services.remove(toRemove);
        }
    }

    /**
     * Unregister all the resources.
     *
     * @param context bundle context
     */
    public void unRegisterResources(final BundleContext context) {
        Iterator it = services.iterator();
        while (it.hasNext()) {
            ServiceRegistration serviceRegistration = (ServiceRegistration) it.next();
            unRegisterResource(serviceRegistration, context);
            it.remove();
        }
    }

    /**
     * Get the habitat.
     *
     * @return Habitat
     */
    protected Habitat getHabitat() {
        return habitat;
    }

    /**
     * Get the resources.
     *
     * @return Resources
     */
    protected Resources getResources() {
        return habitat.getComponent(Domain.class).getResources();
    }

    /**
     * Get the resources helper.
     *
     * @return ResourceHelper
     */
    protected ResourceHelper getResourceHelper() {
        return resourceHelper;
    }

    /**
     * Get the class-loader.
     *
     * @return ClassLoader
     */
    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    /**
     * Register the given resource as an OSGi service.
     *
     * @param context bundle context
     * @param bindableResource JNDI name of the resource
     * @param name name of the resource
     * @param properties service properties
     * @param obj resource instance to register as service
     */
    @SuppressWarnings("unchecked")
    protected void registerResourceAsService(final BundleContext context, final BindableResource bindableResource, final String name,
            final Dictionary properties, final Object obj) {

        ServiceRegistration service = context.registerService(name, obj, properties);
        debug("registering resource [" + bindableResource.getJndiName() + "]");
        services.add(service);
    }

    /**
     * get proxy object for the resource types (interfaces) so as to delegate to actual objects.
     *
     * @param jndiName JNDI name of resource
     * @param ifaces list of interfaces for which the proxy is needed
     * @param loader class-loader to define the proxy class
     * @return proxy object
     */
    protected Object getProxy(final String jndiName, final Class[] ifaces, final ClassLoader loader) {

        ResourceProxy proxy = new ResourceProxy(jndiName);
        return Proxy.newProxyInstance(loader, ifaces, proxy);
    }

    /**
     * Log a message at {@code FINEST} level.
     *
     * @param msg message to log
     */
    protected void debug(final String msg) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-ee-resources] : {0}", msg);
        }
    }
}
