/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijavaeebase;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.osgi.framework.Bundle;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Custom GlassFish deployment context.
 */
public abstract class OSGiDeploymentContext extends DeploymentContextImpl {

    /**
     * Temporary class-loader.
     */
    private ClassLoader shareableTempClassLoader;

    /**
     * Final class-loader.
     */
    private ClassLoader finalClassLoader;

    /**
     * The bundle.
     */
    private final Bundle bundle;

    /**
     * Create a new instance.
     * @param actionReport GlassFish command reporter
     * @param logger logger
     * @param source application archive
     * @param params GlassFish command parameters
     * @param env GlassFish server environment
     * @param bnd application bundle
     * @throws Exception if an error occurs
     */
    public OSGiDeploymentContext(final ActionReport actionReport,
            final Logger logger, final ReadableArchive source,
            final OpsParams params, final ServerEnvironment env,
            final Bundle bnd) throws Exception {

        super(actionReport, logger, source, params, env);
        this.bundle = bnd;
        setupClassLoader();

        // We always this handler instead of going through discovery process
        // which has issues.
        setArchiveHandler(new OSGiArchiveHandler());
    }

    /**
     * Setup the class-loader.
     * @throws Exception if an error occurs
     */
    protected abstract void setupClassLoader() throws Exception;

    @Override
    public void createDeploymentClassLoader(final ClassLoaderHierarchy clh,
            final ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        // do nothing as we override getClassLoader methods.
    }

    @Override
    public void createApplicationClassLoader(final ClassLoaderHierarchy clh,
            final ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        // do nothing as we override getClassLoader methods.
    }

    @Override
    public final ClassLoader getClassLoader() {
        if (getPhase() != Phase.PREPARE) {
            // we return the final class loader
            return finalClassLoader;
        }
        return shareableTempClassLoader;
    }

    @Override
    public final ClassLoader getFinalClassLoader() {
        return finalClassLoader;
    }

    /**
     * Set the final class-loader.
     * @param cl class-loader
     */
    public final void setFinalClassLoader(final ClassLoader cl) {
        this.finalClassLoader = cl;
    }

    /**
     * Get the temporary class-loader.
     * @return class-loader
     */
    public final ClassLoader getShareableTempClassLoader() {
        return shareableTempClassLoader;
    }

    /**
     * Set the temporary class-loader.
     * @param cl class-loader
     */
    public final void setShareableTempClassLoader(final ClassLoader cl) {
        this.shareableTempClassLoader = cl;
    }

    @Override
    public final synchronized ClassLoader getClassLoader(
            final boolean sharable) {

        throw new RuntimeException("Assertion Failure: "
                + "This method should not be called");
    }

    /**
     * Get the application bundle.
     * @return Bundle
     */
    public Bundle getBundle() {
        return bundle;
    }
}
