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

import java.util.List;

import org.glassfish.internal.api.ServerContext;

import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;

/**
 * Resource helper.
 */
public final class ResourceHelper {

    /**
     * Component locator.
     */
    private final Habitat habitat;

    /**
     * Create a new instance.
     *
     * @param hab component locator
     */
    ResourceHelper(final Habitat hab) {
        this.habitat = hab;
    }

    /**
     * Retrieves <i>resource-ref</i> from configuration.
     *
     * @param resourceName resource-name
     * @return resource-ref
     */
    public ResourceRef getResourceRef(final String resourceName) {
        ServerContext context = getHabitat().getComponent(ServerContext.class);
        String instanceName = context.getInstanceName();

        Servers servers = getHabitat().getComponent(Servers.class);
        for (Server server : servers.getServer()) {
            if (server.getName().equals(instanceName)) {
                List<ResourceRef> resourceRefs = server.getResourceRef();
                for (ResourceRef resourceRef : resourceRefs) {
                    if (resourceRef.getRef().equals(resourceName)) {
                        return resourceRef;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the habitat.
     *
     * @return Habitat
     */
    private Habitat getHabitat() {
        return habitat;
    }
}
