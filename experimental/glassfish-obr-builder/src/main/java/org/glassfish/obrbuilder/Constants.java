/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * Most of the class is based on org.jvnet.hk2.osgiadapter.Constants which is written by
 * Sahoo, and I only modify some fields's naming in order to enhance reading of glassfish-obr-builder
 * In addition, I delete "OBR_REPOSITORIES" because I think that currently this field is not used by any scene.
 * 
 * @author Sanjeeb.Sahoo@Sun.COM
 * @author TangYong(tangyong@cn.fujitsu.com)
 */
public final class Constants {
    /**
     * Indicates if OBR is enabled or not.
     */
    public static final String OBR_ENABLED = "com.sun.enterprise.hk2.obrEnabled";
    /**
     * This property is used to decide if OBR repository should be synchronously initialized.
     */
    static final String INITIALIZE_OBR_SYNCHRONOUSLY = "com.sun.enterprise.hk2.initializeRepoSynchronously";

    /**
     * File name prefix used to store generated OBR repository information.
     * This will be suffixed with repository directory name.
     * The file extension will depend on whether we store a binary file or an xml file.
     * For binary file, no extension will be used. For xml file, .xml will be used as extension.
     */
    static final String OBR_FILE_NAME_PREFIX = "obr-";

    /**
     * URL scheme used by OBR to deploy bundles.
     */
    static final String OBR_SCHEME = "obr:";

    /**
     * No. of milliseconds a thread waits for obtaining a reference to repository admin service before timing out.
     */
    static final long OBR_TIMEOUT = 10000; // in ms

    /**
     * List of HK2 module repository URIs. Currently, we only support directory URIs.
     * (TangYong modified naming)
     */
    public static final String GF_MODULE_REPOSITORIES = "com.sun.enterprise.hk2.repositories";

    /**
     * This boolean flag is used to indicate if OBR deploys fragment bundles for any given required bundle.
     * Since fragments are not required resources of a bundle, it requires two pass resolution.
     * Default is false.
     */
    public static final String OBR_DEPLOYS_FRGAMENTS = "com.sun.enterprise.hk2.obrDeploysFragments";

    /**
     * This boolean flag is used to indicate if OBR deploys optional requirements.
     * Default is false.
     */
    public static final String OBR_DEPLOYS_OPTIONAL_REQUIREMENTS = "com.sun.enterprise.hk2.obrDeploysOptionalRequirements";

    static final String HK2_CACHE_DIR = "com.sun.enterprise.hk2.cacheDir";
    static final String INHABITANTS_CACHE = "inhabitants";
    static final String HK2_CACHE_IO_BUFFER_SIZE = "com.sun.enterprise.hk2.cacheIoBufferSize";
    static final int DEFAULT_BUFFER_SIZE = 1024;
    
    /**
     * Glassfish System OBR File Name.
     */
    static final String GF_SYSTEM_OBR_NAME = "obr-modules.xml";
    
    /**
     * OBR Test Repo.
     */
    static final String OBR_TEST_REPO= "provisioning";
}
