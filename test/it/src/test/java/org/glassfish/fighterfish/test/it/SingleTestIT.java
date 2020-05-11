/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.it;

import static java.util.logging.Level.INFO;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.fighterfish.test.util.AbstractTestObject;
import org.glassfish.fighterfish.test.util.TestContext;
import org.junit.Test;
import org.osgi.framework.BundleException;

/**
 * This class demonstrates how simple it is to write a test scenario using pax-exam and the simple abstractions provided
 * by our test.util bundle.
 */
public class SingleTestIT extends AbstractTestObject {

    private static final Logger LOGGER = Logger.getLogger(SingleTestIT.class.getPackage().getName());

    /**
     * Most simple test case. This test method waits a pre-configured amount of time for GlassFish to be started. If
     * GlassFish does not start within that time, it fails.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws IOException
     */
    @Test
    public void test() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, "SingleTest", "test", "ENTRY");
        
        try (TestContext testContext = TestContext.create(getClass())) {
            GlassFish glassFish = testContext.getGlassFish();
            assertNotNull(glassFish);
        } 
    }
}
