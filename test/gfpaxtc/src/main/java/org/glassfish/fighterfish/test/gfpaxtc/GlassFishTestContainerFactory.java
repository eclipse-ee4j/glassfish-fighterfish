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

package org.glassfish.fighterfish.test.gfpaxtc;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.options.SystemPropertyOption;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class GlassFishTestContainerFactory implements TestContainerFactory {
    static {
        // Work around for GLASSFISH-16510.
        // This code gets executes before any test methods get executed, which means this code
        // gets executed before any embedded glassfish gets provisioned. By eagely calling, getPlatformMBeanServer,
        // we ensure that all embedded glassfish will use this as opposed to what is created by
        // AppServerMBeanServerBuilder.
        java.lang.management.ManagementFactory.getPlatformMBeanServer();
    }

    public GlassFishTestContainerFactory() throws MalformedURLException, GlassFishException {
    }

    @Override
    public TestContainer[] create(ExamSystem system) throws TestContainerException {
        return new TestContainer[]{createTestContainer(system)}; // we can only return one container
    }

    private TestContainer createTestContainer(ExamSystem system) {
        return new GlassFishTestContainer(system);
    }

}
