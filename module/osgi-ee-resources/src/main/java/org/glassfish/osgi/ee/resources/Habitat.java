/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgi.ee.resources;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;

/**
 * Adapter for old Habitat's getComponent method We keep the class name same to
 * reduce the no of lines that we have to change.
 */
public final class Habitat {

    /**
     * GlassFish service.
     */
    private final GlassFish glassFish;

    /**
     * Create a new instance.
     * @param gf GlassFish service
     */
    Habitat(final GlassFish gf) {
        this.glassFish = gf;
    }

    /**
     * Lookup a component by type.
     * @param <T> component type
     * @param type component class
     * @return T
     */
    public <T> T getComponent(final Class<T> type) {
        try {
            return glassFish.getService(type);
        } catch (GlassFishException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
    }
}
