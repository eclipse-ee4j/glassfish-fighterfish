/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgicdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * A CDI (JSR-299) Qualifier that indicates a reference to a Service in the OSGi service registry that needs to be
 * injected into a Bean/Java EE Component.
 *
 * A Java EE component developer uses this annotation to indicate that the injection point needs to be injected with an
 * OSGi service and can also provide additional meta-data to aid in service discovery.
 *
 * If this qualifier annotates an injection point, the <code>OSGiServiceExtension</code> discovers and instantiates the
 * service implementing the service interface type of the injection point, and makes it available for injection to that
 * injection point.
 */
@Qualifier
@Target({ TYPE, METHOD, PARAMETER, FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OSGiService {

    /**
     * Determines if the OSGi service that is to be injected refers to a dynamic instance of the service or is statically
     * bound to the service implementation discovered at the time of injection.If the value of this annotation element is
     * true, a proxy to the service interface is returned to the client.
     *
     * When the service is used, an active instance of the service at that point in time is used. If a service instance that
     * was obtained earlier has gone away (deregistered by the service provider or stopped), then a new instance of the
     * service is obtained from the OSGi service registry. This is ideal for stateless and/or idempotent services or service
     * implementations whose life-cycle may be shorter than the client's lifecycle.
     *
     * If the value of this annotation element is false, an instance of the service is obtained from the service registry at
     * the time of injection and provided to the client. If the service implementation provider deregisters the obtained
     * service or the service instance is stopped, no attempt is made to get another instance of the service and a
     * <code>ServiceUnavailableException</code> is thrown on method invocation. This is ideal for stateful or contextual
     * services and for references to service implementations whose life-cycle is well-known and is known to be greater than
     * the life-cycle of the client.
     *
     * @return {@code true} if dynamic, {@code false} otherwise
     */
    boolean dynamic() default false;

    /**
     * Service discovery criteria.The string provided must match the Filter syntax specified in the OSGi Core Specification.
     *
     * @return service criteria
     */
    String serviceCriteria() default "";

    /**
     * Waits, for the specified milliseconds, for at least one service that matches the criteria specified to be available
     * in the OSGi Service registry.
     *
     * @return 0 indicates indefinite wait. -1 indicates that the service is returned immediately if available or a null is
     * returned if not available.
     */
    int waitTimeout() default -1;
}
