/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.advservice;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.fighterfish.sample.uas.entities.LoginAttempt;
import org.glassfish.fighterfish.sample.uas.entities.UserCredential;

import javax.persistence.EntityManager;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class AdvUserAuthServiceImpl implements UserAuthService {

    private final AdvSvcImplActivator activator;

    public AdvUserAuthServiceImpl(AdvSvcImplActivator activator) {
        this.activator = activator;
    }

    @Override
    public boolean login(String name, String password) {
        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return login2(name, password, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                throw t instanceof RuntimeException
                        ? RuntimeException.class.cast(t)
                        : new RuntimeException(t);
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
    public boolean register(@NotNull @Size(min = 3) String name,
            @NotNull @Size(min = 3) String password) {

        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return register2(name, password, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                throw t instanceof RuntimeException ? RuntimeException.class.cast(t) : new RuntimeException(t);
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
    public boolean unregister(String name) {
        boolean startedTX = startTX();
        try {
            EntityManager em = getEM();
            try {
                return unregister2(name, em);
            } catch (Throwable t) {
                t.printStackTrace();
                setRollbackOnly();
                throw t instanceof RuntimeException ? RuntimeException.class.cast(t) : new RuntimeException(t);
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
                throw t instanceof RuntimeException ? RuntimeException.class.cast(t) : new RuntimeException(t);
            } finally {
                em.close();
            }
        } finally {
            if (startedTX) {
                endTX();
            }
        }
    }

    private boolean login2(String name, String password, EntityManager em) {
        UserCredential uc = em.find(UserCredential.class, name);
        boolean result = (uc != null && password.equals(uc.getPassword()));
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

    private boolean register2(String name, String password, EntityManager em) {
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

    private boolean unregister2(String name, EntityManager em) {
        UserCredential uc = em.find(UserCredential.class, name);
        if (uc == null) {
            return false;
        }
        em.remove(uc);
        log("Unregistering (" + name + ")");
        return true;
    }

    @SuppressWarnings("unchecked")
    private String getReport2(EntityManager em) {
        List<LoginAttempt> attempts = em
                .createNamedQuery("LoginAttempt.findAll")
                .getResultList();
        log("Number of entries found: " + attempts.size());
        StringBuilder report = new StringBuilder("Login Attempt Report:\n");
        for (LoginAttempt attempt : attempts) {
            report.append(attempt).append("\n");
        }
        return report.toString();
    }

    private void log(String msg) {
        System.out.println("AdvUserAuthServiceImpl: " + msg);
    }

    /**
     * Start tx iff there is not one active yet.
     *
     * @return true if started, else false
     */
    private boolean startTX() {
        UserTransaction utx = getUTX();
        try {
            if (utx.getStatus() == 6) {  // NO_TRANSACTION
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
     * Commit or rollback a transaction depending on rollback flag set in the
     * tx.
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

    private UserTransaction getUTX() {
        return activator.getUTX();
    }

    private EntityManager getEM() {
        return activator.getEMF().createEntityManager();
    }
}
