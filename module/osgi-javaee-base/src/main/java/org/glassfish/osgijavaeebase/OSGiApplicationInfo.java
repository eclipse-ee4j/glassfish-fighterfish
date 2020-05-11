/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.internal.data.ApplicationInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Represents a deployed OSGi application.
 */
public final class OSGiApplicationInfo {

    /**
     * GlassFish application info.
     */
    private ApplicationInfo appInfo;

    /**
     * Flag that indicates if the application deployed from a directory.
     */
    private boolean isDirectoryDeployment;

    /**
     * The bundle.
     */
    private Bundle bundle;

    /**
     * The application class-loader.
     */
    private final ClassLoader classLoader;

    /**
     * The deployer OSGi service reference.
     */
    private ServiceReference osgiDeployerRef;

    /**
     * Create a new instance.
     *
     * @param gfAppInfo the GlassFish application info
     * @param directoryDeployment flag for directory deployment
     * @param bnd the application bundle
     * @param cl the application class-loader
     */
    public OSGiApplicationInfo(final ApplicationInfo gfAppInfo, final boolean directoryDeployment, final Bundle bnd, final ClassLoader cl) {

        this.appInfo = gfAppInfo;
        isDirectoryDeployment = directoryDeployment;
        this.bundle = bnd;
        this.classLoader = cl;
    }

    /**
     * Get the underlying GlassFish application info.
     *
     * @return ApplicationInfo
     */
    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    /**
     * Set the underlying GlassFish application info.
     *
     * @param gfAppInfo the application to set
     */
    public void setAppInfo(final ApplicationInfo gfAppInfo) {
        this.appInfo = gfAppInfo;
    }

    /**
     * Indicate if the application is deployed from a directory.
     *
     * @return {@code true} if deployed from a directory, {@code false} otherwise
     */
    public boolean isDirectoryDeployment() {
        return isDirectoryDeployment;
    }

    /**
     * Set the directory deployment flag.
     *
     * @param dirDeplFlag the new value for the directory deployment flag
     */
    public void setDirectoryDeployment(final boolean dirDeplFlag) {
        isDirectoryDeployment = dirDeplFlag;
    }

    /**
     * Get the application bundle.
     *
     * @return Bundle
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * Set the application bundle.
     *
     * @param bnd the new application bundle
     */
    public void setBundle(final Bundle bnd) {
        this.bundle = bnd;
    }

    /**
     * Get the deployer OSGi service reference.
     *
     * @return ServiceReference
     */
    public ServiceReference getDeployer() {
        return osgiDeployerRef;
    }

    /**
     * Set the deployer OSGi service reference.
     *
     * @param ref the new reference
     */
    public void setDeployer(final ServiceReference ref) {
        this.osgiDeployerRef = ref;
    }

    /**
     * Get the application class-loader.
     *
     * @return ClassLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
