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
 * Represents a resource in the bundle's class-path.
 */
public final class BundleResource {

    /**
     * URI of the resource.
     */
    private final URI uri;

    /**
     * Path of the resource. This is with respect to bundle class path namespace.
     */
    private final String path;

    /**
     * Path of the sub archive from the top of the bundle containing this resource. "" if it is in the bundle directly.
     */
    private final String archivePath;

    /**
     * Create a new instance.
     *
     * @param resourceUri resource URI
     * @param resourcePath resource path
     * @param subArchivePath sub archive path from the top
     */
    BundleResource(final URI resourceUri, final String resourcePath, final String subArchivePath) {

        this.uri = resourceUri;
        this.path = resourcePath;
        this.archivePath = subArchivePath;
    }

    /**
     * Get the resource URI.
     *
     * @return URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Get the resource path.
     *
     * @return resource path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the sub-archive path.
     *
     * @return path from the top level archive, "" if it is in the bundle directly.
     */
    public String getArchivePath() {
        return archivePath;
    }
}
