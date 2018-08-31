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

package org.glassfish.fighterfish.test.app16.entities;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.Properties;

/**
 * @author sanjeeb.sahoo@oracle.com
 *
 */
public class TestApp16EntitiesActivator implements BundleActivator {

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        final String puName = "test.app16.entities";
        
        EntityManagerFactory emf = createEMF(puName);
        Properties props = new Properties();
        props.setProperty("persistence-unit", puName);
        context.registerService(EntityManagerFactory.class.getName(), emf, props);
        log("registered " + emf);
    }

    private EntityManagerFactory createEMF(final String puName) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(puName);

            // createEMF does not cause java2db to happen - at least that's the behaviour in EclipseLink.
            // so, calling createEM will force java2db to happen.
            // If we don't java2db here, it will happen the first time someone uses our EMF and that could
            // be part of a transaction and we can get into deadlocks based on RDBMS type.
            emf.createEntityManager().close();
            return emf;
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private void log(String msg) {
        System.out.println("TestApp16EntitiesActivator: " + msg);
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

}
