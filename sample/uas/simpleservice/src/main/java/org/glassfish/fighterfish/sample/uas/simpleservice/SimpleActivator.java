/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.uas.simpleservice;


import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SimpleActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(UserAuthService.class.getName(), new UserAuthServiceImpl(), null);
		log("Registered service");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// No need to unregister, as the service gets unregistered automatically when bundle stops.
	}
	
	static void log(String string) {
		System.out.println("UserAuthService: " + string);
	}

}
