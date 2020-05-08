/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgihttp;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.sun.enterprise.web.WebModule;

/**
 * Utility that holds an instance of {@link InvocationContext}.
 */
public final class InvocationContextMgr {

    /**
     * Cannot be instanciated.
     */
    private InvocationContextMgr() {
    }

    /**
     * The invocation context instance.
     */
    private static final InvocationContext INVOCATION_CTX = new InvocationContextImpl();

    /**
     * Get the invocation context.
     *
     * @return InvocationContext
     */
    public static InvocationContext getInvocationContext() {
        return INVOCATION_CTX;
    }

    /**
     * Implementation of {@link InvocationContext}.
     */
    private static final class InvocationContextImpl implements InvocationContext {

        /**
         * Logger.
         */
        private final Logger logger = Logger.getLogger(InvocationContextImpl.class.getPackage().getName());

        /**
         * Thread local to access the current servlet context.
         */
        private final ThreadLocal<WeakReference<ServletContext>> currentSC = new InheritableThreadLocal<>();

        /**
         * Thread local to access the current web module.
         */
        private final ThreadLocal<WeakReference<WebModule>> currentWM = new InheritableThreadLocal<>();

        @Override
        public WebModule getWebModule() {
            WeakReference<WebModule> current = currentWM.get();
            WebModule result;
            if (current != null) {
                result = current.get();
            } else {
                result = null;
            }
            logger.logp(Level.FINE, "InvocationContextMgr", "getWebModule", "result = {0}", new Object[] { result });
            return result;
        }

        @Override
        public void setWebModule(final WebModule webModule) {
            logger.logp(Level.FINE, "InvocationContextMgr", "setWebModule", "webModule = {0}", new Object[] { webModule });
            if (webModule != null) {
                currentWM.set(new WeakReference<>(webModule));
            } else {
                currentWM.set(null);
            }
        }
    }
}
