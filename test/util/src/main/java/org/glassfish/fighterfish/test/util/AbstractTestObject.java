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
package org.glassfish.fighterfish.test.util;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import jakarta.inject.Inject;
import java.io.IOException;

/**
 * PAX-EXAM requires each test class to be annotated with {@link RunWith}
 * annotation with a value of {@link JUnit4TestRunner}. We have also set
 * {@link ExamReactorStrategy} as {@link PerClass} which means for every test
 * method invocation, a new test container instance won't be created. Pax-Exam
 * will create a new test container instance for each TestClass and reuse it for
 * every test method found in that class. Each test can optionally configure the
 * test container by having one or more configuration methods. Each such method
 * must be annotated with {@link Configuration} and return an array of
 * {@link Option}. The options returned by such a method is used to configure
 * the OSGi framework that's going to be launched by PAX-EXAM. If a test has
 * more than one such methods, then pax-exam will create multiple test container
 * and run the test in each such container.
 *
 * Most of our tests require the OSGi platform to be configured similarly, so we
 * provide this base class as a convenience for our tests. In a lot of way, this
 * shields individual tests from pax-exam details. In addition to providing
 * pax-exam contracts, it also provides some helper methods which are needed in
 * every test. It is not mandatory for tests to extend this class.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractTestObject {

    /**
     * Bundle context.
     */
    @Inject
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected BundleContext ctx;

    /**
     * PaxExamJunit driver treats methods in JUnit Test class annotated with
     * {@code @Configuration} specially. For each such method, it creates a
     * separate test container configuring it with the options as returned by
     * the method.
     *
     * @return Options used to configure a test container
     * @throws IOException if an error occurs
     */
    @Configuration
    public Option[] getPaxExamConfiguration() throws IOException {
        return TestsConfiguration.getInstance().getPaxExamConfiguration();
    }

    /**
     * Get the configured timeout.
     * @return timeout
     */
    protected Long getTimeout() {
        return TestsConfiguration.getInstance().getTimeout();
    }
}
