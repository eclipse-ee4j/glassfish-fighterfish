/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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
 */
public final class TimeoutException extends RuntimeException {

    /**
     * Additional message.
     */
    private String msg;

    /**
     * Create a new instance.
     */
    public TimeoutException() {
        collectStackTrace();
    }

    /**
     * Fill-up the additional message with extra info.
     */
    private void collectStackTrace() {
        msg = "Stack traces of all threads are given below:\n"
                + "[StackTraceBegin]\n"
                + new JStack()
                + "\n[StackTraceEnd]";
    }

    /**
     * Create a new instance with a message and no cause.
     * @param message exception message
     */
    public TimeoutException(final String message) {
        super(message);
        collectStackTrace();
    }

    /**
     * Create a new instance with a message and a cause.
     * @param message exception message
     * @param cause exception cause
     */
    public TimeoutException(final String message, final Throwable cause) {
        super(message, cause);
        collectStackTrace();
    }

    /**
     * Create a new instance with no message and a cause.
     * @param cause exception cause
     */
    public TimeoutException(final Throwable cause) {
        super(cause);
        collectStackTrace();
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + msg;
    }
}
