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

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.ExamConfigurationException;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.ExceptionHelper;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.DefaultExamReactor;
import org.ops4j.pax.exam.spi.ExamReactor;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactorFactory;
import org.ops4j.pax.exam.spi.reactors.ReactorManager;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

/**
 * Custom JUnit runner for fighterfish based applications.
 */
public final class FighterFishJUnitRunner extends BlockJUnit4ClassRunner {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            FighterFishJUnitRunner.class.getPackage().getName());

    /**
     * PAX-EXAM reactor.
     */
    private final StagedExamReactor mReactor;

    /**
     * Methods map.
     */
    private final Map<TestAddress, FrameworkMethod> mMap =
            new HashMap<TestAddress, FrameworkMethod>();

    /**
     * Tests map.
     */
    private final Map<FrameworkMethod, TestAddress> mChilds =
            new HashMap<FrameworkMethod, TestAddress>();

    /**
     * PAX-EXAM system.
     */
    private final ExamSystem mSystem;

    /**
     * PAX-EXAM reactor manager.
     */
    private final ReactorManager manager;

    /**
     * Create a new instance.
     * @param klass test class
     * @throws Exception if an error occurs
     */
    public FighterFishJUnitRunner(final Class<?> klass)
            throws Exception {
        super(klass);
        manager = ReactorManager.getInstance();
        mSystem = PaxExamRuntime.createTestSystem();
        mReactor = prepareReactor();
    }

    @Override
    public void run(final RunNotifier notifier) {
        Class<?> testClass = getTestClass().getJavaClass();
        try {
            manager.beforeClass(mReactor, testClass);
            super.run(notifier);
        } catch (Exception e) {
            throw new TestContainerException(
                    "Problem interacting with reactor.", e);
        } finally {
            manager.afterClass(mReactor, testClass);
        }
    }

    /**
     * Override to avoid running BeforeClass and AfterClass by the driver.
     * They shall only be run by the container.
     * @param notifier run notifier
     * @return Statement
     */
    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement = childrenInvoker(notifier);
        return statement;
    }

    /**
     * Override to avoid running Before, After and Rule methods by the driver.
     * They shall only be run by the container.
     * @param method test method
     * @return Statement
     */
    @Override
    protected Statement methodBlock(final FrameworkMethod method) {
        Object test;
        try {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        Statement statement = methodInvoker(method, test);
        return statement;
    }

    /**
     * We overwrite those with reactor content.
     * @return list of method
     */
    @Override
    protected List<FrameworkMethod> getChildren() {
        if (mChilds.isEmpty()) {
            fillChildren();
        }
        return Arrays.asList(mChilds.keySet().toArray(
                new FrameworkMethod[mChilds.size()]));
    }

    /**
     * Fill in children for the reactor.
     */
    private void fillChildren() {
        Set<TestAddress> targets = mReactor.getTargets();
        for (final TestAddress address : targets) {
            final FrameworkMethod frameworkMethod = mMap.get(address.root());

            // now, someone later may refer to that artificial FrameworkMethod.
            // We need to be able to tell the address.
            FrameworkMethod method = new FrameworkMethod(
                    frameworkMethod.getMethod()) {
                @Override
                public String getName() {
                    return frameworkMethod.getName() + ":" + address.caption();
                }

                @Override
                public boolean equals(final Object obj) {
                    return address.equals(obj);
                }

                @Override
                public int hashCode() {
                    return address.hashCode();
                }
            };

            mChilds.put(method, address);
        }
    }

    @Override
    protected void collectInitializationErrors(final List<Throwable> errors) {
        // do nothing
    }

    /**
     * Prepare the reactor.
     * @return StagedExamReactor
     * @throws Exception if an error occurs
     */
    private synchronized StagedExamReactor prepareReactor()
            throws Exception {

        Class testClass = getTestClass().getJavaClass();
        Object testClassInstance = testClass.newInstance();
        ExamReactor reactor = getReactor(testClass);
        addConfigurationsToReactor(reactor);
        addTestsToReactor(reactor, testClass, testClassInstance);
        return reactor.stage(getFactory(testClass));
    }

    /**
     * Add reactor configuration.
     * @param reactor the reactor to process
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     * @throws IOException if an error occurs
     */
    private void addConfigurationsToReactor(final ExamReactor reactor)
            throws IllegalAccessException, InvocationTargetException,
            IllegalArgumentException, IOException {

        reactor.addConfiguration(TestsConfiguration.getInstance()
                .getPaxExamConfiguration());
    }

    /**
     * Add tests to the reactor.
     * @param reactor the reactor to process
     * @param testClass the test class
     * @param testClassInstance the test instance
     * @throws IOException if an error occurs
     * @throws ExamConfigurationException if an error occurs
     */
    private void addTestsToReactor(final ExamReactor reactor,
            final Class testClass, final Object testClassInstance)
            throws IOException, ExamConfigurationException {

        TestProbeBuilder probe = mSystem.createProbe();
        probe = overwriteWithUserDefinition(testClass, testClassInstance,
                probe);

        for (FrameworkMethod s : super.getChildren()) {
            // record the method -> adress matching
            TestAddress address = delegateTest(testClassInstance, probe, s);
            if (address == null) {
                address = probe.addTest(testClass, s.getMethod().getName());
            }
            mMap.put(address, s);
        }
        reactor.addProbe(probe);
    }

    /**
     * Delegate the test invocation.
     * @param testClassInstance test instance
     * @param probe PAX-EXAM probe
     * @param method test method
     * @return TestAddress
     */
    private TestAddress delegateTest(final Object testClassInstance,
            final TestProbeBuilder probe, final FrameworkMethod method) {

        try {
            Class<?>[] types = method.getMethod().getParameterTypes();
            if (types.length == 1
                    && types[0].isAssignableFrom(TestProbeBuilder.class)) {
                // do some backtracking:
                return (TestAddress) method.getMethod()
                        .invoke(testClassInstance, probe);

            } else {
                return null;
            }
        } catch (Exception e) {
            throw new TestContainerException("Problem delegating to test.",
                    e);
        }
    }

    /**
     * Get the PAX-EXAM reactor factory.
     * @param testClass test class
     * @return StagedExamReactorFactory
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private StagedExamReactorFactory getFactory(final Class testClass)
            throws InstantiationException, IllegalAccessException {
        ExamReactorStrategy strategy = (ExamReactorStrategy) testClass
                .getAnnotation(ExamReactorStrategy.class);

        StagedExamReactorFactory fact;
        if (strategy != null) {
            fact = strategy.value()[0].newInstance();
        } else {
            // default:
            fact = new PerClass();
        }
        return fact;
    }

    /**
     * Get the PAX-EXAM reactor.
     * @param testClass test class
     * @return DefaultExamReactor
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     */
    private DefaultExamReactor getReactor(final Class testClass)
            throws InstantiationException, IllegalAccessException {
        return new DefaultExamReactor(mSystem, getExamFactory(testClass));
    }

    /**
     * Get the PAX-EXAM factory.
     * @param testClass test class
     * @return TestContainerFactory
     * @throws IllegalAccessException if an error occurs
     * @throws InstantiationException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private TestContainerFactory getExamFactory(final Class testClass)
            throws IllegalAccessException, InstantiationException {
        ExamFactory f = (ExamFactory) testClass.getAnnotation(
                ExamFactory.class);

        TestContainerFactory fact;
        if (f != null) {
            fact = f.value().newInstance();
        } else {
            // default:
            fact = PaxExamRuntime.getTestContainerFactory();
        }
        return fact;
    }

    @Override
    protected synchronized Statement methodInvoker(
            final FrameworkMethod method, final Object test) {

        return new Statement() {

            @Override
            public void evaluate()
                    throws Throwable {
                TestAddress address = mChilds.get(method);
                TestAddress root = address.root();

                LOGGER.log(Level.FINE, "Invoke {0} @ {1} Arguments: {2}",
                        new Object[]{method.getName(), address,
                            Arrays.toString(root.arguments())});
                try {
                    mReactor.invoke(address);
                } catch (Exception e) {
                    Throwable t = ExceptionHelper.unwind(e);
                    LOGGER.log(Level.SEVERE, "Exception", e);
                    fail(t.getMessage());
                }
            }
        };

    }

    @Override
    protected void validatePublicVoidNoArgMethods(
            final Class<? extends Annotation> annotation,
            final boolean isStatic, final List<Throwable> errors) {
    }

    /**
     * Override a test probe for a given test.
     * @param testClass test class
     * @param instance test instance
     * @param probe new probe
     * @return TestProbeBuilder
     * @throws ExamConfigurationException if an error occurs
     */
    private TestProbeBuilder overwriteWithUserDefinition(final Class testClass,
            final Object instance, final TestProbeBuilder probe)
            throws ExamConfigurationException {

        Method[] methods = testClass.getMethods();
        for (Method m : methods) {
            ProbeBuilder conf = m.getAnnotation(ProbeBuilder.class);
            if (conf != null) {
                // consider as option, so prepare that one:
                LOGGER.log(Level.FINE, "User defined probe hook found: {0}",
                        m.getName());
                TestProbeBuilder probeBuilder;
                try {
                    probeBuilder = (TestProbeBuilder) m.invoke(instance, probe);
                } catch (Exception e) {
                    throw new ExamConfigurationException(
                            "Invoking custom probe hook " + m.getName()
                                    + " failed", e);
                }
                if (probeBuilder != null) {
                    return probe;
                } else {
                    throw new ExamConfigurationException(
                            "Invoking custom probe hook " + m.getName()
                                    + " succeeded but returned null");
                }
            }
        }
        LOGGER.fine("No User defined probe hook found");
        return probe;
    }
}
