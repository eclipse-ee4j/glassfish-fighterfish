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

import static org.glassfish.osgi.ee.resources.Constants.JNDI_NAME;
import static org.glassfish.osgi.ee.resources.Constants.QUEUE;
import static org.glassfish.osgi.ee.resources.Constants.TOPIC;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.glassfish.connectors.config.AdminObjectResource;
import org.osgi.framework.BundleContext;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;

import jakarta.jms.Queue;
import jakarta.jms.Topic;

/**
 * Resource-Manager to export jms-destinations (JMS-RA admin-object-resources) in GlassFish to OSGi's service-registry.
 */
public final class JMSDestinationResourceManager extends BaseResourceManager implements ResourceManager {

    /**
     * Create a new instance.
     *
     * @param habitat component locator
     */
    public JMSDestinationResourceManager(Habitat habitat) {
        super(habitat);
    }

    @Override
    public void registerResources(final BundleContext context) {
        registerJmsResources(context);
    }

    /**
     * Registers the admin-object-resource in service-registry.
     *
     * @param context bundle-context
     */
    public void registerJmsResources(final BundleContext context) {
        Resources resources = getHabitat().getComponent(Domain.class).getResources();
        Collection<AdminObjectResource> administeredObjectResources = resources.getResources(AdminObjectResource.class);
        for (AdminObjectResource resource : administeredObjectResources) {
            if (isJmsResource(resource)) {
                ResourceRef resRef = getResourceHelper().getResourceRef(resource.getJndiName());
                registerResource(resource, resRef, context);
            }
        }
    }

    @Override
    public void registerResource(final BindableResource resource, final ResourceRef resRef, final BundleContext bundleContext) {
        AdminObjectResource adminObjectResource = (AdminObjectResource) resource;

        if (adminObjectResource.getEnabled().equalsIgnoreCase("true")) {
            if (resRef != null && resRef.getEnabled().equalsIgnoreCase("true")) {
                String defnName = adminObjectResource.getResType();
                Class<?> claz = null;
                Class<?>[] intf = null;

                if (defnName.equals(QUEUE)) {
                    claz = Queue.class;
                    intf = new Class[] { Queue.class, Invalidate.class };
                } else if (defnName.equals(TOPIC)) {
                    claz = Topic.class;
                    intf = new Class[] { Topic.class, Invalidate.class };
                } else {
                    throw new RuntimeException("Invalid Destination [ " + defnName + " ]" + " for jms-resource [ " + resource.getJndiName() + " ]");
                }

                Dictionary<String, String> properties = new Hashtable<>();
                properties.put(JNDI_NAME, adminObjectResource.getJndiName());
                
                registerResourceAsService(
                    bundleContext, 
                    adminObjectResource, 
                    claz.getName(), 
                    properties, 
                    getProxy(adminObjectResource.getJndiName(), intf, getClassLoader()));
            }
        }

    }

    @Override
    public boolean handlesResource(final BindableResource resource) {
        boolean result = false;
        if (resource instanceof AdminObjectResource) {
            if (isJmsResource((AdminObjectResource) resource)) {
                result = true;
            }
        }
        
        return result;
    }

    /**
     * Test if the given resource is a JMS-RA resource.
     *
     * @param resource admin-object-resource
     * @return {@code true} if a jms resource, {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean isJmsResource(final AdminObjectResource resource) {
        boolean result = false;
        String raName = resource.getResAdapter();
        if (raName.equals(Constants.DEFAULT_JMS_ADAPTER)) {
            result = true;
        }
        
        return result;
    }
}
