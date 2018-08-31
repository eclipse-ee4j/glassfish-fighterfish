/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgiejb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * We need this class to encapsulate our dependency on EJB DOL. They have changed binary incompatibly between
 * GlassFish 3.1.x and 4.x, so we write this class to use those objects using reflection.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
/*package*/final class DolAdapter {
    static Collection<EjbDescriptor> convert(Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbDescriptors) {
        Collection<EjbDescriptor> result = new ArrayList<EjbDescriptor>(ejbDescriptors.size());
        for (com.sun.enterprise.deployment.EjbDescriptor ejbDescriptor : ejbDescriptors) {
            Class[] interfaces = (ejbDescriptor instanceof com.sun.enterprise.deployment.EjbSessionDescriptor) ?
                    new Class[] {EjbSessionDescriptor.class} :
                    new Class[] {EjbDescriptor.class};
            final EjbDescriptor proxy = (EjbDescriptor) Proxy.newProxyInstance(
                    EjbDescriptor.class.getClassLoader(),
                    interfaces,
                    new EJbDolInvocationHandler(ejbDescriptor));
            result.add(proxy);
        }
        return result;
    }

    static interface EjbDescriptor {
        String getName();

        String getType();

    }

    static interface EjbSessionDescriptor extends EjbDescriptor {
        String getSessionType();

        Set<String> getLocalBusinessClassNames();

        String getPortableJndiName(String lbi);
    }

    static class EJbDolInvocationHandler implements InvocationHandler {
        protected final com.sun.enterprise.deployment.EjbDescriptor delegate;

        public EJbDolInvocationHandler(com.sun.enterprise.deployment.EjbDescriptor delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method m = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
            return m.invoke(delegate, args);
        }
    }
}
