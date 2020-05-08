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

/**
 * Deployment exception.
 */
public class DeploymentException extends Exception {

    /**
     * Create a new exception with no message and no cause.
     */
    public DeploymentException() {
    }

    /**
     * Create a new exception with a message and no cause.
     *
     * @param message exception message
     */
    public DeploymentException(final String message) {
        super(message);
    }

    /**
     * Create a new exception with a message and a cause.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public DeploymentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new exception with no message and a cause.
     *
     * @param cause exception cause
     */
    public DeploymentException(final Throwable cause) {
        super(cause);
    }
}
