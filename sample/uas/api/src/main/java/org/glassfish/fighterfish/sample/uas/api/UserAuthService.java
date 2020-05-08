/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.api;

/**
 * Simple interface to add users and authenticate them.
 */
public interface UserAuthService {

    /**
     * Authenticate a user.
     * 
     * @param name user name
     * @param password user password
     * @return {@code true} if authenticated, {@code false} otherwise
     */
    boolean login(String name, String password);

    /**
     * Register a user.
     * 
     * @param name user name
     * @param password user password
     * @return {@code true} if the user is registered, {@code false} otherwise
     */
    boolean register(String name, String password);

    /**
     * Unregister a user.
     * 
     * @param name user name
     * @return {@code true} if the user was unregistered, {@code false} otherwise
     */
    boolean unregister(String name);

    /**
     * Get the report.
     * 
     * @return report
     */
    String getReport();
}
