/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.fighterfish.test.app2;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity implementation class for Entity: UserCredential.
 */
@Entity
public class UserCredential implements Serializable {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;

    // Absence of @GeneratedValue annotation means
    // user is responsible for setting id.
    // name is the PK of this entity.
    /**
     * User name.
     */
    @Id
    private String name;

    /**
     * User password.
     */
    private String password;

    /**
     * Create a new instance.
     */
    protected UserCredential() {
        // Every entity bean must have a default public or protected
        // constructor
    }

    /**
     * Create a new instance.
     * @param username user name
     * @param passwd user password
     */
    public UserCredential(final String username, final String passwd) {
        if ((username == null || username.length() == 0)
                || (passwd == null || passwd.length() == 0)) {
            throw new IllegalArgumentException("name or password is empty");
        }
        this.name = username;
        this.password = passwd;
    }

    /**
     * Match the user password.
     * @param passwd user password
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    public boolean isMatchingPassword(final String passwd) {
        return this.password.equals(passwd);
    }
}
