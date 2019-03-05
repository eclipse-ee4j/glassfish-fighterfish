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

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for resource-managers that export resources in GlassFish to OSGi
 * service-registry
 *
 * @author Jagadish Ramu
 */
public class BaseResourceManager {

    protected static final Logger LOGGER = Logger.getLogger(
            BaseResourceManager.class.getPackage().getName());

    protected final List<ServiceRegistration> services =
            new ArrayList<ServiceRegistration>();
    protected final ResourceHelper resourceHelper;
    private final Habitat habitat;

    public BaseResourceManager(Habitat habitat) {
        this.habitat = habitat;
        this.resourceHelper = new ResourceHelper(habitat);
    }

    @SuppressWarnings("unchecked")
    protected void unRegisterResource(ServiceRegistration serviceRegistration,
            BundleContext context) {

        debug("unregistering resource ["
                + serviceRegistration.getReference()
                        .getProperty(Constants.JNDI_NAME) + "]");
        Invalidate proxy = (Invalidate) serviceRegistration
                .getReference()
                .getBundle()
                .getBundleContext()
                .getService(serviceRegistration.getReference());
        serviceRegistration.unregister();
        proxy.invalidate();
    }

    public void unRegisterResource(BindableResource resource,
            ResourceRef resRef, BundleContext bundleContext) {

        String jndiName = resource.getJndiName();
        ServiceRegistration toRemove = null;
        for (ServiceRegistration serviceRegistration : services) {
            if (serviceRegistration.getReference()
                    .getProperty(Constants.JNDI_NAME).equals(jndiName)) {
                unRegisterResource(serviceRegistration, bundleContext);
                toRemove = serviceRegistration;
                break;
            }
        }
        if (toRemove != null) {
            services.remove(toRemove);
        }
    }

    public void unRegisterResources(BundleContext context) {
        Iterator it = services.iterator();
        while (it.hasNext()) {
            ServiceRegistration serviceRegistration =
                    (ServiceRegistration) it.next();
            unRegisterResource(serviceRegistration, context);
            it.remove();
        }
    }

    protected Habitat getHabitat() {
        return habitat;
    }

    protected Resources getResources() {
        return habitat.getComponent(Domain.class).getResources();
    }

    protected ResourceHelper getResourceHelper() {
        return resourceHelper;
    }

    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    @SuppressWarnings("unchecked")
    protected void registerResourceAsService(BundleContext bundleContext,
            BindableResource bindableResource, String name,
            Dictionary properties, Object o) {

        ServiceRegistration service = bundleContext
                .registerService(name, o, properties);
        debug("registering resource [" + bindableResource.getJndiName() + "]");
        services.add(service);
    }

    /**
     * get proxy object for the resource types (interfaces) so as to delegate to
     * actual objects<br>
     *
     * @param jndiName JNDI name of resource
     * @param ifaces list of interfaces for which the proxy is needed
     * @param loader class-loader to define the proxy class
     * @return proxy object
     */
    protected Object getProxy(String jndiName, Class[] ifaces,
            ClassLoader loader) {

        ResourceProxy proxy = new ResourceProxy(jndiName);
        return Proxy.newProxyInstance(loader, ifaces, proxy);
    }

    protected void debug(String s) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-ee-resources] : {0}", s);
        }
    }
}
