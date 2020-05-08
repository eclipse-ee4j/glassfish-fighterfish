/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.simpleservice;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;

/**
 * Implementation of the user authentication service.
 */
final class UserAuthServiceImpl implements UserAuthService {

    /**
     * Create a new instance.
     */
    UserAuthServiceImpl() {
    }

    /**
     * Users map.
     */
    private final Map<String, String> users = new HashMap<>();

    /**
     * Report string.
     */
    private final StringBuilder report = new StringBuilder("Login Attempt Report:\n");

    @Override
    public boolean login(final String name, final String password) {
        boolean result = name != null && name.trim().length() > 0 && password != null && password.trim().length() > 0 && password.equals(users.get(name));
        addReport(name, password, result);
        return result;
    }

    @Override
    public boolean register(final String name, final String password) {
        boolean result = name != null && name.trim().length() > 0 && password != null && password.trim().length() > 0 && !users.containsKey(name);
        if (result) {
            users.put(name, password);
        }
        addReport(name, password, result);
        return result;
    }

    @Override
    public boolean unregister(final String name) {
        return name != null && users.remove(name) != null;
    }

    @Override
    public String getReport() {
        return report.toString();
    }

    /**
     * Add a login report to the report string.
     *
     * @param name user name
     * @param password user password
     * @param result authentication result
     */
    private void addReport(final String name, final String password, final boolean result) {

        String msg = new Date() + "LoginAttempt: (" + name + ", " + password + "): " + result;
        SimpleActivator.log(msg);
        report.append(msg).append("\n");
    }
}
