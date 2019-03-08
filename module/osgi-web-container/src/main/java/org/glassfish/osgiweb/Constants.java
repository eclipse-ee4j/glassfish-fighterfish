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

/**
 * Constant for this module.
 */
public final class Constants {

    /**
     * Cannot be instanciated.
     */
    private Constants() {
    }

    /**
     * Constant for {@code webbundle} URI scheme.
     */
    public static final String WEB_BUNDLE_SCHEME = "webbundle";

    /**
     * Constant for {@code Web-ContextPath} manifest entry.
     */
    public static final String WEB_CONTEXT_PATH = "Web-ContextPath";

    /**
     * Constant for {@code Web-JSPExtractLocation} manifest entry.
     */
    public static final String WEB_JSP_EXTRACT_LOCATION =
            "Web-JSPExtractLocation";

    /**
     * Constant for {@code osgi-bundlecontext}.
     */
    public static final String BUNDLE_CONTEXT_ATTR = "osgi-bundlecontext";

    /**
     * Constant for the property name of the osgi-web bundle symbolic name.
     */
    public static final String OSGI_WEB_SYMBOLIC_NAME =
            "osgi.web.symbolicname";

    /**
     * Constant for the property name of the osgi-web bundle version.
     */
    public static final String OSGI_WEB_VERSION = "osgi.web.version";

    /**
     * Constant for the property name of the osgi-web bundle context path.
     */
    public static final String OSGI_WEB_CONTEXTPATH = "osgi.web.contextpath";

    // constants related to integration with event admin service

    /**
     * Constant for the {@code DEPLOYING} event topic.
     */
    public static final String EVENT_TOPIC_DEPLOYING =
            "org/osgi/service/web/DEPLOYING";

    /**
     * Constant for the {@code DEPLOYED} event topic.
     */
    public static final String EVENT_TOPIC_DEPLOYED =
            "org/osgi/service/web/DEPLOYED";

    /**
     * Constant for the {@code UNDEPLOYING} event topic.
     */
    public static final String EVENT_TOPIC_UNDEPLOYING =
            "org/osgi/service/web/UNDEPLOYING";

    /**
     * Constant for the {@code UNDEPLOYED} event topic.
     */
    public static final String EVENT_TOPIC_UNDEPLOYED =
            "org/osgi/service/web/UNDEPLOYED";

    /**
     * Constant for the {@code FAILED} event topic.
     */
    public static final String EVENT_TOPIC_FAILED =
            "org/osgi/service/web/FAILED";

    // various properties published as part of the event data

    /**
     * Event property for bundle symbolic name.
     */
    public static final String EVENT_PROPERTY_BUNDLE_SYMBOLICNAME =
            "bundle.symbolicName";

    /**
     * Event property for bundle id.
     */
    public static final String EVENT_PROPERTY_BUNDLE_ID = "bundle.id";

    /**
     * Event property for bundle.
     */
    public static final String EVENT_PROPERTY_BUNDLE = "bundle";

    /**
     * Event property for bundle version.
     */
    public static final String EVENT_PROPERTY_BUNDLE_VERSION =
            "bundle.version";

    /**
     * Event property for context path.
     */
    public static final String EVENT_PROPERTY_CONTEXT_PATH = "context.path";

    /**
     * Event property for timestamp.
     */
    public static final String EVENT_PROPERTY_TIMESTAMP = "timestamp";

    /**
     * Event property for extender.
     */
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE =
            "extender.bundle";

    /**
     * Event property for extender bundle id.
     */
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_ID =
            "extender.bundle.id";

    /**
     * Event property for bundle symbolic name.
     */
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_NAME =
            "extender.bundle.symbolicName";

    /**
     * Event property for extender bundle version.
     */
    public static final String EVENT_PROPERTY_EXTENDER_BUNDLE_VERSION =
            "extender.bundle.version";

    /**
     * Event property for exception.
     */
    public static final String EVENT_PROPERTY_EXCEPTION = "exception";

    /**
     * Event property for collision.
     */
    public static final String EVENT_PROPERTY_COLLISION = "collision";

    /**
     * Event property for collision bundles.
     */
    public static final String EVENT_PROPERTY_COLLISION_BUNDLES =
            "collision.bundles";

    // Below are GlassFish specific constants

    /**
     * Property name for custom faces config.
     */
    public static final String FACES_CONFIG_ATTR =
            "glassfish.osgi.web.facesconfigs";

    /**
     * Property name for custom facelet config.
     */
    public static final String FACELET_CONFIG_ATTR =
            "glassfish.osgi.web.faceletconfigs";

    /**
     * Property name for faces annotated classes.
     */
    public static final String FACES_ANNOTATED_CLASSES =
            "glassfish.osgi.web.facesannotatedclasses";

    /**
     * Constant for the GlassFish domain config entry for virtual servers.
     */
    public static final String VIRTUAL_SERVERS = "Virtual-Servers";
}
