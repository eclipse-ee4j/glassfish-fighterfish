/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.osgihttp.helloworld;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * This class is an SCR component. It registers servlets and resources in
 * activate() method and unregisters them in deactivate() method. It consumes a
 * service of type HttpService. The service reference is bound and unbound in
 * setHttp() and unsetHttp() method respectively as specified in scr.xml file.
 */
public class HelloWorldHttpMain {

    /**
     * OSGi HTTP service.
     */
    private HttpService http; // Set and unset by setHttp() and unsetHttp()

    /**
     * OSGi event administration.
     */
    private EventAdmin eventAdmin;
    // methods that are called by SCR. See scr.xml

    /**
     * Activate the application.
     * @param ctx component context
     * @throws ServletException if an error occurs while registering the servlet
     * @throws NamespaceException if an error occurs
     */
    protected final void activate(final ComponentContext ctx)
            throws ServletException, NamespaceException {

        // One HttpContext maps to one ServletContext.
        // To demonstrate this functionality, we shall do the following:
        // We will create one HttpContext and register Servlet1 and Servlet2
        // using this.
        // We will then set an attribute in ServletContext and make sure we
        // can read
        // its value inside both Servlet's service().
        // We will create a second HttpContext and register Servlet3 using
        // the second one.
        // We will show that Servlet3's ServletContext does not have the
        // attribute set in Servlet1 or Servlet2's
        // ServletContext. We will also show that Servlet1 and Servlet2 share
        // the same HttpSession which is
        // different from Servlet3.
        HttpContext httpCtx = http.createDefaultHttpContext();
        HttpServlet servlet1 = new HelloWorldServlet1();
        http.registerServlet("/hello1", servlet1, null, httpCtx);
        System.out.println(servlet1.getServletContext());
        HttpServlet servlet2 = new HelloWorldServlet2();
        http.registerServlet("/hello2", servlet2, null, httpCtx);
        System.out.println(servlet2.getServletContext());
        servlet1.getServletContext().setAttribute(
                HelloWorldServlet1.ATTRIBUTE_NAME, new Integer(0));

        // Let's create another HttpContext and make sure that each context
        // has its own
        // ServletContext and HttpSession.
        HttpContext httpCtx2 = http.createDefaultHttpContext();
        HttpServlet servlet3 = new HelloWorldServlet3();
        http.registerServlet("/hello3", servlet3, null, httpCtx2);
        System.out.println(servlet3.getServletContext());
        assert (servlet3.getServletContext() != servlet1.getServletContext());
        if (eventAdmin != null) {
            // raise an event so that our test framework can catch it to
            // proceed to test
            Map props = new HashMap();
            Event event = new Event(getClass().getPackage().getName()
                    .replace(".", "/"), props);
            eventAdmin.postEvent(event);
            System.out.println("raised event " + event);
        }
    }

    /**
     * Deactivate the given component.
     * @param ctx component context
     */
    protected final void deactivate(final ComponentContext ctx) {
        try {
            http.unregister("/hello1");
            http.unregister("/hello2");
            http.unregister("/hello3");
        } catch (Exception e) {
            // This can happen if the HttpService has been undpeloyed in which
            // case as part of its undepoyment,
            // it would have unregistered all aliases. So, we should protect
            // against such a case.
            System.out.println(e);
        }
    }

    /**
     * Set the OSGi HTTP service.
     * @param hs service instance
     */
    protected final void setHttp(final HttpService hs) {
        this.http = hs;
    }

    /**
     * Unset the OSGi HTTP service.
     * @param hs service instance
     */
    protected final void unsetHttp(final HttpService hs) {
        this.http = null;
    }

    /**
     * Set the event admin.
     * @param ea event admin instance
     */
    protected final void setEventAdmin(final EventAdmin ea) {
        this.eventAdmin = ea;
    }

    /**
     * Unset the event admin.
     * @param ea event admin instance.
     */
    protected final void unsetEventAdmin(final EventAdmin ea) {
        this.eventAdmin = null;
    }
}
