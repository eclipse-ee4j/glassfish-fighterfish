/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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
 *
 * @author sanjeeb.sahoo@oracle.com
 */
public class Habitat {

    private final GlassFish gf;

    Habitat(GlassFish gf) {
        this.gf = gf;
    }

    public <T> T getComponent(Class<T> type) {
        try {
            return gf.getService(type);
        } catch (GlassFishException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
    }
}
