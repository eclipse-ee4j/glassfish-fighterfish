/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgicdi.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.util.AnnotationLiteral;

import org.osgi.framework.ServiceException;

import org.glassfish.osgicdi.OSGiService;
import org.glassfish.osgicdi.ServiceUnavailableException;

/**
 * A portable extension that supports discovery and injection of OSGi services
 * from an OSGi service registry into Beans/Java EE components that support
 * injection.
 *
 * @see OSGiService
 */
public final class OSGiServiceExtension implements Extension {

    /**
     * A map of Framework Service Types to be injected and additional metadata
     * about the OSGiService to be injected.
     */
    private final HashMap<Type, Set<InjectionPoint>> servicesToBeInjected
            = new HashMap<Type, Set<InjectionPoint>>();

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            OSGiServiceExtension.class.getPackage().getName());

    /**
     * Observers for container life-cycle events.
     * @param bdd before bean discovery event
     */
    void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery bdd) {
        debug("beforeBeanDiscovery" + bdd);
        bdd.addQualifier(OSGiService.class); //XXX:needed?
    }

    /**
     * Observer for <code>ProcessInjectionTarget</code> events. This event is
     * fired for every Java EE component class supporting injection that may be
     * instantiated by the container at runtime. Injections points of every
     * discovered enabled Java EE component is checked to see if there is a
     * request for injection of a framework service.
     * @param pb after process injection target event
     */
    void afterProcessInjectionTarget(
            @Observes final ProcessInjectionTarget<?> pb) {

        debug("AfterProcessInjectionTarget" + pb.getAnnotatedType()
                .getBaseType());
        Set<InjectionPoint> ips = pb.getInjectionTarget().getInjectionPoints();
        discoverServiceInjectionPoints(ips);
    }

    /**
     * Observer for <code>ProcessBean</code> events. This event is fired fire an
     * event for each enabled bean, interceptor or decorator deployed in a bean
     * archive, before registering the Bean object. Injections points of every
     * discovered enabled Java EE component is checked to see if there is a
     * request for injection of a framework service.
     * @param pb after process bean event
     */
    @SuppressWarnings("unchecked")
    void afterProcessBean(@Observes final ProcessBean pb) {
        debug("afterProcessBean - " + pb.getAnnotated().getBaseType());
        Set<InjectionPoint> ips = pb.getBean().getInjectionPoints();
        discoverServiceInjectionPoints(ips);
    }

    /**
     * Discover injection points where the framework service is requested
     * through the <code>OSGiService</code> qualifier and a map is
     * populated for all framework services that have been requested.
     * @param ips set of injection points
     */
    private void discoverServiceInjectionPoints(
            final Set<InjectionPoint> ips) {

        for (Iterator<InjectionPoint> iterator = ips.iterator();
                iterator.hasNext();) {
            InjectionPoint injectionPoint = iterator.next();
            Set<Annotation> qualifs = injectionPoint.getQualifiers();
            for (Iterator<Annotation> qualifIter = qualifs.iterator();
                    qualifIter.hasNext();) {
                Annotation annotation = qualifIter.next();
                if (annotation.annotationType().equals(OSGiService.class)) {
                    printDebugForInjectionPoint(injectionPoint);
                    String s = "---- Injection requested for "
                            + "framework service type " + injectionPoint
                                    .getType()
                            + " and annotated with dynamic="
                            + injectionPoint.getAnnotated()
                                    .getAnnotation(OSGiService.class)
                                    .dynamic()
                            + ", serviceCriteria="
                            + injectionPoint.getAnnotated()
                                    .getAnnotation(OSGiService.class)
                                    .serviceCriteria();
//                    LOGGER.logp(Level.INFO, "OSGiServiceExtension",
//                            "discoverServiceInjectionPoints", s);
                    debug(s);
                    //Keep track of service-type and its injection point
                    //Add to list of framework services to be injected
                    addServiceInjectionInfo(injectionPoint);
                    debug("number of injection points for "
                            + injectionPoint.getType() + "="
                            + servicesToBeInjected.size());

                }
            }
        }
    }

    /**
     * Add service injection information.
     * @param injectionPoint injection point
     */
    private void addServiceInjectionInfo(final InjectionPoint injectionPoint) {
        Type key = injectionPoint.getType();
        if (!servicesToBeInjected.containsKey(key)) {
            servicesToBeInjected.put(key,
                    new CopyOnWriteArraySet<InjectionPoint>());
        }
        servicesToBeInjected.get(key).add(injectionPoint);
    }

    /**
     * Observer for <code>AfterBeanDiscovery</code> events. This observer method
     * is used to register <code>Bean</code>s for the framework services that
     * have been requested to be injected.
     * @param abd after bean discovery event
     */
    void afterBeanDiscovery(@Observes final AfterBeanDiscovery abd) {
        debug("After Bean Discovery");
        for (Iterator<Type> iterator = this.servicesToBeInjected.keySet()
                .iterator(); iterator.hasNext();) {

            Type type = iterator.next();
            //If the injection point's type is not a Class or Interface, we
            //don't know how to handle this.
            if (!(type instanceof Class)) {
                //XXX: need to handle Instance<Class>. This fails currently
                LOGGER.logp(Level.WARNING, "OSGiServiceExtension",
                        "afterBeanDiscovery", "Unknown type: {0}",
                        new Object[]{type});
                abd.addDefinitionError(new UnsupportedOperationException(
                        "Injection target type " + type + "not supported"));
                break; //abort deployment
            }
            //Add the Bean representing the framework service so that it
            //is available for injection
            addBean(abd, type, this.servicesToBeInjected.get(type));
        }
    }

    /**
     * Add a <code>Bean</code> for the framework service requested. Instantiate
     * or discover the bean from the framework service registry, and return a
     * reference to the service if a dynamic reference is requested.
     * @param abd after bean discovery event
     * @param type bean type
     * @param injectionPoints set of injection points
     */
    private void addBean(final AfterBeanDiscovery abd, final Type type,
            final Set<InjectionPoint> injectionPoints) {

        List<OSGiService> registeredBeans = new ArrayList<OSGiService>();
        for (Iterator<InjectionPoint> iterator = injectionPoints.iterator();
                iterator.hasNext();) {

            final InjectionPoint svcInjectionPoint = iterator.next();
            if (!registeredBeans.contains(svcInjectionPoint.getAnnotated()
                    .getAnnotation(OSGiService.class))) {
                debug(" --- Adding an OSGi service BEAN "
                        + type + " for " + svcInjectionPoint);
                OSGiService os = svcInjectionPoint.getAnnotated()
                        .getAnnotation(OSGiService.class);
                if (!os.dynamic()) {
                    //If Static, check for existence of Service before going
                    //ahead and adding a Bean.
                    //If a service that matches the requirements specified
                    //is unavailable, fail deployment by throwing
                    //a <code>ServiceUnavailableException</code>
                    try {
                        OSGiServiceFactory.checkServiceAvailability(
                                svcInjectionPoint);
                    } catch (ServiceUnavailableException sue) {
                        sue.printStackTrace();
                        throw new ServiceUnavailableException(
                                "A static OSGi service reference was requested"
                                + " in " + svcInjectionPoint + ". However no "
                                + svcInjectionPoint.getType()
                                + " service available",
                                ServiceException.SUBCLASSED, sue);
                    }
                }
                abd.addBean(new OSGiServiceBean(svcInjectionPoint));
                registeredBeans.add(svcInjectionPoint.getAnnotated()
                        .getAnnotation(OSGiService.class));
            } else {
                debug(" --- NOT Adding an OSGi service BEAN "
                        + type + " for " + svcInjectionPoint
                        + "as there has already been one registered for"
                        + svcInjectionPoint.getAnnotated()
                                .getAnnotation(OSGiService.class));
            }
        }
    }


    /**
     * A <code>Bean</code> class that represents an OSGi Service.
     */
    private final class OSGiServiceBean implements Bean {

        /**
         * Bean type.
         */
        private final Type type;

        /**
         * Injection point for the OSGi service.
         */
        private final InjectionPoint svcInjectionPoint;

        /**
         * OSGi service instance.
         */
        private final OSGiService os;

        /**
         * Create a new instance.
         * @param injectionPoint injection point
         */
        OSGiServiceBean(final InjectionPoint injectionPoint) {
            this.svcInjectionPoint = injectionPoint;
            this.type = this.svcInjectionPoint.getType();
            this.os = this.svcInjectionPoint.getAnnotated()
                    .getAnnotation(OSGiService.class);
        }

        @Override
        public Object create(final CreationalContext ctx) {
            debug("create::" + type);
            //get the service from the service registry
            try {
                return OSGiServiceFactory.getService(this.svcInjectionPoint);
            } catch (ServiceUnavailableException e) {
                e.printStackTrace();
                throw new CreationException(e);
            }
        }

        @Override
        public void destroy(final Object instance,
                final CreationalContext creationalContext) {

            //debug("destroy::" + instance);
            //unget the service reference
            OSGiServiceFactory.ungetService(instance, this.svcInjectionPoint);
        }

        @Override
        public Class getBeanClass() {
            return (Class) type;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return type + getServiceMetadata();
        }

        /**
         * Get the string for the service metadata.
         * @return service metadata
         */
        private String getServiceMetadata() {
            return "_dynamic_" + os.dynamic()
                    + "_criteria_" + os.serviceCriteria()
                    + "_waitTimeout" + os.waitTimeout();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> s = new HashSet<Annotation>();
            s.add(new AnnotationLiteral<Default>() { });
            s.add(new AnnotationLiteral<Any>() { });
            //Add the appropriate parameters to the OSGiService qualifier
            //as requested in the injection point
            s.add(new OSGiServiceQualifierType(this.os));
            return s;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
            //Similar to Java EE comp resources made available as Dependent only
            //we now allow OSGi services as Dependent Beans only.
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> s = new HashSet<Type>();
            s.add(type);
            s.add(Object.class);
            return s;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return false;
        }
    }

    /**
     * Represents an annotation type instance of OSGiService
     * with parameters equal to those specified in the injection point.
     */
    private final class OSGiServiceQualifierType
            extends AnnotationLiteral<OSGiService>
            implements OSGiService {

        /**
         * Service criteria.
         */
        private String serviceCriteria = "";

        /**
         * Dynamic flag.
         */
        private boolean dynamic = false;

        /**
         * Wait timeout value.
         */
        private int waitTimeout = -1;

        /**
         * Create a new instance.
         * @param os service instance
         */
        OSGiServiceQualifierType(final OSGiService os) {
            this.serviceCriteria = os.serviceCriteria();
            this.dynamic = os.dynamic();
            this.waitTimeout = os.waitTimeout();
        }

        @Override
        public String serviceCriteria() {
            return this.serviceCriteria;
        }

        @Override
        public boolean dynamic() {
            return this.dynamic;
        }

        @Override
        public int waitTimeout() {
            return this.waitTimeout;
        }
    }

    /**
     * Log a message at the {@Code FINE} level.
     * @param msg message to log
     */
    private void debug(final String msg) {
        LOGGER.logp(Level.FINE, "OSGiServiceExtension", "debug",
                getClass().getSimpleName() + ":: {0}", new Object[]{msg});
    }

    /**
     * Log a debug message for the given injection point.
     * @param injectionPoint injection point
     */
    private void printDebugForInjectionPoint(
            final InjectionPoint injectionPoint) {

        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("@@@@@@@ INJECTION-POINT: Annotation:")
                    // annotatedfield
                    .append(injectionPoint.getAnnotated())
                    .append(" ,Bean:")
                    // bean
                    .append(injectionPoint.getBean())
                    // r untime
                    .append(" ,Class:").append(injectionPoint.getClass())
                    // class?
                    // Field
                    .append(" ,Member:").append(injectionPoint.getMember())
                    // qualifiers
                    .append(" ,Qualifiers:").append(injectionPoint
                            .getQualifiers())
                    // type of injection point
                    .append(" ,Type:").append(injectionPoint.getType());
            LOGGER.logp(Level.FINE, "OSGiServiceExtension",
                    "printDebugForInjectionPoint",
                    getClass().getSimpleName() + ":: {0}", new Object[]{sb});
        }
    }
}
