/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.simplejaxrs;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.ServiceException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;

/**
 * Example resource class hosted at the URI path "/login".
 */
@Path("/login")
@RequestScoped
public class MyResource {

    /**
     * URI info.
     */
    @Context
    private UriInfo uriInfo;

    /**
     * Request.
     */
    @Context
    private Request request;

    /**
     * User authentication service.
     */
    @Inject
    @OSGiService(dynamic = true)
    private UserAuthService userAuthService;

    /**
     * Method processing HTTP GET requests, producing "text/plain" MIME media
     * type.
     *
     * @return String that will be send back as a response of type "text/plain".
     */
    @POST
    @Produces("text/plain")
    public String getIt() {
        return "Hi there!";
    }

    /**
     * Login to the application.
     * @param request servlet request
     * @param response servlet response
     * @return {@code "Logged in"} if successful, {@code "Fail"} otherwise
     * @throws ServletException if an error occurs
     */
    @GET
    @Produces(TEXT_HTML)
    @Consumes(TEXT_PLAIN)
    public String login(@Context HttpServletRequest request, @Context HttpServletResponse response) throws ServletException {
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        
        try {
            if (userAuthService.login(name, password)) {
                return "Logged in";
            }
            
            return "Fail";
        } catch (ServiceException e) {
        }
        
        return "none";
    }
}
