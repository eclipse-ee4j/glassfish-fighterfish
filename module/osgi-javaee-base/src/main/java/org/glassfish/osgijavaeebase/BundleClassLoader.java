/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijavaeebase;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;

/**
 * This is a delegating class loader. It always delegates to OSGi bundle's class loader. ClassLoader.defineClass() is
 * never called in the context of this class. There will never be a class for which getClassLoader() would return this
 * class loader. It overrides loadClass(), getResource() and getResources() as opposed to their findXYZ() equivalents so
 * that the OSGi export control mechanism is enforced even for classes and resources available in the system/boot class
 * loader.
 */
public final class BundleClassLoader extends ClassLoader {

    /**
     * The bundle.
     */
    private final Bundle bundle;

    /**
     * Create a new instance.
     *
     * @param bnd the bundle
     */
    public BundleClassLoader(final Bundle bnd) {
        super(Bundle.class.getClassLoader());
        this.bundle = bnd;
    }

    @Override
    public synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {

        return bundle.loadClass(name);
    }

    @Override
    public URL getResource(final String name) {
        return bundle.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {

        Enumeration<URL> resources = bundle.getResources(name);
        if (resources == null) {
            // This check is needed, because ClassLoader.getResources()
            // expects us to return an empty enumeration.
            resources = new Enumeration<URL>() {

                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public URL nextElement() {
                    throw new NoSuchElementException();
                }
            };
        }
        return resources;
    }
}
