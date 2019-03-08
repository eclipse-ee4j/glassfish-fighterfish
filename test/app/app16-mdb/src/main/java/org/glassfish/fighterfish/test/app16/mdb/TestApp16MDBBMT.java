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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.transaction.UserTransaction;

/**
 * Message-Driven Bean implementation class for: TestApp16MDB_BMT.
 */
@MessageDriven(
        activationConfig = {
            @ActivationConfigProperty(
                    propertyName = "destinationType",
                    propertyValue = "javax.jms.Topic"
            )},
        mappedName = "jms/fighterfish.TestApp16Topic"
)
@TransactionManagement(TransactionManagementType.BEAN)
@SuppressWarnings("checkstyle:DesignForExtension")
public class TestApp16MDBBMT extends TestApp16MDBBase {

    /**
     * User transaction.
     */
    @Inject
    @OSGiService
    private UserTransaction utx;

    /**
     * Interceptor for setEM.
     *
     * @param ctx invocation context
     * @return Object
     * @throws Exception if an error occurs
     */
    @AroundInvoke
    Object setEM(final InvocationContext ctx) throws Exception {
        log("entering setEM()");
        utx.begin();
        em = getEmf().createEntityManager();
        em.joinTransaction();
        try {
            Object result = ctx.proceed();
            utx.commit();
            return result;
        } catch (Exception e) {
            utx.rollback();
            throw e;
        } finally {
            em.close();
            log("exiting setEM()");
        }
    }
}
