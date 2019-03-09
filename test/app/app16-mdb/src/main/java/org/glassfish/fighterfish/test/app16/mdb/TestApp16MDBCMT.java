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


import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Message-Driven Bean implementation class for: TestApp16MDB.
 */
@MessageDriven(
        activationConfig = {
            @ActivationConfigProperty(
                    propertyName = "destinationType",
                    propertyValue = "javax.jms.Topic"
            )},
        mappedName = "jms/fighterfish.TestApp16Topic"
)
@SuppressWarnings("checkstyle:DesignForExtension")
public class TestApp16MDBCMT extends TestApp16MDBBase {

    /**
     * Interceptor for setEM.
     * @param ctx invocation context
     * @return Object
     * @throws Exception if an error occurs
     */
    @AroundInvoke
    Object setEM(final InvocationContext ctx) throws Exception {
        log("entering setEM()");
        em = getEmf().createEntityManager();
        try {
            Object result = ctx.proceed();
            return result;
        } finally {
            em.close();
            log("exiting setEM()");
        }
    }
}
