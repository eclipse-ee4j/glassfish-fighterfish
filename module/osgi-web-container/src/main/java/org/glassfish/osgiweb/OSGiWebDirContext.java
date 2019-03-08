/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgiweb;

import org.apache.naming.resources.WebDirContext;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * As per WAB spec, resources must not be allowed from OSGI-INF or OSGI-OPT
 * dirs. So, we install a special dir context that takes care of this
 * requirement.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
class OSGiWebDirContext extends WebDirContext {

    private static final Logger LOGGER = Logger.getLogger(
            OSGiWebDirContext.class.getPackage().getName());

    @Override
    protected File file(String name) {
        final String s = name.toUpperCase();
        if (s.startsWith("/OSGI-INF/") || s.startsWith("/OSGI-OPT/")) {
            LOGGER.logp(Level.FINE, getClass().getSimpleName(), "file",
                    "Forbidding access to resource called {0}",
                    new Object[]{name});
            return null;
        } else {
            return super.file(name);
        }
    }
}
