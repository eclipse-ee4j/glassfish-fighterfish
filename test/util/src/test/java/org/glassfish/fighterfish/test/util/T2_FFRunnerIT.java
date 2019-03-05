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
package org.glassfish.fighterfish.test.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

/**
 * @author sanjeeb.sahoo@oracle.com
 */
@RunWith(FighterFishJUnitRunner.class)
public class T2_FFRunnerIT {

    @Inject
    private BundleContext ctx;

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(ctx);
        TestContext tc = TestContext.create(getClass());
        try {
            Assert.assertSame(tc.getBundleContext(), ctx);
            System.out.println("tc.getBundleContext() = "
                    + tc.getBundleContext());
            System.out.println(tc.getGlassFish());
        } finally {
            tc.destroy();
        }
    }

    @Test
    public void test2() throws Exception {
        TestContext tc = TestContext.create(getClass());
        try {
            Assert.assertSame(tc.getBundleContext(), ctx);
            System.out.println("tc.getBundleContext() = "
                    + tc.getBundleContext());
            System.out.println(tc.getGlassFish());
        } finally {
            tc.destroy();
        }
    }
}
