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

package org.glassfish.osgijta;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleContext;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JTAExtender implements Extender {
    private final BundleContext ctx;

    public JTAExtender(BundleContext ctx) {
        this.ctx = ctx;
    }

    public void start() {
        Class[] classes = {UserTransaction.class, TransactionManager.class, TransactionSynchronizationRegistry.class};
        String[] jndiNames = {"java:comp/UserTransaction", "java:appserver/TransactionManager", "java:appserver/TransactionSynchronizationRegistry"};
        for (int i = 0; i < 3; ++i) {
            registerProxy(classes[i], jndiNames[i]);
        }
    }

    private void registerProxy(Class clazz, String jndiName) {
        InvocationHandler ih = new MyInvocationHandler(clazz, jndiName);
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{clazz}, ih);
        ctx.registerService(clazz.getName(), proxy, null);
    }

    public void stop() {
    }

    private class MyInvocationHandler implements InvocationHandler {
        private Class<?> clazz;
        private String jndiName;

        private MyInvocationHandler(Class<?> clazz, String jndiName) {
            this.clazz = clazz;
            this.jndiName = jndiName;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                InitialContext ic = getInitialContext();
                Object target = ic.lookup(jndiName);
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    // We need to unwrap the real exception and throw it
                    throw e.getCause();
                }
            } catch (NamingException e) {
                throw new RuntimeException("JTA Service is not available.", e);
            }
        }
    }

    private InitialContext getInitialContext() throws NamingException {
        return new InitialContext();
    }
}
