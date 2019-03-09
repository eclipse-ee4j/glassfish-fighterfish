/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.advservice;

import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;

/**
 * Bundle activator for the advanced user authentication service.
 */
public final class AdvSvcImplActivator implements BundleActivator {

    /**
     * Transaction tracker.
     */
    private volatile ServiceTracker txTracker;

    /**
     * Entity manager factory tracker.
     */
    private volatile ServiceTracker emfTracker;

    /**
     * Persistence unit name.
     */
    // should be a configuration property
    private static final String PUNAME = "sample.uas.entities";

    @Override
    public void start(final BundleContext context) throws Exception {
        txTracker = new ServiceTracker(context,
                UserTransaction.class.getName(), null);
        txTracker.open();
        Filter filter = context.createFilter("(&"
                + "(" + Constants.OBJECTCLASS + "="
                + EntityManagerFactory.class.getName() + ")"
                + "(persistence-unit=" + PUNAME + ")"
                + ")");
        emfTracker = new ServiceTracker(context, filter, null);
        emfTracker.open();
        AdvUserAuthServiceImpl uas = new AdvUserAuthServiceImpl(this);
        context.registerService(UserAuthService.class.getName(), uas, null);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        txTracker.close();
        emfTracker.close();
    }

    /**
     * Get the user transaction.
     * @return UserTransaction
     */
    public UserTransaction getUTX() {
        return (UserTransaction) txTracker.getService();
    }

    /**
     * Get the EntityManagerFactory.
     * @return EntityManagerFactory
     */
    public EntityManagerFactory getEMF() {
        return (EntityManagerFactory) emfTracker.getService();
    }
}
