/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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
import javax.persistence.*;

/**
 * Entity implementation class for Entity: UserCredential
 *
 */
@Entity
public class UserCredential implements Serializable {

	
	private static final long serialVersionUID = 1L;

    // Absence of @GeneratedValue annotation means
    // user is responsible for setting id.
    @Id // name is the PK of this entity.
    private String name;

    private String password;

    protected UserCredential() {
        // Every entity bean must have a default public or protected constructor
    }

    public UserCredential(String name, String password) {
        if ( (name == null || name.length() == 0) ||
             (password == null || password.length() == 0)) {
             throw new IllegalArgumentException("name or password is empty");
        }
        this.name = name;
        this.password = password;
    }

    public boolean isMatchingPassword(String password) {
        return this.password.equals(password);
    }
   
}
