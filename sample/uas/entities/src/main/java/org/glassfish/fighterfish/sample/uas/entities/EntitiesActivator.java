/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.entities;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This activator is responsible for registering an EMF as an OSGi service.
 */
public final class EntitiesActivator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        String persistenceUnitName = "sample.uas.entities";
        EntityManagerFactory entityManagerFactory = createEMF(persistenceUnitName);
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("persistence-unit", persistenceUnitName);

        context.registerService(EntityManagerFactory.class.getName(), entityManagerFactory, properties);

        log("registered " + entityManagerFactory);
    }

    /**
     * Create the entity manager factory.
     *
     * @param persistenceUnitName persistence unit name
     * @return EntityManagerFactory
     */
    private EntityManagerFactory createEMF(String persistenceUnitName) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);

            // createEMF does not cause java2db to happen - at least that's
            // the behavior in EclipseLink.
            // so, calling createEM will force java2db to happen.
            // If we don't java2db here, it will happen the first time someone
            // uses our EMF and that could
            // be part of a transaction and we can get into deadlocks based on
            // RDBMS trype.
            entityManagerFactory.createEntityManager().close();
            return entityManagerFactory;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
    }

    /**
     * Log a message to the standard output.
     *
     * @param msg message to log
     */
    private void log(final String msg) {
        System.out.println("EntitiesActivator: " + msg);
    }
}
