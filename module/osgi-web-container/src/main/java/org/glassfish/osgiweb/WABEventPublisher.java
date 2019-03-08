/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgiweb;

import org.glassfish.osgijavaeebase.AbstractOSGiDeployer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.glassfish.osgiweb.Constants.*;

/**
 * This class is responsible for publishing events using EventAdmin service for
 * a WAB deployment lifecycle. During deployment, various events are raised as
 * described below (taken verbatim from spec): The Web Extender must track all
 * WABs in the OSGi service platform in which the Web Extender is installed. The
 * Web Extender must post Event Admin events, which is asynchronous, at crucial
 * points in its processing. The topic of the event must be one of the following
 * values: • org/osgi/service/web/DEPLOYING – The Web Extender has accepted a
 * WAB and started the process of deploying a Web Application. •
 * org/osgi/service/web/DEPLOYED – The Web Extender has finished deploying a Web
 * Application, and the Web Application is now available for web requests on its
 * Context Path. • org/osgi/service/web/UNDEPLOYING – The web extender started
 * undeploying the Web Application in response to its corresponding WAB being
 * stopped or the Web Extender is stopped. • org/osgi/service/web/UNDEPLOYED –
 * The Web Extender has undeployed the Web Application. The application is no
 * longer available for web requests. • org/osgi/service/web/FAILED – The Web
 * Extender has failed to deploy the Web Application, this event can be fired
 * after the DEPLOYING event has fired and indicates that no DEPLOYED event will
 * be fired.
 * <p/>
 * For each event topic above, the following properties must be published: •
 * bundle.symbolicName – (String) The bundle symbolic name of the WAB. •
 * bundle.id – (Long) The bundle id of the WAB. • bundle – (Bundle) The Bundle
 * object of the WAB. • bundle.version – (Version) The version of the WAB. •
 * context.path – (String) The Context Path of the Web Application. • timestamp
 * – (Long) The time when the event occurred • extender.bundle – (Bundle) The
 * Bundle object of the Web Extender Bundle • extender.bundle.id – (Long) The id
 * of the Web Extender Bundle. • extender.bundle.symbolicName – (String) The
 * symbolic name of the Web Extender Bundle. • extender.bundle.version –
 * (Version) The version of the Web Extender Bundle.
 * <p/>
 * In addition, the org/osgi/service/web/FAILED event must also have the
 * following property: • exception – (Throwable) If an exception caused the
 * failure, an exception detailing the error that occurred during the deployment
 * of the WAB. • collision – (String) If a name collision occurred, the
 * Web-ContextPath that had a collision • collision.bundles – (Long) If a name
 * collision occurred, a list of bundle ids that all have the same value for the
 * Web-ContextPath manifest header.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class WABEventPublisher {

    void raiseEvent(AbstractOSGiDeployer.State state, Bundle appBundle,
            Bundle extenderBundle, Throwable e) {

        Event event = prepareEvent(state, appBundle, extenderBundle, e);
        if (event != null) {
            postEvent(event, extenderBundle.getBundleContext());
        }
    }

    private Event prepareEvent(AbstractOSGiDeployer.State state,
            Bundle appBundle, Bundle extenderBundle, Throwable e) {

        String topic;
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(EVENT_PROPERTY_BUNDLE_SYMBOLICNAME,
                appBundle.getSymbolicName());
        props.put(EVENT_PROPERTY_BUNDLE_ID, appBundle.getBundleId());
        props.put(EVENT_PROPERTY_BUNDLE_VERSION, appBundle.getVersion());
        props.put(EVENT_PROPERTY_CONTEXT_PATH, Util.getContextPath(appBundle));
        props.put(EVENT_PROPERTY_TIMESTAMP, System.currentTimeMillis());
        props.put(EVENT_PROPERTY_BUNDLE, appBundle);

        props.put(EVENT_PROPERTY_EXTENDER_BUNDLE, extenderBundle);
        props.put(EVENT_PROPERTY_EXTENDER_BUNDLE_ID,
                extenderBundle.getBundleId());
        props.put(EVENT_PROPERTY_EXTENDER_BUNDLE_NAME,
                extenderBundle.getSymbolicName());
        props.put(EVENT_PROPERTY_EXTENDER_BUNDLE_VERSION,
                extenderBundle.getVersion());

        switch (state) {
            case DEPLOYING:
                topic = EVENT_TOPIC_DEPLOYING;
                break;
            case DEPLOYED:
                topic = EVENT_TOPIC_DEPLOYED;
                break;
            case FAILED:
                topic = EVENT_TOPIC_FAILED;
                props.put(EVENT_PROPERTY_EXCEPTION, e);
                if (e instanceof ContextPathCollisionException) {
                    final ContextPathCollisionException ce =
                            ContextPathCollisionException.class.cast(e);
                    Long ids[] = ce.getCollidingWabIds();
                    props.put(EVENT_PROPERTY_COLLISION_BUNDLES,
                            // The spec requires it to be a collection
                            Arrays.asList(ids));
                    props.put(EVENT_PROPERTY_COLLISION, ce.getContextPath());
                }
                break;
            case UNDEPLOYING:
                topic = EVENT_TOPIC_UNDEPLOYING;
                break;
            case UNDEPLOYED:
                topic = EVENT_TOPIC_UNDEPLOYED;
                break;
            default:
                return null;
        }
        Event event = new Event(topic, props);
        return event;
    }

    @SuppressWarnings("unchecked")
    private void postEvent(Event event, BundleContext bc) {
        ServiceReference ref = ServiceReference.class.cast(
                bc.getServiceReference(EventAdmin.class.getName()));
        if (ref != null) {
            EventAdmin ea = (EventAdmin) bc.getService(ref);
            if (ea != null) {
                ea.postEvent(event); // asynchronous
            }
            bc.ungetService(ref);
        }
    }
}
