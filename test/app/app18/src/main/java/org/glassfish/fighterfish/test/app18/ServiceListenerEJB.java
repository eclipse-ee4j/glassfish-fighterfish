/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Session Bean implementation class ServiceListenerEJB
 */
@Singleton
@Startup
@DependsOn("EjbLifecycleObserverEJB")
public class ServiceListenerEJB {

    @Inject
    BundleContext bundleCtx;

    @Inject
    MyServiceListener listener;

    @PostConstruct
    public void installListener() {
        String filter = "(" + Constants.OBJECTCLASS + "="
                + Foo.class.getName() + ")";
        try {
            bundleCtx.addServiceListener(listener, filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void uninstallListener() {
        bundleCtx.removeServiceListener(listener);
        System.out.println("ServiceListenerEJB.uninstallListener() "
                + "Removed service listener " + listener);
    }

    public static class MyServiceListener implements ServiceListener {

        @Inject
        @OSGiService(dynamic = true)
        private EjbLifecycleObserver observer;

        @Override
        public synchronized void serviceChanged(ServiceEvent event) {
            System.out.println(getClass().getName() + ".serviceChanged() " + event);
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    observer.registered(Foo.class.getName());
                    break;
            }
        }
    }
}
