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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class holds version values populated from the build.
 */
public final class Version {

    /**
     * Cannot be instanciated.
     */
    private Version() {
    }

    /**
     * Get the main fighterfish version.
     * @return version string
     */
    public static String getVersion() {
        Properties props = new Properties();
        final InputStream stream = Version.class.getResourceAsStream(
                "VersionInfo.properties");
        try {
            props.load(stream);
            return props.getProperty("Version");
        } catch (IOException e) {
            // not sure why this should happen. if it happens, propagate up
            throw new RuntimeException(e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }
}
