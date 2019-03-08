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
package org.glassfish.osgijavaeebase;

import java.net.URI;

/**
 * Represents a resource in the bundle's class-path.
 */
public class BundleResource {

    // uri of the resource
    private final URI uri;
    // path of the resource. this is with respect to bundle class path
    // namespace
    private final String path;
    // path of the sub archive from the top of the bundle containing this
    // resource. "" if it is in the bundle directly.
    private final String archivePath;

    BundleResource(URI uri, String path, String archivePath) {
        this.uri = uri;
        this.path = path;
        this.archivePath = archivePath;
    }

    public URI getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public String getArchivePath() {
        return archivePath;
    }
}
