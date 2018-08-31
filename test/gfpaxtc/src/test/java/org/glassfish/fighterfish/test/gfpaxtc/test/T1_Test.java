/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.gfpaxtc.test;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.BundleException;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class T1_Test {

    @Configuration
    public Option[] configure() {
        return options(
                junitBundles()
        );
    }

    @Inject
    GlassFish gf;

    @Test
    public void foo() throws InterruptedException, BundleException, IOException, GlassFishException {
        System.out.println("Foo: Hello World - I am inside GlassFish");
        assertNotNull(gf);
        assertEquals("GF Started", GlassFish.Status.STARTED, gf.getStatus());
    }

    @Test
    public void bar() throws GlassFishException {
        System.out.println("Bar: Hello World - I am also inside GlassFish");
        assertNotNull(gf);
        CommandResult result = gf.getCommandRunner().run("list-components");
        System.out.println(result.getOutput());
        assertEquals(CommandResult.ExitStatus.SUCCESS, result.getExitStatus());
    }
}
