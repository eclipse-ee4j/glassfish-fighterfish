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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * A simple {@link BaseMatcher} that matches if a string contains a specific
 * character sequence. No regular expression support as yet.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class StringPatternMatcher extends BaseMatcher<String> {

    private final String expectedOutputPattern;

    public StringPatternMatcher(String expectedOutputPattern) {
        this.expectedOutputPattern = expectedOutputPattern;
    }

    @Override
    public boolean matches(Object o) {
        return String.class.cast(o).contains(expectedOutputPattern);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Expected output to contain ["
                + expectedOutputPattern + "]");
    }
}
