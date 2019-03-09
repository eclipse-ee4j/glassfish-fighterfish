/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app18;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import javax.enterprise.inject.Produces;

/**
 * A simple producer bean which allows us to inject BundleContext of this
 * bundle.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class BundleContextProducerBean {

    /**
     * Produce the bundle context bean.
     * @return BundleContext
     */
    @Produces
    public BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        System.out.println("BundleContextProducerBean.getBundleContext() "
                + bundle);
        BundleContext bundleContext = bundle.getBundleContext();
        System.out.println("BundleContextProducerBean.getBundleContext() "
                + bundleContext);
        return bundleContext;
    }
}
