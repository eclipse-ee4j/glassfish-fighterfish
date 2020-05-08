/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.ejbservice2;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.fighterfish.sample.uas.entities.LoginAttempt;
import org.glassfish.fighterfish.sample.uas.entities.UserCredential;
import org.glassfish.osgicdi.OSGiService;

/**
 * Session Bean implementation class UserAuthServiceEJB2.
 */
@Stateless
@Local(UserAuthService.class)
public final class UserAuthServiceEJB2 implements UserAuthService {

    /**
     * Entity manager factory.
     */
    @Inject
    @OSGiService(dynamic = true, serviceCriteria = "(persistence-unit=sample.uas.entities)")
    private EntityManagerFactory emf;

    /**
     * Create a new instance.
     */
    public UserAuthServiceEJB2() {
    }

    @Override
    public boolean login(String name, String password) {
        log("Logging in (" + name + ", " + password + ")");

        EntityManager entityManager = emf.createEntityManager();
        try {
            UserCredential userCredential = entityManager.find(UserCredential.class, name);
            boolean result = (userCredential != null && password.equals(userCredential.getPassword()));
            if (userCredential != null) {

                // create LoginAttempt only for existing users.
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
        } finally {
            entityManager.close();
        }
    }

    @Override
    public boolean register(final String name, final String password) {
        log("Registering (" + name + ", " + password + ")");

        EntityManager entityManager = emf.createEntityManager();
        try {
            UserCredential userCredential = entityManager.find(UserCredential.class, name);
            if (userCredential != null) {
                return false;
            }

            userCredential = new UserCredential();
            userCredential.setName(name);
            userCredential.setPassword(password);
            entityManager.persist(userCredential);

            return true;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public boolean unregister(String name) {
        log("Unregistering (" + name + ")");

        EntityManager entityManager = emf.createEntityManager();
        try {
            UserCredential userCredential = entityManager.find(UserCredential.class, name);
            if (userCredential == null) {
                return false;
            }
            entityManager.remove(userCredential);
            return true;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public String getReport() {
        EntityManager entityManager = emf.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<LoginAttempt> attempts = entityManager.createNamedQuery("LoginAttempt.findAll").getResultList();

            log("Number of entries found: " + attempts.size());

            StringBuilder report = new StringBuilder("Login Attempt Report:\n");
            for (LoginAttempt attempt : attempts) {
                report.append(attempt).append("\n");
            }
            return report.toString();
        } finally {
            entityManager.close();
        }
    }

    /**
     * Log a message to the standard output.
     * 
     * @param msg message to log
     */
    private void log(final String msg) {
        System.out.println(UserAuthServiceEJB2.class.getSimpleName() + ": " + msg);
    }
}
