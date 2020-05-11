/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.ejbservice;

import java.util.List;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.fighterfish.sample.uas.entities.LoginAttempt;
import org.glassfish.fighterfish.sample.uas.entities.UserCredential;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Session Bean implementation class UserAuthServiceEJB.
 */
@Stateless
@Local({ UserAuthService.class })
public final class UserAuthServiceEJB implements UserAuthService {

    /**
     * Entity manager.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create a new instance.
     */
    public UserAuthServiceEJB() {
    }

    @Override
    public boolean login(String name, String password) {
        log("Logging in (" + name + ", " + password + ")");
        
        UserCredential userCredential = entityManager.find(UserCredential.class, name);
        boolean result = userCredential != null && password.equals(userCredential.getPassword());
        if (userCredential != null) {
            
            // Create a LoginAttempt only for existing users.
            LoginAttempt attempt = new LoginAttempt();
            attempt.setSuccessful(result);
            attempt.setUserCredential(userCredential);
            
            // set both sides of relationships because stupid JPA providers
            // don't even update their second level cache
            // with relationships in database.
            userCredential.getLoginAttempts().add(attempt);
            entityManager.persist(attempt);
        }
        
        return result;
    }

    @Override
    public boolean register(String name, String password) {
        log("Registering (" + name + ", " + password + ")");
        
        UserCredential userCredential = entityManager.find(UserCredential.class, name);
        if (userCredential != null) {
            return false;
        }
        
        userCredential = new UserCredential();
        userCredential.setName(name);
        userCredential.setPassword(password);
        entityManager.persist(userCredential);
        
        return true;
    }

    @Override
    public boolean unregister(final String name) {
        log("Unregistering (" + name + ")");
        
        UserCredential userCredential = entityManager.find(UserCredential.class, name);
        if (userCredential == null) {
            return false;
        }
        entityManager.remove(userCredential);
        
        return true;
    }

    @Override
    public String getReport() {
        @SuppressWarnings("unchecked")
        List<LoginAttempt> attempts = entityManager.createNamedQuery("LoginAttempt.findAll").getResultList();
        log("Number of entries found: " + attempts.size());
        
        StringBuilder report = new StringBuilder("Login Attempt Report:\n");
        for (LoginAttempt attempt : attempts) {
            report.append(attempt).append("\n");
        }
        
        return report.toString();
    }

    /**
     * Log a message to the standard output.
     *
     * @param msg message to log
     */
    private void log(final String msg) {
        System.out.println("UserAuthServiceEJB: " + msg);
    }
}
