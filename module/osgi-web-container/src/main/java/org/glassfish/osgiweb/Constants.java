/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Constants {

    public static final String WEB_BUNDLE_SCHEME = "webbundle";
    public static final String WEB_CONTEXT_PATH = "Web-ContextPath";
    public static final String WEB_JSP_EXTRACT_LOCATION = "Web-JSPExtractLocation";
    public static final String BUNDLE_CONTEXT_ATTR = "osgi-bundlecontext";
    public static final String OSGI_WEB_SYMBOLIC_NAME = "osgi.web.symbolicname";
    public static final String OSGI_WEB_VERSION = "osgi.web.version";
    public static final String OSGI_WEB_CONTEXTPATH = "osgi.web.contextpath";

    // constants related to integration with event admin service
    public static final String EVENT_TOPIC_DEPLOYING = "org/osgi/service/web/DEPLOYING";
    public static final String EVENT_TOPIC_DEPLOYED = "org/osgi/service/web/DEPLOYED";
    public static final String EVENT_TOPIC_UNDEPLOYING = "org/osgi/service/web/UNDEPLOYING";
    public static final String EVENT_TOPIC_UNDEPLOYED = "org/osgi/service/web/UNDEPLOYED";
    public static final String EVENT_TOPIC_FAILED = "org/osgi/service/web/FAILED";

    // various properties published as part of the event data
    public static final String EVENT_PROPERTY_BUNDLE_SYMBOLICNAME = "bundle.symbolicName";
    public static final String EVENT_PROPERTY_BUNDLE_ID = "bundle.id";
    public static final String EVENT_PROPERTY_BUNDLE = "bundle";
    public static final String EVENT_PROPERTY_BUNDLE_VERSION = "bundle.version";
    public static final String EVENT_PROPERTY_CONTEXT_PATH = "context.path";
    public static final String EVENT_PROPERTY_TIMESTAMP = "timestamp";
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE = "extender.bundle";
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_ID = "extender.bundle.id";
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_NAME = "extender.bundle.symbolicName";
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_VERSION = "extender.bundle.version";
    public static final String EVENT_PROPERTY_EXCEPTION = "exception";
    public static final String EVENT_PROPERTY_COLLISION = "collision";
    public static final String EVENT_PROPERTY_COLLISION_BUNDLES = "collision.bundles";

    // Below are GlassFish specific constants
    public static final String FACES_CONFIG_ATTR = "glassfish.osgi.web.facesconfigs";
    public static final String FACELET_CONFIG_ATTR = "glassfish.osgi.web.faceletconfigs";
    public static final String FACES_ANNOTATED_CLASSES = "glassfish.osgi.web.facesannotatedclasses";
    public static final String VIRTUAL_SERVERS = "Virtual-Servers";
}
