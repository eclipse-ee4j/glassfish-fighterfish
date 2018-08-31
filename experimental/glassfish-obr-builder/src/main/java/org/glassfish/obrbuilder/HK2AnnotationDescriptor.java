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

import java.util.ArrayList;
import java.util.List;

/**
 * @author TangYong(tangyong@cn.fujitsu.com)
 */
public class HK2AnnotationDescriptor {
	private String targetBundleSymbolicName;
	private String targetBundleVersion;

	private List<HK2InjectMetadata> injectionFieldMetaDatas = new ArrayList<HK2InjectMetadata>();
	private List<String> contractClassNames = new ArrayList<String>();

	public HK2AnnotationDescriptor(String bundleSymbolicName, String bundleVersion){
		this.targetBundleSymbolicName = bundleSymbolicName;
		this.targetBundleVersion = bundleVersion;
	}
	
	public String getTargetBundleSymbolicName() {
		return targetBundleSymbolicName;
	}

	public void setTargetBundleSymbolicName(String targetBundleSymbolicName) {
		this.targetBundleSymbolicName = targetBundleSymbolicName;
	}

	public String getTargetBundleVersion() {
		return targetBundleVersion;
	}

	public void setTargetBundleVersion(String targetBundleVersion) {
		this.targetBundleVersion = targetBundleVersion;
	}

	public List<HK2InjectMetadata> getInjectionFieldMetaDatas() {
		return injectionFieldMetaDatas;
	}
	
	public List<String> getContractClassNames() {
		return contractClassNames;
	}

}
