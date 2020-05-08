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

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;

import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;

/**
 * This implementation is specialized for OSGi-ed WAR files. It can be extended for different type of OSGi archives. It
 * is not exported as a Service.
 */
public class OSGiArchiveHandler extends AbstractArchiveHandler {

    /**
     * This implementation returns {@code OSGiBundle}.
     *
     * @return {@code OSGiBundle}
     */
    @Override
    public String getArchiveType() {
        return "OSGiBundle";
    }

    /**
     * This implementation always returns false.
     *
     * @return {@code false}
     */
    @Override
    public boolean handles(final ReadableArchive archive) {
        // We don't want this handler to participate in any automatic
        // discovery, so it returns false.
        return false;
    }

    /**
     * This implementation always throws a {@code RuntimeException}, this method should not be called.
     *
     * @param parent parent class-loader
     * @param context deployment context
     * @throws RuntimeException always
     * @return ClassLoader
     */
    @Override
    public ClassLoader getClassLoader(final ClassLoader parent, final DeploymentContext context) {

        throw new RuntimeException("Assertion Failure: This method should not be called");
    }

    /**
     * This implementation derives the application name from the archive name.
     *
     * @param archive application archive
     * @return String
     */
    @Override
    public String getDefaultApplicationName(final ReadableArchive archive) {
        String appName = archive.getName();
        int lastDot = appName.lastIndexOf('.');
        if (lastDot != -1) {
            appName = appName.substring(0, lastDot);
        }
        return appName;
    }

    /**
     * This implementation derives the application anme from the archive name.
     *
     * @param archive application archive
     * @param context deployment context
     * @return String
     */
    @Override
    public String getDefaultApplicationName(final ReadableArchive archive, final DeploymentContext context) {

        return getDefaultApplicationName(archive);
    }
}
