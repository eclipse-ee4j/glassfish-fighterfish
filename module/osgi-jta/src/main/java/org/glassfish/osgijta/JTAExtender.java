/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleContext;

/**
 * JTA extender.
 */
public final class JTAExtender implements Extender {

    /**
     * JTA classes.
     */
    private static final Class[] JTA_CLASSES = { UserTransaction.class, TransactionManager.class, TransactionSynchronizationRegistry.class };

    /**
     * JNDI names for the JTA classes.
     */
    private static final String[] JTA_JDNI_NAMES = { "java:comp/UserTransaction", "java:appserver/TransactionManager",
            "java:appserver/TransactionSynchronizationRegistry" };

    /**
     * Bundle context.
     */
    private final BundleContext ctx;

    /**
     * Create a new instance.
     *
     * @param bndCtx bundle context
     */
    public JTAExtender(final BundleContext bndCtx) {
        this.ctx = bndCtx;
    }

    @Override
    public void start() {
        for (int i = 0; i < JTA_CLASSES.length; ++i) {
            registerProxy(JTA_CLASSES[i], JTA_JDNI_NAMES[i]);
        }
    }

    /**
     * Register a proxy service for the given class and JNDI name.
     *
     * @param zeClass the class to proxy
     * @param zeJndiName the JNDI name to use
     */
    private void registerProxy(final Class zeClass, final String zeJndiName) {
        InvocationHandler ih = new MyInvocationHandler(zeClass, zeJndiName);
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { zeClass }, ih);
        ctx.registerService(zeClass.getName(), proxy, null);
    }

    @Override
    public void stop() {
    }

    /**
     * Proxy invocation handler.
     */
    private class MyInvocationHandler implements InvocationHandler {

        /**
         * The JNDI name.
         */
        private final String jndiName;

        /**
         * Create a new instance.
         *
         * @param zeClass the class to proxy
         * @param zeJndiName the JNDI name to use
         */
        MyInvocationHandler(final Class<?> zeClass, final String zeJndiName) {
            this.jndiName = zeJndiName;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

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

    /**
     * Create the JNDI initial context.
     *
     * @return InitialContext
     * @throws NamingException if an error occurs
     */
    private InitialContext getInitialContext() throws NamingException {
        return new InitialContext();
    }
}
