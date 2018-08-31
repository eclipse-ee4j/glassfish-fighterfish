/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.helloworld.osgijta;

import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class JTASampleActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		debug("Started");
		ServiceReference txRef = context
				.getServiceReference(UserTransaction.class.getName());
		UserTransaction utx = (UserTransaction) context.getService(txRef);
		ServiceReference tsrRef = context
				.getServiceReference(TransactionSynchronizationRegistry.class
						.getName());
		TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) context
				.getService(tsrRef);
		try {
			debug("Status: before utx.begin: "
					+ statusToString(utx.getStatus()));
			utx.begin();
			debug("Status: after utx.begin: " + statusToString(utx.getStatus()));

			// Get hold of JTA DataSource and do some operations using
			// connection obtained from there
			// ...

			// Let's listen to transaction life cycle event.
			tsr.registerInterposedSynchronization(new Synchronization() {
				public void beforeCompletion() {
					debug("beforeCompletion");
				}

				public void afterCompletion(int i) {
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
	public void stop(BundleContext context) throws Exception {
		debug("Stopped");
	}

	/**
	 * Maps status integer as returned by getStatus() method to a String
	 */
	private static String statusToString(int status) {
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

	private void debug(String msg) {
		System.out.println("JTATestBundleActivator: " + msg);
	}
}
