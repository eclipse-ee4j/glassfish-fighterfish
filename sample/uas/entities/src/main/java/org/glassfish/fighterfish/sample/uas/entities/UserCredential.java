/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.uas.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Entity implementation class for Entity: UserCredential
 * 
 */
@Entity
public class UserCredential implements Serializable {

	@Id
	private String name;
	private String password;
	@OneToMany(mappedBy = "userCredential", cascade = CascadeType.REMOVE)
	private List<LoginAttempt> loginAttempts = new ArrayList<LoginAttempt>();
	private static final long serialVersionUID = 1L;

	public UserCredential() {
		super();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setLoginAttempts(List<LoginAttempt> loginAttempts) {
		this.loginAttempts = loginAttempts;
	}

	public List<LoginAttempt> getLoginAttempts() {
		return loginAttempts;
	}

}
