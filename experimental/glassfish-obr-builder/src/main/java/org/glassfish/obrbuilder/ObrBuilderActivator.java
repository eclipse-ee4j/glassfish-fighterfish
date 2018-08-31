/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.obrbuilder;

import java.net.URI;
import java.util.logging.Level;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.glassfish.obrbuilder.Logger.logger;

/**
 * @author TangYong(tangyong@cn.fujitsu.com)
 */
public class ObrBuilderActivator implements BundleActivator {

	private BundleContext bctx;
	ServiceRegistration registration = null;

	public void start(BundleContext context) throws Exception {
		this.bctx = context;

		String gfModuleRepoPath = context
				.getProperty(Constants.GF_MODULE_REPOSITORIES);

		createGFObrRepository(gfModuleRepoPath);
		//createGFObrRepository(gfModuleRepoPath + Constants.OBR_TEST_REPO);
	}

	public void stop(BundleContext context) throws Exception {
		if (registration != null) {
			registration.unregister();
		}
	}

	private void createGFObrRepository(String repositoryUris) {
		if (repositoryUris != null) {
			for (String s : repositoryUris.split("\\s")) {
				URI repoURI = URI.create(s);
				ObrHandlerService obrHandler = new ObrHandlerServiceImpl(bctx);
				try {
					obrHandler.addRepository(repoURI);
				} catch (Exception e) {
					e.printStackTrace();
					logger.logp(
							Level.SEVERE,
							"ObrBuilderActivator",
							"createGFObrRepository",
							"Creating Glassfish OBR Repository failed, RepoURI: {0}",
							new Object[] { repoURI });
				}
			}
		}
	}
}
