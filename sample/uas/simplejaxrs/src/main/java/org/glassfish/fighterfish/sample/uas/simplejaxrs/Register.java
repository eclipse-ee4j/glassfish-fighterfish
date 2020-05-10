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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;

/**
 * Example resource class hosted at the URI path "/register".
 */
@Path("/register")
@RequestScoped
public class Register {

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
    @Path("/getIt")
    @Produces("text/plain")
    public String getIt() {
        return "Hi there!";
    }

    /**
     * Register a user.
     * @param name user name
     * @param password user password
     * @return {@code "Registered"} if successful, {@code "Failed"} otherwise
     * @throws ServletException if an error occurs
     */
    @POST
    @Produces(TEXT_HTML)
    @Consumes(TEXT_PLAIN)
    public String register(@QueryParam("name") String name, @QueryParam("password") String password) throws ServletException {
        try {
            if (userAuthService.register(name, password)) {
                return "Registered";
            }
                
            return "Failed";
        } catch (ServiceException e) {
        }
        
        return "none";
    }
}
