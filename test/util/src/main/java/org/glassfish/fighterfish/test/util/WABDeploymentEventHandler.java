/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.util.Hashtable;

/**
 * Listens to OSGi Event Admin Service generated events generated for a WAB and calls back a registered handler
 */
public class WABDeploymentEventHandler implements EventHandler {
    public interface Callback {
        void deploying();

        void deployed(String contextPath);

        void undeploying();

        void undeployed();

        void failed(Throwable throwable, String collision, Long[] collisionBundleIds);
    }

    private Callback callback;
    private ServiceRegistration registration;


    /**
     * @param context  BundleContext used to register the service
     * @param b        Bundle whose deployment related events we are interested in
     * @param callback object that will be called back when appropriate events are received
     */
    public WABDeploymentEventHandler(BundleContext context, Bundle b, Callback callback) {
        this.callback = callback;
        String[] topics = new String[]{"org/osgi/service/web/*"};
        Hashtable ht = new Hashtable();
        ht.put(EventConstants.EVENT_TOPIC, topics);
        final String filterString = "(" + EventConstants.BUNDLE_ID + "=" + b.getBundleId() + ")";
        ht.put(EventConstants.EVENT_FILTER, filterString);
        registration = context.registerService(EventHandler.class.getName(), this, ht);
    }

    @Override
    public void handleEvent(Event event) {
        final String topic = event.getTopic();
        System.out.println(topic);
        System.out.println("event = " + event);
        if ("org/osgi/service/web/DEPLOYING".equals(topic)) {
            callback.deploying();
        } else if ("org/osgi/service/web/DEPLOYED".equals(topic)) {
            String contextPath = (String) event.getProperty("context.path");
            callback.deployed(contextPath);
        } else if ("org/osgi/service/web/FAILED".equals(topic)) {
            Throwable throwable = (Throwable) event.getProperty("exception");
            String collision = (String) event.getProperty("collision");
            Long[] collisionBundleIds = (Long[]) event.getProperty("collision.bundles");
            callback.failed(throwable, collision, collisionBundleIds);
        } else if ("org/osgi/service/web/UNDEPLOYING".equals(topic)) {
            callback.undeploying();
        } else if ("org/osgi/service/web/UNDEPLOYED".equals(topic)) {
            callback.undeployed();
        }
    }

    /**
     * Stop listening for events.
     */
    public void stop() {
        registration.unregister();
    }
}
