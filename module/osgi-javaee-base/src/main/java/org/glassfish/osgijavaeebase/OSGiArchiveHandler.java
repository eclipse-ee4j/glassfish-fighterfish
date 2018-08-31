/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * An implementation of {@link org.glassfish.api.deployment.archive.ArchiveHandler}
 * specialized for OSGi-ed WAR files. It is not exported as a Service.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiArchiveHandler extends AbstractArchiveHandler
{
    public String getArchiveType()
    {
        return "OSGiBundle";
    }

    public boolean handles(ReadableArchive archive)
    {
        // We don't want this handler to participate in any automatic
        // discovery, so it returns false.
        return false;
    }

    public ClassLoader getClassLoader(ClassLoader parent, DeploymentContext context)
    {
        throw new RuntimeException("Assertion Failure: This method should not be called");
    }

    // Since we don't have a fixed file extension, we override
    // getDefaultApplicationName methods
    @Override
    public String getDefaultApplicationName(ReadableArchive archive)
    {
        String appName = archive.getName();
        int lastDot = appName.lastIndexOf('.');
        if (lastDot != -1) {
            appName = appName.substring(0, lastDot);
        }
        return appName;
    }

    @Override
    public String getDefaultApplicationName(ReadableArchive archive, DeploymentContext context)
    {
        return getDefaultApplicationName(archive);
    }
}
