/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Constants used by this module.
 */
public final class Constants {

    /**
     * Cannot be instanciated.
     */
    private Constants() {
    }

    /**
     * Property name for JNDI name property.
     */
    public static final String JNDI_NAME = "jndi-name";

    /**
     * Property name for {@code javax.sql.DataSource}.
     */
    public static final  String DS = "javax.sql.DataSource";

    /**
     * Property name for {@code javax.sql.ConnectionPoolDataSource}.
     */
    public static final  String CPDS = "javax.sql.ConnectionPoolDataSource";

    /**
     * Property name for {@code javax.sql.XADataSource}.
     */
    public static final  String XADS = "javax.sql.XADataSource";

    /**
     * Property name for {@code java.sql.Driver}.
     */
    public static final  String DRIVER = "java.sql.Driver";

    /**
     * Constant for the default JMS adapter.
     */
    public static final  String DEFAULT_JMS_ADAPTER = "jmsra";

    // JMS - Connection Factories

    /**
     * Property name for JMS queue connection factory.
     */
    public static final  String QUEUE_CF = "javax.jms.QueueConnectionFactory";

    /**
     * Property name for JMS topic connection factory.
     */
    public static final  String TOPIC_CF = "javax.jms.TopicConnectionFactory";

    /**
     * Property name for JMS connection factory.
     */
    public static final  String UNIFIED_CF = "javax.jms.ConnectionFactory";

    // Admin Object Resources - Destinations

    /**
     * Property name for JMS queue.
     */
    public static final  String QUEUE = "javax.jms.Queue";

    /**
     * Property name for JMS topic.
     */
    public static final  String TOPIC = "javax.jms.Topic";

    /**
     * Property name for JMS destination.
     */
    public static final  String DESTINATION = "javax.jms.Destination";

}
