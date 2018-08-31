/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.uas.entities;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This activator is responsible for registering an EMF as an OSGi service
 * 
 * @author Sanjeeb Sahoo
 *
 */
public class EntitiesActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		final String puName = "sample.uas.entities";
		
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

            // createEMF does not cause java2db to happen - at least that's the behavior in EclipseLink.
            // so, calling createEM will force java2db to happen.
            // If we don't java2db here, it will happen the first time someone uses our EMF and that could
            // be part of a transaction and we can get into deadlocks based on RDBMS trype.
            emf.createEntityManager().close();
			return emf;
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
	
	private void log(String msg) {
		System.out.println("EntitiesActivator: " + msg);
	}

}
