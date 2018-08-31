/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.web.WebModule;

import javax.servlet.ServletContext;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class InvocationContextMgr {

    private static final InvocationContext invCtx = new InvocationContext() {
        private final Logger logger = Logger.getLogger(getClass().getPackage().getName());

        private ThreadLocal<WeakReference<ServletContext>> currentSC =
                new InheritableThreadLocal<WeakReference<ServletContext>>();

        private ThreadLocal<WeakReference<WebModule>> currentWM =
                new InheritableThreadLocal<WeakReference<WebModule>>();

        @Override
        public WebModule getWebModule() {
            WeakReference<WebModule> current = currentWM.get();
            WebModule result = current != null ? current.get() : null;
            logger.logp(Level.FINE, "InvocationContextMgr", "getWebModule", "result = {0}", new Object[]{result});
            return result;
        }

        @Override
        public void setWebModule(WebModule webModule) {
            logger.logp(Level.FINE, "InvocationContextMgr", "setWebModule", "webModule = {0}", new Object[]{webModule});
            currentWM.set(webModule != null ? new WeakReference<WebModule>(webModule) : null);
        }
        
    };

    public static InvocationContext getInvocationContext() {
        return invCtx;
    }
}
