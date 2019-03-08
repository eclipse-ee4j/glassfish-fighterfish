/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app16.msgproducer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Dictionary;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Bundle activator.
 */
public final class TestApp16ProducerActivator implements BundleActivator {

    @Override
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        System.out.println(
                "Message producer started - "
                + "waiting to be configured with topic name");
        Properties props = new Properties();
        final String pkgName = getClass().getPackage().getName();
        props.put(Constants.SERVICE_PID, pkgName);
        context.registerService(ManagedService.class.getName(),
                new ManagedService() {
            @Override
            public void updated(final Dictionary properties)
                    throws ConfigurationException {
                if (properties != null) {
                    String destinationName = (String) properties
                            .get(pkgName + ".Destination");
                    String connectionFactoryName = (String) properties
                            .get(pkgName + ".ConnectionFactory");
                    int noOfMsgs = Integer.valueOf((String) properties
                            .get(pkgName + ".NoOfMsgs"));
                    sendMessage(connectionFactoryName, destinationName,
                            noOfMsgs);
                }
            }
        }, (Dictionary) props);
    }

    /**
     * Create connection. Create session from connection; false means session is
     * not transacted. Create producer and text message. Send messages, varying
     * text slightly. Send end-of-messages message. Finally, close connection.
     * @param cfName connection factory name
     * @param destName destination name
     * @param noOfMsgs number of messages
     */
    @SuppressWarnings("unchecked")
    private void sendMessage(final String cfName, final String destName,
            final int noOfMsgs) {

        Connection connection = null;
        try {
            InitialContext ctx = new InitialContext();
            ConnectionFactory connectionFactory =
                    (ConnectionFactory) ctx.lookup(cfName);
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);

            Destination dest = (Destination) ctx.lookup(destName);
            MessageProducer producer = session.createProducer(dest);
            TextMessage message = session.createTextMessage();

            for (int i = 0; i < noOfMsgs; i++) {
                message.setText("This is message " + (i + 1));
                System.out.println("Sending message: " + message.getText());
                producer.send(message);
            }

            // Send a non-text control message indicating end of messages.
            producer.send(session.createMessage());
        } catch (JMSException e) {
            System.err.println("Exception occurred: " + e.toString());
        } catch (NamingException e) {
            System.err.println("Exception occurred: " + e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                }
            }
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
    }
}
