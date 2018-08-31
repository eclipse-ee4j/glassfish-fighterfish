/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgicdi;

import org.osgi.framework.ServiceException;

/**
 * This exception is thrown to indicate that the service is unavailable.
 * If an <code>OSGiService</code> service reference is marked as dynamic, 
 * an attempt is made to get a reference to the service in the OSGi Service 
 * Registry when the service is used, and then the method is 
 * invoked on the newly obtained service. If the service cannot be discovered
 * or a reference obtained, the <code>ServiceUnavailableException</code>
 * is thrown.
 * 
 * @author Sivakumar Thyagarajan
 */
public class ServiceUnavailableException extends ServiceException {

    private static final long serialVersionUID = -8776963108373969053L;

    /**
     * {@inheritDoc}
     */
    public ServiceUnavailableException(String msg, int type, Throwable cause) {
        super(msg, type, cause);
    }

}
