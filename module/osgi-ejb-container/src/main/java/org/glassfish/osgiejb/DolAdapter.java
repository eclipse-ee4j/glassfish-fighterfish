/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
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
 * We need this class to encapsulate our dependency on EJB DOL. They have
 * changed binary incompatibly between GlassFish 3.1.x and 4.x, so we write this
 * class to use those objects using reflection.
 */
final class DolAdapter {

    /**
     * Cannot be instanciated.
     */
    private DolAdapter() {
    }

    /**
     * Convert GlassFish EjbDescriptor to fighterfish EJB descriptor.
     * @param ejbDescriptors instances to convert
     * @return converted instances
     */
    //CHECKSTYLE:OFF
    static Collection<EjbDescriptor> convert(
            final Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbDescriptors) {
    //CHECKSTYLE:ON

        Collection<EjbDescriptor> result =
                new ArrayList<EjbDescriptor>(ejbDescriptors.size());
        for (com.sun.enterprise.deployment.EjbDescriptor ejbDescriptor
                : ejbDescriptors) {
            Class[] interfaces;
            //CHECKSTYLE:OFF
            if (ejbDescriptor instanceof com.sun.enterprise.deployment.EjbSessionDescriptor) {
            //CHECKSTYLE:ON
                interfaces = new Class[]{EjbSessionDescriptor.class};
            } else {
                interfaces = new Class[]{EjbDescriptor.class};
            }
            final EjbDescriptor proxy = (EjbDescriptor) Proxy.newProxyInstance(
                    EjbDescriptor.class.getClassLoader(),
                    interfaces,
                    new EJbDolInvocationHandler(ejbDescriptor));
            result.add(proxy);
        }
        return result;
    }

    /**
     * Fighterfish EJB descriptor.
     */
    interface EjbDescriptor {

        /**
         * Get the name.
         * @return name
         */
        String getName();

        /**
         * Get the type.
         * @return type
         */
        String getType();
    }

    /**
     * EJB session descriptor.
     */
    interface EjbSessionDescriptor extends EjbDescriptor {

        /**
         * Get the session type.
         * @return session type
         */
        String getSessionType();

        /**
         * Get the local business class names.
         * @return set of class names
         */
        Set<String> getLocalBusinessClassNames();

        /**
         * Get the portable JNDI name.
         * @param lbi business class name
         * @return portable JNDI name
         */
        String getPortableJndiName(String lbi);
    }

    /**
     * Custom invocation handler to proxy GlassFish ejb descriptor.
     */
    private static final class EJbDolInvocationHandler
            implements InvocationHandler {

        /**
         * Delegate instance.
         */
        private final com.sun.enterprise.deployment.EjbDescriptor delegate;

        /**
         * Create a new instance.
         * @param desc delegate instance
         */
        EJbDolInvocationHandler(
                final com.sun.enterprise.deployment.EjbDescriptor desc) {
            this.delegate = desc;
        }

        @Override
        public Object invoke(final Object proxy, final Method method,
                final Object[] args) throws Throwable {

            Method m = delegate.getClass().getMethod(method.getName(),
                    method.getParameterTypes());
            return m.invoke(delegate, args);
        }

        /**
         * Get the delegate descriptor.
         * @return delegate descriptor
         */
        public com.sun.enterprise.deployment.EjbDescriptor getDelegate() {
            return delegate;
        }
    }
}
