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

package org.glassfish.fighterfish.test.app16.mdb;

import org.glassfish.osgicdi.OSGiService;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.glassfish.fighterfish.test.app16.entities.Message;

/**
 * Base class for MDB implementations.
 */
public class TestApp16MDBBase  implements MessageListener {

    /**
     * Entity manager factory.
     */
    @Inject
    @OSGiService(dynamic = true,
            serviceCriteria = "(persistence-unit=test.app16.entities)")
    private EntityManagerFactory emf;

    /**
     * Entity manager.
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected EntityManager em;

    /**
     * Create a new instance.
     */
    protected TestApp16MDBBase() {
    }

    /**
     * Get the entity manager factory.
     * @return EntityManagerFactory
     */
    protected EntityManagerFactory getEmf() {
        return emf;
    }

    /**
     * This implementation persists the message in database.
     * @param message incoming message
     */
    @Override
    public void onMessage(final javax.jms.Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        String str;
        if (message instanceof TextMessage) {
            try {
                str = TextMessage.class.cast(message).getText();
                Message msg = new Message();
                msg.setValue(str);
                em.persist(msg);
                log(str);
            } catch (JMSException e) {
                // ignore
            }
        } else {
            log(message.toString()
                    + " is being ignored as it is not a TextMessage");
        }
    }

    /**
     * Log a message to the standard output.
     * @param msg message to log
     */
    protected static void log(final String msg) {
        System.out.println(TestApp16MDBBase.class.getSimpleName()
                + ": " + msg);
    }
}
