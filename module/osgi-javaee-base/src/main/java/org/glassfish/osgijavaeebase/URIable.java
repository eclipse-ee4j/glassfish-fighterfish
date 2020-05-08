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

import java.net.URI;

/**
 * A possibly nested archive that provides URI entries.
 */
public interface URIable {

    /**
     * Get an entry by name.
     *
     * @param name the entry name relative to the root of the archive
     * @return the URI corresponding to the entry, null if no such entry found
     */
    URI getEntryURI(String name);

    /**
     * When this archive is embedded in another archive, this method returns the distance from top.
     *
     * @return a string indicating the path to the root of the top level archive, or an empty string if not nested.
     */
    String getDistanceFromTop();
}
