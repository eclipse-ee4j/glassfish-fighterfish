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

import java.util.ArrayList;
import java.util.List;

/**
 * Annotation descriptor.
 */
public final class HK2AnnotationDescriptor {

    /**
     * Target bundle symbolic name.
     */
    private String targetBundleSymbolicName;

    /**
     * Target bundle version.
     */
    private String targetBundleVersion;

    /**
     * Injection field metadatas.
     */
    private final List<HK2InjectMetadata> injectionFieldMetaDatas =
            new ArrayList<HK2InjectMetadata>();

    /**
     * Contract class names.
     */
    private final List<String> contractClassNames = new ArrayList<String>();

    /**
     * Create a new instance.
     * @param bsn bundle symbolic name
     * @param bv bundle version
     */
    public HK2AnnotationDescriptor(final String bsn, final String bv) {
        this.targetBundleSymbolicName = bsn;
        this.targetBundleVersion = bv;
    }

    /**
     * Get the target bundle symbolic name.
     * @return String
     */
    public String getTargetBundleSymbolicName() {
        return targetBundleSymbolicName;
    }

    /**
     * Set the target bundle symbolic name.
     * @param targetBsn new target bundle symbolic name
     */
    public void setTargetBundleSymbolicName(final String targetBsn) {
        this.targetBundleSymbolicName = targetBsn;
    }

    /**
     * Get the target bundle version.
     * @return String
     */
    public String getTargetBundleVersion() {
        return targetBundleVersion;
    }

    /**
     * Set the target bundle version.
     * @param targetBv new target bundle version
     */
    public void setTargetBundleVersion(final String targetBv) {
        this.targetBundleVersion = targetBv;
    }

    /**
     * Get the injection field metadatas.
     * @return list of HK2InjectMetadata
     */
    public List<HK2InjectMetadata> getInjectionFieldMetaDatas() {
        return injectionFieldMetaDatas;
    }

    /**
     * Get the contract class names.
     * @return list of class names
     */
    public List<String> getContractClassNames() {
        return contractClassNames;
    }
}
