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
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * @author sanjeeb.sahoo@oracle.com
 *
 */
public class TestApp16MDB_Base  implements MessageListener{

    @Inject
    @OSGiService(dynamic = true, serviceCriteria = "(persistence-unit=test.app16.entities)")
    protected EntityManagerFactory emf;
    protected EntityManager em;

    /**
     * 
     */
    public TestApp16MDB_Base() {
        super();
    }

    /**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message message) {
        String str = null;
        if (message instanceof TextMessage) {
            try {
                str = TextMessage.class.cast(message).getText();
                org.glassfish.fighterfish.test.app16.entities.Message msg = new org.glassfish.fighterfish.test.app16.entities.Message();
                msg.setValue(str);
                em.persist(msg);
                log(str);
            } catch (JMSException e) {
                // ignore
            }
        } else {
            log(message.toString() + " is being ignored as it is not a TextMessage");
        }
    }

    /**
     * @param str
     */
    protected void log(String str) {
        System.out.println(getClass().getSimpleName() + ": " + str);
    }

}
