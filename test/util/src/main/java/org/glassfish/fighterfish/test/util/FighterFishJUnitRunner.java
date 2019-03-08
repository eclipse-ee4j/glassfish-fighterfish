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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

/**
 * See {@link org.ops4j.pax.exam.junit.impl.ProbeRunner}.
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class FighterFishJUnitRunner extends BlockJUnit4ClassRunner {

    private static final Logger LOGGER = Logger.getLogger(
            FighterFishJUnitRunner.class.getPackage().getName());

    private final StagedExamReactor m_reactor;
    private final Map<TestAddress, FrameworkMethod> m_map =
            new HashMap<TestAddress, FrameworkMethod>();
    private final Map<FrameworkMethod, TestAddress> m__childs =
            new HashMap<FrameworkMethod, TestAddress>();

    private final ExamSystem m_system;
    private final ReactorManager manager;

    public FighterFishJUnitRunner(Class<?> klass)
            throws Exception {
        super(klass);
        manager = ReactorManager.getInstance();
        m_system = PaxExamRuntime.createTestSystem();
        m_reactor = prepareReactor();
    }

    @Override
    public void run(RunNotifier notifier) {
        Class<?> testClass = getTestClass().getJavaClass();
        try {
            manager.beforeClass(m_reactor, testClass);
            super.run(notifier);
        } catch (Exception e) {
            throw new TestContainerException(
                    "Problem interacting with reactor.", e);
        } finally {
            manager.afterClass(m_reactor, testClass);
        }
    }

    /**
     * Override to avoid running BeforeClass and AfterClass by the driver.
     * They shall only be run by the container.
     * @param notifier
     * @return 
     */
    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement= childrenInvoker(notifier);
        return statement;
    }

    /**
     * Override to avoid running Before, After and Rule methods by the driver.
     * They shall only be run by the container.
     * @param method
     * @return 
     */
    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        Object test;
        try {
            test= new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        Statement statement= methodInvoker(method, test);
        return statement;
    }


    /**
     * We overwrite those with reactor content
     * @return 
     */
    @Override
    protected List<FrameworkMethod> getChildren() {
        if (m__childs.isEmpty()) {
            fillChildren();
        }
        return Arrays.asList(m__childs.keySet().toArray(
                new FrameworkMethod[m__childs.size()]));
    }

    private void fillChildren() {
        Set<TestAddress> targets = m_reactor.getTargets();
        for (final TestAddress address : targets) {
            final FrameworkMethod frameworkMethod = m_map.get(address.root());

            // now, someone later may refer to that artificial FrameworkMethod.
            // We need to be able to tell the address.
            FrameworkMethod method = new FrameworkMethod(
                    frameworkMethod.getMethod()) {
                @Override
                public String getName() {
                    return frameworkMethod.getName() + ":" + address.caption();
                }

                @Override
                public boolean equals(Object obj) {
                    return address.equals(obj);
                }

                @Override
                public int hashCode() {
                    return address.hashCode();
                }
            };

            m__childs.put(method, address);
        }
    }

    @Override
    protected void collectInitializationErrors
            (List<Throwable> errors) {
        // do nothing
    }

    private synchronized StagedExamReactor prepareReactor()
            throws Exception {

        Class testClass = getTestClass().getJavaClass();
        Object testClassInstance = testClass.newInstance();
        ExamReactor reactor = getReactor(testClass);
        addConfigurationsToReactor(reactor);
        addTestsToReactor(reactor, testClass, testClassInstance);
        return reactor.stage(getFactory(testClass));
    }

    private void addConfigurationsToReactor(ExamReactor reactor)
            throws IllegalAccessException, InvocationTargetException,
            IllegalArgumentException, IOException {

        reactor.addConfiguration(TestsConfiguration.getInstance()
                .getPaxExamConfiguration());
    }

    private void addTestsToReactor(ExamReactor reactor, Class testClass,
            Object testClassInstance)
            throws IOException, ExamConfigurationException {

        TestProbeBuilder probe = m_system.createProbe();
        probe = overwriteWithUserDefinition(testClass, testClassInstance,
                probe);

        for (FrameworkMethod s : super.getChildren()) {
            // record the method -> adress matching
            TestAddress address = delegateTest(testClassInstance, probe, s);
            if (address == null) {
                address = probe.addTest(testClass, s.getMethod().getName());
            }
            m_map.put(address, s);
        }
        reactor.addProbe(probe);
    }

    private TestAddress delegateTest(Object testClassInstance,
            TestProbeBuilder probe, FrameworkMethod s) {

        try {
            Class<?>[] types = s.getMethod().getParameterTypes();
            if (types.length == 1 && types[0].isAssignableFrom(TestProbeBuilder.class)) {
                // do some backtracking:
                return (TestAddress) s.getMethod().invoke(testClassInstance, probe);

            } else {
                return null;
            }
        } catch (Exception e) {
            throw new TestContainerException("Problem delegating to test.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private StagedExamReactorFactory getFactory(Class testClass)
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

    private DefaultExamReactor getReactor(Class testClass)
            throws InstantiationException, IllegalAccessException {
        return new DefaultExamReactor(m_system, getExamFactory(testClass));
    }

    @SuppressWarnings("unchecked")
    private TestContainerFactory getExamFactory(Class testClass)
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
    protected synchronized Statement methodInvoker(final FrameworkMethod method,
            final Object test) {

        return new Statement() {

            @Override
            public void evaluate()
                    throws Throwable {
                TestAddress address = m__childs.get(method);
                TestAddress root = address.root();

                LOGGER.log(Level.FINE, "Invoke {0} @ {1} Arguments: {2}",
                        new Object[]{method.getName(), address,
                            Arrays.toString(root.arguments())});
                try {
                    m_reactor.invoke(address);
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
            Class<? extends Annotation> annotation, boolean isStatic,
            List<Throwable> errors) {
    }

    private TestProbeBuilder overwriteWithUserDefinition(Class testClass,
            Object instance, TestProbeBuilder probe)
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
