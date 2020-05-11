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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;

import org.glassfish.internal.api.DelegatingClassLoader;

/**
 * An implementation of {@link org.glassfish.internal.api.DelegatingClassLoader.ClassFinder} that uses reflection to
 * call the methods of the delegate. It is currently NOT used because it requires special permission granted to this
 * code-base to access protected members like findClass.
 *
 * This is pretty much an ugly hack.
 */
final class ReflectiveClassFinder implements DelegatingClassLoader.ClassFinder {

    /**
     * The delegate class-loader.
     */
    private final ClassLoader delegate;

    /**
     * The "findClass" method of the delegate class-loader.
     */
    private final Method findClass;

    /**
     * The "findLoadedClass" method of the delegate class-loader.
     */
    private final Method findLoadedClass;

    /**
     * The "findResource" method of the delegate class-loader.
     */
    private final Method findResource;

    /**
     * The "findResources" method of the delegate class-loader.
     */
    private final Method findResources;

    /**
     * Create a new instance.
     *
     * @param cl delegate class-loader
     */
    ReflectiveClassFinder(final ClassLoader cl) {
        this.delegate = cl;
        Class<ClassLoader> clazz = ClassLoader.class;
        try {
            findClass = clazz.getDeclaredMethod("findClass", String.class);
            findLoadedClass = clazz.getDeclaredMethod("findLoadedClass", String.class);
            findResource = clazz.getDeclaredMethod("findResource", String.class);
            findResources = clazz.getDeclaredMethod("findResources", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClassLoader getParent() {
        return delegate.getParent();
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {

        try {
            Object result = findClass.invoke(delegate, name);
            return (Class) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Class<?> findExistingClass(final String name) {
        try {
            Object result = findLoadedClass.invoke(delegate, name);
            return (Class) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public URL findResource(final String name) {
        try {
            Object result = findResource.invoke(delegate, name);
            return (URL) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<URL> findResources(final String name) throws IOException {

        try {
            Object result = findResources.invoke(delegate, name);
            return (Enumeration<URL>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
