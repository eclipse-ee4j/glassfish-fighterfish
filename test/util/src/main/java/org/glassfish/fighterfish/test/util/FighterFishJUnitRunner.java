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

package org.glassfish.fighterfish.test.util;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.*;
import org.ops4j.pax.exam.junit.ExamFactory;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.spi.*;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class FighterFishJUnitRunner extends BlockJUnit4ClassRunner {

    private Logger LOG = Logger.getLogger(getClass().getPackage().getName());

    final private StagedExamReactor m_reactor;
    final private Map<TestAddress, FrameworkMethod> m_map = new HashMap<TestAddress, FrameworkMethod>();
    final private Map<FrameworkMethod, TestAddress> m__childs = new HashMap<FrameworkMethod, TestAddress>();

    private ExamSystem m_system;

    public FighterFishJUnitRunner(Class<?> klass)
            throws Exception {
        super(klass);

        m_reactor = prepareReactor();
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            super.run(notifier);
        } catch (Exception e) {
            throw new TestContainerException("Problem interacting with reactor.", e);
        } finally {
            m_reactor.tearDown();
            m_system.clear();
        }
    }

    /**
     * Override to avoid running BeforeClass and AfterClass by the driver.
     * They shall only be run by the container.
     */
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement= childrenInvoker(notifier);
        return statement;
    }

    /**
     * Override to avoid running Before, After and Rule methods by the driver.
     * They shall only be run by the container.
     */
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
     */
    @Override
    protected List<FrameworkMethod> getChildren() {
        if (m__childs.isEmpty()) {
            fillChildren();
        }
        return Arrays.asList(m__childs.keySet().toArray(new FrameworkMethod[m__childs.size()]));
    }

    private void fillChildren() {
        Set<TestAddress> targets = m_reactor.getTargets();
        for (final TestAddress address : targets) {
            final FrameworkMethod frameworkMethod = m_map.get(address.root());

            // now, someone later may refer to that artificial FrameworkMethod. We need to be able to tell the address.
            FrameworkMethod method = new FrameworkMethod(frameworkMethod.getMethod()) {
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
        m_system = PaxExamRuntime.createTestSystem();
        Class testClass = getTestClass().getJavaClass();
        Object testClassInstance = testClass.newInstance();
        ExamReactor reactor = getReactor(testClass);

        addConfigurationsToReactor(reactor, testClass, testClassInstance);
        addTestsToReactor(reactor, testClass, testClassInstance);
        return reactor.stage(getFactory(testClass));
    }

    private void addConfigurationsToReactor(ExamReactor reactor, Class testClass, Object testClassInstance)
            throws IllegalAccessException, InvocationTargetException, IllegalArgumentException, IOException {
        reactor.addConfiguration(TestsConfiguration.getInstance().getPaxExamConfiguration());
    }

    private void addTestsToReactor(ExamReactor reactor, Class testClass, Object testClassInstance)
            throws IOException, ExamConfigurationException {
        TestProbeBuilder probe = m_system.createProbe();
        probe = overwriteWithUserDefinition(testClass, testClassInstance, probe);

        //probe.setAnchor( testClass );
        for (FrameworkMethod s : super.getChildren()) {
            // record the method -> adress matching
            TestAddress address = delegateTest(testClassInstance, probe, s);
            if (address == null) {
                address = probe.addTest(testClass, s.getMethod().getName());
            }
            m_map.put(address, s);
        }
        reactor.addProbe(probe.build());
    }

    private TestAddress delegateTest(Object testClassInstance, TestProbeBuilder probe, FrameworkMethod s) {
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
        ExamReactorStrategy strategy = (ExamReactorStrategy) testClass.getAnnotation(ExamReactorStrategy.class);

        StagedExamReactorFactory fact;
        if (strategy != null) {
            fact = strategy.value()[0].newInstance();
        } else {
            // default:
            fact = new EagerSingleStagedReactorFactory();
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
        ExamFactory f = (ExamFactory) testClass.getAnnotation(ExamFactory.class);

        TestContainerFactory fact;
        if (f != null) {
            fact = f.value().newInstance();
        } else {
            // default:
            fact = PaxExamRuntime.getTestContainerFactory();
        }
        return fact;
    }

    protected synchronized Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new Statement() {

            @Override
            public void evaluate()
                    throws Throwable {
                TestAddress address = m__childs.get(method);
                TestAddress root = address.root();

                LOG.fine("Invoke " + method.getName() + " @ " + address + " Arguments: " + root.arguments());
                try {
                    m_reactor.invoke(address);
                } catch (Exception e) {
                    Throwable t = ExceptionHelper.unwind(e);
                    LOG.log(Level.SEVERE, "Exception", e);
                    fail(t.getMessage());
                }
            }
        };

    }

    @Override
    protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {

    }

    private TestProbeBuilder overwriteWithUserDefinition(Class testClass, Object instance, TestProbeBuilder probe)
            throws ExamConfigurationException {
        Method[] methods = testClass.getMethods();
        for (Method m : methods) {
            ProbeBuilder conf = m.getAnnotation(ProbeBuilder.class);
            if (conf != null) {
                // consider as option, so prepare that one:
                LOG.fine("User defined probe hook found: " + m.getName());
                TestProbeBuilder probeBuilder;
                try {
                    probeBuilder = (TestProbeBuilder) m.invoke(instance, probe);
                } catch (Exception e) {
                    throw new ExamConfigurationException("Invoking custom probe hook " + m.getName() + " failed", e);
                }
                if (probeBuilder != null) {
                    return probe;
                } else {
                    throw new ExamConfigurationException("Invoking custom probe hook " + m.getName() + " succeeded but returned null");
                }

            }
        }
        LOG.fine("No User defined probe hook found");
        return probe;
    }
}
