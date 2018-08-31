/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.helloworld.osgijdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class JDBCSampleActivator implements BundleActivator {

	private static final String DSNAME = "jdbc/__default"; // Should be a configurable property
	private ServiceTracker st;

	@Override
	public void start(BundleContext context) throws Exception {
		debug("Activator started");

		// Create an LDAP filter which matches both the interface type
		// as well as jndi-name property.
		Filter filter = context.createFilter("(&" + "(" + Constants.OBJECTCLASS
				+ "=" + DataSource.class.getName() + ")" + "(jndi-name="
				+ DSNAME + ")" + ")");
		st = new ServiceTracker(context, filter, null) {

			@Override
			public Object addingService(ServiceReference reference) {
				DataSource ds = (DataSource) context.getService(reference);
				try {
					debug(ds.getConnection().toString());
				} catch (SQLException e) {
					e.printStackTrace();
				}

				return super.addingService(reference);
			}

			@Override
			public void removedService(ServiceReference reference,
					Object service) {
				super.removedService(reference, service);
			}

		};
		st.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		st.close();
		debug("Activator stopped");
	}

	private void debug(String msg) {
		System.out.println("JDBCTestBundleActivator: " + msg);
	}

}
