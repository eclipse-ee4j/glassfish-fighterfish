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
package org.glassfish.obrbuilder;

/**
 * Injection field metadata.
 */
public final class HK2InjectMetadata {

    /**
     * Injection field class name.
     */
    private final String injectionFieldClassName;

    /**
     * Is the field annotated with {@code @Optional}.
     */
    private final boolean isOptional;

    /**
     * Create a new instance.
     * @param fieldClassName injection field class name
     * @param isOpt is optional flag
     */
    public HK2InjectMetadata(final String fieldClassName, final boolean isOpt) {
        this.injectionFieldClassName = fieldClassName;
        this.isOptional = isOpt;
    }

    /**
     * Get the injection field class name.
     * @return String
     */
    public String getInjectionFieldClassName() {
        return injectionFieldClassName;
    }

    /**
     * Get the is optional flag value.
     * @return {@code true} if the field is annotated with {@code @Optional},
     * {@code false} otherwise
     */
    public boolean getOptional() {
        return isOptional;
    }
}
