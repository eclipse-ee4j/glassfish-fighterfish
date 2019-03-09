/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgiejb;

/**
 * Constants for this module.
 */
public final class Constants {

    /**
     * Cannot be instanciated.
     */
    private Constants() {
    }

    /**
     * Constant for {@code Export-EJB} manifest entry.
     */
    public static final String EXPORT_EJB = "Export-EJB";

    /**
     * Constant for value {@code ALL} of {@code Export-EJB} manifest entry.
     */
    public static final String EXPORT_EJB_ALL = "ALL";

    /**
     * Constant for value {@code NONE} of {@code Export-EJB} manifest entry.
     */
    public static final String EXPORT_EJB_NONE = "NONE";
}
