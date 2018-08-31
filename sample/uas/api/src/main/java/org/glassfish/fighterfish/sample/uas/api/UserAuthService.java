/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.uas.api;

/**
 * 
 * Simple interface to add users and authenticate them.
 * 
 * @author Sanjeeb Sahoo
 *
 */
public interface UserAuthService {
	boolean login(String name, String password);

	boolean register(String name, String password);

    boolean unregister(String name);
	
	String getReport();
}
