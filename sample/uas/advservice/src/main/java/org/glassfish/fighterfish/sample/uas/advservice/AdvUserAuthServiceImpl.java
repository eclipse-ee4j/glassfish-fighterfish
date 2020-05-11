/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.advservice;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.fighterfish.sample.uas.entities.LoginAttempt;
import org.glassfish.fighterfish.sample.uas.entities.UserCredential;

/**
 * Implementation of the user authentication service.
 */
public final class AdvUserAuthServiceImpl implements UserAuthService {

    /**
     * Minimum length for username.
     */
    private static final int MIN_NAME_LENGHT = 3;

    /**
     * Minimum password length.
     */
    private static final int MIN_PASWORD_LENGTH = 3;

    /**
     * Bundle activator.
     */
    private final AdvSvcImplActivator activator;

    /**
     * Create a new instance.
     *
     * @param bndActivator bundle activator
     */
    public AdvUserAuthServiceImpl(final AdvSvcImplActivator bndActivator) {
        this.activator = bndActivator;
    }

    @Override
    public boolean login(final String name, final String password) {
        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return login2(name, password, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                if (t instanceof RuntimeException) {
                    throw RuntimeException.class.cast(t);
                } else {
                    throw new RuntimeException(t);
                }
            } finally {
                em.close();
            }
        } finally {
            if (startedTX) {
                endTX();
            }
        }
    }

    @Override
    public boolean register(@NotNull @Size(min = MIN_NAME_LENGHT) final String name, @NotNull @Size(min = MIN_PASWORD_LENGTH) final String password) {

        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return register2(name, password, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                if (t instanceof RuntimeException) {
                    throw RuntimeException.class.cast(t);
                } else {
                    throw new RuntimeException(t);
                }
            } finally {
                em.close();
            }
        } finally {
            if (startedTX) {
                endTX();
            }
        }
    }

    @Override
    public boolean unregister(final String name) {
        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return unregister2(name, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                if (t instanceof RuntimeException) {
                    throw RuntimeException.class.cast(t);
                } else {
                    throw new RuntimeException(t);
                }
            } finally {
                em.close();
            }
        } finally {
            if (startedTX) {
                endTX();
            }
        }
    }

    @Override
    public String getReport() {
        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return getReport2(em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                if (t instanceof RuntimeException) {
                    throw RuntimeException.class.cast(t);
                } else {
                    throw new RuntimeException(t);
                }
            } finally {
                em.close();
            }
        } finally {
            if (startedTX) {
                endTX();
            }
        }
    }

    /**
     * Actual implementation of the login method.
     *
     * @param name user name
     * @param password user passowrd
     * @param em entity manager
     * @return {@code true} if authenticated, {@code false} otherwise
     */
    private boolean login2(final String name, final String password, final EntityManager em) {

        UserCredential uc = em.find(UserCredential.class, name);
        boolean result = uc != null && password.equals(uc.getPassword());
        log("Logging in (" + name + ", " + password + ")");
        if (uc != null) {
            LoginAttempt attempt = new LoginAttempt();
            attempt.setSuccessful(result);
            attempt.setUserCredential(uc);
            // set both sides of relationships because stupid JPA providers
            // don't even update their second level cache
            // with relationships in database.
            uc.getLoginAttempts().add(attempt);
            em.persist(attempt);
        }
        return result;
    }

    /**
     * Actual implementation of the register method.
     *
     * @param name user name
     * @param password user password
     * @param em entity manager
     * @return {@code true} if registered, {@code false} otherwise
     */
    private boolean register2(final String name, final String password, final EntityManager em) {

        UserCredential uc = em.find(UserCredential.class, name);
        if (uc != null) {
            return false;
        }
        uc = new UserCredential();
        uc.setName(name);
        uc.setPassword(password);
        em.persist(uc);
        log("Registering (" + name + ", " + password + ")");
        return true;
    }

    /**
     * Actual implementation of the unregister method.
     *
     * @param name user name
     * @param em entity manager
     * @return {@code true} if unregistered, {@code false} otherwise
     */
    private boolean unregister2(final String name, final EntityManager em) {

        UserCredential uc = em.find(UserCredential.class, name);
        if (uc == null) {
            return false;
        }
        em.remove(uc);
        log("Unregistering (" + name + ")");
        return true;
    }

    /**
     * Actual implementation of the report method.
     *
     * @param em entity manager
     * @return report string
     */
    @SuppressWarnings("unchecked")
    private String getReport2(final EntityManager em) {
        List<LoginAttempt> attempts = em.createNamedQuery("LoginAttempt.findAll").getResultList();
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
        System.out.println("AdvUserAuthServiceImpl: " + msg);
    }

    /**
     * Start tx iff there is not one active yet.
     *
     * @return true if started, else false
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private boolean startTX() {
        UserTransaction utx = getUTX();
        try {
            if (utx.getStatus() == 6) { // NO_TRANSACTION
                utx.begin();
                return true;
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Commit or rollback a transaction depending on rollback flag set in the tx.
     */
    private void endTX() {
        try {
            UserTransaction utx = getUTX();
            if (utx.getStatus() == 1) {
                utx.rollback();
            } else {
                utx.commit();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the user transaction as rollback only.
     */
    private void setRollbackOnly() {
        UserTransaction utx = getUTX();
        try {
            utx.setRollbackOnly();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the user transaction.
     *
     * @return UserTransaction
     */
    private UserTransaction getUTX() {
        return activator.getUTX();
    }

    /**
     * Get the entity manager.
     *
     * @return EntityManager
     */
    private EntityManager getEM() {
        return activator.getEMF().createEntityManager();
    }
}
