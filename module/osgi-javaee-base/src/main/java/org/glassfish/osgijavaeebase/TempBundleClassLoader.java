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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * This class loader only provides a new class loading namespace It is useful during annotation scanning classes get
 * loaded in that separate namespace. This class loader delegates all stream handling (i.e. reading actual
 * class/resource data) operations to a delegate Bundle. It only defines the Class using the byte codes.
 */
public final class TempBundleClassLoader extends ClassLoader {

    /**
     * Delegate class-loader.
     */
    private final BundleClassLoader delegate;

    /**
     * Create a new instance.
     * 
     * @param cl the delegate class-loader
     */
    public TempBundleClassLoader(final BundleClassLoader cl) {
        // Set our parent same as delegate's
        super(cl.getParent());
        this.delegate = cl;
    }

    /**
     * This method uses the delegate to use class bytes and then defines the class using this class loader.
     * 
     * @return Class
     * @throws java.lang.ClassNotFoundException if an error occurs
     */
    @Override
    protected Class findClass(final String name) throws ClassNotFoundException {

        String entryName = name.replace('.', '/') + ".class";
        URL url = delegate.getResource(entryName);
        if (url == null) {
            throw new ClassNotFoundException(name);
        }
        InputStream inputStream = null;
        byte[] bytes = null;
        try {
            inputStream = url.openStream();
            bytes = getClassData(inputStream);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        // Define package information if necessary
        int lastPackageSep = name.lastIndexOf('.');
        if (lastPackageSep != -1) {
            String packageName = name.substring(0, lastPackageSep);
            if (getPackage(packageName) == null) {
                try {
                    // There's a small chance that one of our parents
                    // could define the same package after getPackage
                    // returns null but before we call definePackage,
                    // since the parent classloader instances
                    // are not locked. So, just catch the exception
                    // that is thrown in that case and ignore it.
                    //
                    // It's unclear where we would get the info to
                    // set all spec and impl data for the package,
                    // so just use null. This is consistent will the
                    // JDK code that does the same.
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException iae) {
                    // duplicate attempt to define same package.
                    // safe to ignore.
                }
            }
        }
        Class clazz = null;
        try {
            // TODO(Sahoo): Set appropriate protection domain
            clazz = defineClass(name, bytes, 0, bytes.length, null);
            return clazz;
        } catch (UnsupportedClassVersionError ucve) {
            throw new UnsupportedClassVersionError(name + " can't be defined as we are running in Java version" + System.getProperty("java.version"));
        }
    }

    @Override
    public URL getResource(final String name) {
        return delegate.getResource(name);
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {

        return delegate.getResources(name);
    }

    /**
     * Returns the byte array from the given input stream.
     *
     * @param istream input stream to the class or resource
     * @return byte array
     * @throws IOException if an i/o error
     */
    @SuppressWarnings("checkstyle:magicnumber")
    private byte[] getClassData(final InputStream istream) throws IOException {

        BufferedInputStream bstream = new BufferedInputStream(istream);
        byte[] buf = new byte[4096];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int num = 0;
        try {
            while ((num = bstream.read(buf)) != -1) {
                bout.write(buf, 0, num);
            }
        } finally {
            if (bstream != null) {
                bstream.close();
            }
        }
        return bout.toByteArray();
    }
}
