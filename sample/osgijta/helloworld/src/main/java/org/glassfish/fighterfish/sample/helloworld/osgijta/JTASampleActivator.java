/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.helloworld.osgijta;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Bundle activator.
 */
public final class JTASampleActivator implements BundleActivator {

    @Override
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        debug("Started");
        ServiceReference txRef = context.getServiceReference(UserTransaction.class.getName());
        UserTransaction utx = (UserTransaction) context.getService(txRef);
        ServiceReference tsrRef = context.getServiceReference(TransactionSynchronizationRegistry.class.getName());
        TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) context.getService(tsrRef);
        try {
            debug("Status: before utx.begin: " + statusToString(utx.getStatus()));
            utx.begin();
            debug("Status: after utx.begin: " + statusToString(utx.getStatus()));

            // Get hold of JTA DataSource and do some operations using
            // connection obtained from there
            // ...
            // Let's listen to transaction life cycle event.
            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    debug("beforeCompletion");
                }

                @Override
                public void afterCompletion(final int i) {
                    debug("afterCompletion");
                }
            });

            utx.commit();
            debug("Transaction test completed");
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.ungetService(txRef);
        context.ungetService(tsrRef);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        debug("Stopped");
    }

    /**
     * Maps status integer as returned by getStatus() method to a String.
     *
     * @param status status int
     * @return status string
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static String statusToString(final int status) {
        switch (status) {
        case 0:
            return "ACTIVE";
        case 1:
            return "MARKED_ROLLBACK";
        case 2:
            return "PREPARED";
        case 3:
            return "COMMITTED";
        case 4:
            return "ROLLEDBACK";
        case 5:
            return "UNKNOWN";
        case 6:
            return "NO_TRANSACTION";
        case 7:
            return "PREPARING";
        case 8:
            return "COMMITTING";
        case 9:
            return "ROLLING_BACK";
        default:
            return "NOT_YET_MAPPED";
        }
    }

    /**
     * Log a message to the standard output.
     *
     * @param msg message to log
     */
    private void debug(final String msg) {
        System.out.println("JTATestBundleActivator: " + msg);
    }
}
