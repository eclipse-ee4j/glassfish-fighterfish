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
package org.glassfish.fighterfish.test.util;

/**
 * This exception has the additional role of printing equivalent of JStack so
 * that we can analyze any locking issues easily.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class TimeoutException extends RuntimeException {

    String msg;

    public TimeoutException() {
        collectStackTrace();
    }

    private void collectStackTrace() {
        msg = "Stack traces of all threads are given below:\n"
                + "[StackTraceBegin]\n"
                + new JStack()
                + "\n[StackTraceEnd]";
    }

    public TimeoutException(String message) {
        super(message);
        collectStackTrace();
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
        collectStackTrace();
    }

    public TimeoutException(Throwable cause) {
        super(cause);
        collectStackTrace();
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + msg;
    }
}
