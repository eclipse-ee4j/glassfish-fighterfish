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
 * Common constants for the base module.
 */
public final class Constants {

    /**
     * Cannot be instanciated.
     */
    private Constants() {
    }

    /**
     * Protocol used both in Felix and Equinox to read content of a bundle directly from a jar or directory as opposed to
     * first copying it to bundle cache and then reading from there.
     */
    public static final String REFERENCE_PROTOCOL = "reference:";

    /**
     * File protocol.
     */
    public static final String FILE_PROTOCOL = "file:";

}
