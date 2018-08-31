/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.uas.simpleservice;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;

class UserAuthServiceImpl implements UserAuthService {

	UserAuthServiceImpl() {
	}

	Map<String, String> users = new HashMap<String, String>();
	
	StringBuilder report = new StringBuilder("Login Attempt Report:\n");
	
	@Override
	public boolean login(String name, String password) {
		boolean result = (name != null && name.trim().length() > 0 
				&& password != null && password.trim().length() > 0
				&& password.equals(users.get(name)));
		addReport(name, password, result);
		return result;
	}

	@Override
	public boolean register(String name, String password) {
		boolean result = (name != null && name.trim().length() > 0 
				&& password != null && password.trim().length() > 0
				&& !users.containsKey(name));
		if (result) users.put(name, password);
		addReport(name, password, result);
		return result;
	}

    @Override
    public boolean unregister(String name) {
        return (name != null && users.remove(name) != null);
    }

    @Override
	public String getReport() {
		return report.toString();
	}
	
	private void addReport(String name, String password, boolean result) {
		String msg = new Date() + "LoginAttempt: (" + name + ", " + password + "): " + result;
		SimpleActivator.log(msg);
		report.append(msg + "\n");
	}

}
