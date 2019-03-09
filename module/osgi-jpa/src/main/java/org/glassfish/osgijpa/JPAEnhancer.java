/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijpa;

import org.osgi.framework.Bundle;
import org.glassfish.osgijpa.dd.Persistence;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;

/**
 * An enhancer is used to statically enhance the classes of a bundle and produce
 * a new JarInputStream which can then be used to update the supplied bundle.
 */
interface JPAEnhancer {

    /**
     * Enhance the given bundles. An enhancer may have to explode the bundle in
     * a directory so that it can scan the contents of the bundle using File or
     * Jar APIs. If so, it is the responsibility of the enhancer to delete such
     * temporary locations.
     *
     * @param bnd Bundle to be enhanced
     * @param persistenceXMLs objects corresponding to persistence.xmls present
     * in the bundle
     * @return a JarInputStream which contains the enhanced classes along with
     * changed manifest, or {@code null} if no enhancement is needed
     * @throws IOException if an error occurs
     */
    InputStream enhance(Bundle bnd, List<Persistence> persistenceXMLs)
            throws IOException;
}
