/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.testing.failsafe;

import jakarta.inject.Inject;

import org.glassfish.fighterfish.test.util.FighterFishJUnitRunner;
import org.glassfish.fighterfish.test.util.TestContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

/**
 * This sample test demonstrates use of a custom JUnit test runner called {@link FighterFishJUnitRunner} to execute a
 * JUnit test using maven surefire plugin. The test name is suffixed with IT so as to be automatically included by maven
 * failsafe plugin.
 *
 * The custom runner has the ability to provision GlassFish, which includes downloading of the GlassFish bundles,
 * installing the smae and bootstrapping GlassFish inside or outside the current JVM. All these steps are pretty
 * configurable via various configuration options specified as system properties. See the pom.xml to see various
 * configuration options. If you chose to control those options from code, then you can provide in a method in test
 * class annotated with @Configuration.
 */
@RunWith(FighterFishJUnitRunner.class)
public class FighterFishJUnitRunnerIT {

    /**
     * This is how one can inject BundleContext. In fact, one can even inject provisioned services.
     */
    @Inject
    private BundleContext ctx;

    @Test
    public void test() throws Exception {
        System.out.println("FighterFishJUnitRunnerIT.test()");
        TestContext tc = TestContext.create(getClass());
        try {
            Assert.assertSame(ctx, tc.getBundleContext());
            System.out.println("tc.getBundleContext() = " + tc.getBundleContext());
            System.out.println(tc.getGlassFish());
        } finally {
            tc.destroy();
        }
    }

    @Test
    public void test2() throws Exception {
        System.out.println("FighterFishJUnitRunnerIT.test2()");
        TestContext tc = TestContext.create(getClass());
        try {
            System.out.println("tc.getBundleContext() = " + tc.getBundleContext());
            System.out.println(tc.getGlassFish());
        } finally {
            tc.destroy();
        }
    }
}
