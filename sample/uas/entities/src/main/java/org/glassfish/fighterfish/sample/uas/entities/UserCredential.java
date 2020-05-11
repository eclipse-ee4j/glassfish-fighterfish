/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.entities;

import static jakarta.persistence.CascadeType.REMOVE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Entity implementation class for Entity: UserCredential.
 */
@Entity
public class UserCredential implements Serializable {
    
    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * User credential ID.
     */
    @Id
    private String name;

    /**
     * User password.
     */
    private String password;

    /**
     * Login attempts.
     */
    @OneToMany(mappedBy = "userCredential", cascade = REMOVE)
    private List<LoginAttempt> loginAttempts = new ArrayList<>();


    /**
     * Get the user name.
     *
     * @return user name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the user name.
     *
     * @param username new name
     */
    public void setName(String username) {
        this.name = username;
    }

    /**
     * Get the user password.
     *
     * @return password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Set the user password.
     *
     * @param passwd new password
     */
    public void setPassword(String passwd) {
        this.password = passwd;
    }

    /**
     * Set the login attempts.
     *
     * @param attemps new login attempts
     */
    public void setLoginAttempts(List<LoginAttempt> attemps) {
        this.loginAttempts = attemps;
    }

    /**
     * Get the login attempts.
     *
     * @return list of login attempts
     */
    public List<LoginAttempt> getLoginAttempts() {
        return loginAttempts;
    }
}
