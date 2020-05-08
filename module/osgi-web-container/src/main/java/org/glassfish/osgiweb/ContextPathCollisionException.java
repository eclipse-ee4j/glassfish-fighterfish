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
package org.glassfish.osgiweb;

import org.glassfish.osgijavaeebase.DeploymentException;

import java.util.Arrays;

/**
 * This exception is thrown when multiple WABs have same Web-ContextPath.
 */
class ContextPathCollisionException extends DeploymentException {

    /**
     * Context paths.
     */
    private final String contextPath;

    /**
     * set of colliding web application bundle id.
     */
    private final Long[] collidingWabIds;

    /**
     * Create a new instance.
     * 
     * @param ctxPath context path for which collision is detected
     * @param wabIds bundle id of the WABs that have same context path. The last entry denotes the current bundle being
     * deployed
     */
    ContextPathCollisionException(final String ctxPath, final Long[] wabIds) {

        if (wabIds.length < 2) {
            throw new IllegalArgumentException("At least two WAB ids are needed");
        }
        this.contextPath = ctxPath;
        this.collidingWabIds = Arrays.copyOf(wabIds, wabIds.length);
        Arrays.sort(this.collidingWabIds);
    }

    /**
     * Get the context path.
     * 
     * @return context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Get the colliding web application bundle id.
     * 
     * @return Long[]
     */
    public Long[] getCollidingWabIds() {
        // return a new copy
        return Arrays.copyOfRange(collidingWabIds, 0, collidingWabIds.length);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("context path [" + contextPath + "] is same for following bundles: [");
        for (int i = 0; i < collidingWabIds.length; i++) {
            sb.append(collidingWabIds[i]);
            if (i != collidingWabIds.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
