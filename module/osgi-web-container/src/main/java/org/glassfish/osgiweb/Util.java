/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

import org.osgi.framework.Bundle;

import static org.glassfish.osgiweb.Constants.WEB_CONTEXT_PATH;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Util {

    /**
     * This method attached slash when context path header does not start with
     * /. This is done keeping the following observations in mind: a) GlassFish
     * web container automatically attaches a '/'. b) The r42 CT installs some
     * WABs which contains such context path. Unless we attach a '/', they fail.
     *
     * @param bundle
     * @return
     */
    static String getContextPath(Bundle bundle) {
        String cp = (String) bundle.getHeaders().get(WEB_CONTEXT_PATH);
        if (cp != null && !cp.startsWith("/")) {
            cp = "/".concat(cp);
        }
        return cp;
    }
}
