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

import org.glassfish.internal.data.ApplicationInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiApplicationInfo {

    private ApplicationInfo appInfo;
    private boolean isDirectoryDeployment;
    private Bundle bundle;
    private final ClassLoader classLoader;
    private ServiceReference osgiDeployerRef;

    public OSGiApplicationInfo(ApplicationInfo appInfo,
            boolean directoryDeployment, Bundle bundle,
            ClassLoader classLoader) {

        this.appInfo = appInfo;
        isDirectoryDeployment = directoryDeployment;
        this.bundle = bundle;
        this.classLoader = classLoader;
    }

    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(ApplicationInfo appInfo) {
        this.appInfo = appInfo;
    }

    public boolean isDirectoryDeployment() {
        return isDirectoryDeployment;
    }

    public void setDirectoryDeployment(boolean directoryDeployment) {
        isDirectoryDeployment = directoryDeployment;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public ServiceReference getDeployer() {
        return osgiDeployerRef;
    }

    public void setDeployer(ServiceReference osgiDeployerRef) {
        this.osgiDeployerRef = osgiDeployerRef;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
