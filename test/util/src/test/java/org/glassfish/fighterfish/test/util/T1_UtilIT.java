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
package org.glassfish.fighterfish.test.util;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleException;

/**
 * This is an integration test for this module. The class name is suffixed with
 * IT so that it gets picked up by failsafe plugin only.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@RunWith(FighterFishJUnitRunner.class)
public class T1_UtilIT {

    @Test
    public void test() throws GlassFishException, InterruptedException,
            BundleException {

        TestContext tc = TestContext.create(getClass());
        try {
            GlassFish gf = tc.getGlassFish();
            CommandRunner cr = gf.getCommandRunner();
            cr.setTerse(true);
            CommandResult result = cr.run("version");
            System.out.println(result.getOutput());
            Assert.assertTrue(result.getExitStatus() ==
                    CommandResult.ExitStatus.SUCCESS);
        } finally {
            tc.destroy();
        }
    }
}
