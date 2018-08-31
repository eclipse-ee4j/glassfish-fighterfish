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
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity implementation class for Entity: LoginAttempt
 * 
 */
@Entity
@NamedQueries(@NamedQuery(name = "LoginAttempt.findAll", query = "select o from LoginAttempt o"))
public class LoginAttempt implements Serializable {
	@Id
	@GeneratedValue
	private long serialNumber;

	private boolean successful;

	@ManyToOne(fetch = FetchType.LAZY)
	private UserCredential userCredential;

	@Temporal(TemporalType.TIMESTAMP)
	Date timeStamp = new Date();
	
	private static final long serialVersionUID = 1L;

	public LoginAttempt() {
		super();
	}

	public long getSerialNumber() {
		return this.serialNumber;
	}

	public boolean isSuccessful() {
		return this.successful;
	}

	public void setSuccessful(boolean isSuccessful) {
		this.successful = isSuccessful;
	}

	public UserCredential getUserCredential() {
		return this.userCredential;
	}

	public void setUserCredential(UserCredential userCredential) {
		this.userCredential = userCredential;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

    @Override
    public String toString() {
        return "LoginAttempt: (" +
                "serialNumber = " + serialNumber +
                "user = "  + getUserCredential().getName() +
                "isSucecssful = " + isSuccessful() +
                ")";
    }
}
