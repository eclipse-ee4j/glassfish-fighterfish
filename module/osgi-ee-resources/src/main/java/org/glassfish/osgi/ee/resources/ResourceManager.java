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

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import org.osgi.framework.BundleContext;

/**
 * Resource-Manager to export resources defined in GlassFish to OSGi's service-registry.
 */
public interface ResourceManager {

    /**
     * Register all appropriate resources.
     *
     * @param context bundle-context
     */
    void registerResources(BundleContext context);

    /**
     * Register the resource that is created or enabled.
     *
     * @param resource resource that is created or enabled
     * @param resRef resource-ref of the resource
     * @param bundleContext bundle-context
     */
    void registerResource(BindableResource resource, ResourceRef resRef, BundleContext bundleContext);

    /**
     * Un-register the resource that is deleted or disabled.
     *
     * @param resource resource that is deleted or disabled
     * @param resRef resource-ref of the resource
     * @param bundleContext bundle-context
     */
    void unRegisterResource(BindableResource resource, ResourceRef resRef, BundleContext bundleContext);

    /**
     * un-register all appropriate resources.
     *
     * @param context bundle-context
     */
    void unRegisterResources(BundleContext context);

    /**
     * Test if the manager can handle the given resource.
     *
     * @param resource resource to be handled
     * @return {@code true} if this manager handles the given resource, {@code false} otherwise
     */
    boolean handlesResource(BindableResource resource);
}
