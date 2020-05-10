/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app18;

import org.glassfish.osgicdi.OSGiService;
import org.glassfish.osgicdi.ServiceUnavailableException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.Dictionary;
import java.util.Properties;

/**
 * Session Bean implementation class EjbLifecycleObserverEJB.
 */
@Singleton
@Startup
@SuppressWarnings("checkstyle:DesignForExtension")
public class EjbLifecycleObserverEJB implements EjbLifecycleObserver {

    /**
     * OSGi event admin.
     */
    @Inject
    @OSGiService(dynamic = true)
    private EventAdmin eventAdmin;

    @Override
    @SuppressWarnings("unchecked")
    public void registered(final String serviceName) {
        String message = "EjbLifecycleObserverEJB.registered() " + serviceName;
        System.out.println(message);
        try {
            Dictionary eventProps = new Properties();
            eventProps.put("eventType", "REGISTERED");
            eventProps.put("serviceName", serviceName);
            Event event = new Event("org/glassfish/fighterfist/test/app18",
                    eventProps);
            eventAdmin.sendEvent(event);
        } catch (ServiceUnavailableException e) {
            System.out.println("EjbLifecycleObserverEJB.registered() " + e);
        }
    }
}
