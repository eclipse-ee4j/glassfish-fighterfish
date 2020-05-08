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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.sun.faces.spi.ConfigurationResourceProvider;
import com.sun.faces.spi.FaceletConfigResourceProvider;

/**
 * Built-in provider can't discover resources named as xxx.taglib.xml. This config resource provider knows how to
 * iterate over bundle entries in order to discover the resources. It is registered as a META-INF service so that
 * mojarra can discover it.
 *
 * @see org.glassfish.osgiweb.OSGiWebModuleDecorator#discoverJSFConfigs
 */
public final class OSGiFaceletConfigResourceProvider implements FaceletConfigResourceProvider, ConfigurationResourceProvider {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiFaceletConfigResourceProvider.class.getPackage().getName());

    @Override
    @SuppressWarnings("unchecked")
    public Collection<URI> getResources(final ServletContext context) {

        Collection<URI> uris = (Collection<URI>) context.getAttribute(Constants.FACELET_CONFIG_ATTR);
        if (uris == null) {
            return Collections.EMPTY_LIST;
        }
        // TODO(Sahoo): change to debug level statement
        LOGGER.log(Level.INFO, "Facelet Config uris = {0}", uris);
        return uris;
    }
}
