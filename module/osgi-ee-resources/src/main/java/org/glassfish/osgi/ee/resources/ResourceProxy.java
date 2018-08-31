/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**
 * ResourceProxy that can delegate to actual objects upon usage.<br>
 * Does not cache the actual object as the actual object can be re-configured<br>
 *
 * @author Jagadish Ramu
 */
public class ResourceProxy implements InvocationHandler, Invalidate {

    private String jndiName;
    private boolean invalidated = false;

    public ResourceProxy(String jndiName) {
        this.jndiName = jndiName;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        if (method.getName().equals("invalidate")) {
            invalidate();
        } else {
            result = method.invoke(getActualObject(), args);
        }
        return result;
    }

    /**
     * It is possible that reconfiguration of resource will happen.<br>
     * Always do lookup.
     */
    private Object getActualObject() {
        if (!invalidated) {
            try {
                return new InitialContext().lookup(jndiName);
            } catch (NamingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new RuntimeException("Resource [" + jndiName + "] is invalidated");
        }
    }

    /**
     * Sets the state of the proxy as invalid<br>
     */
    public void invalidate() {
        invalidated = true;
    }
}
