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
package org.glassfish.osgiweb;

import com.sun.faces.spi.AnnotationProvider;

import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationType;
import org.glassfish.hk2.classmodel.reflect.Member;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;

/**
 * Custom annotation provider for Mojarra.
 */
public final class OSGiFacesAnnotationScanner extends AnnotationProvider {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiFacesAnnotationScanner.class.getPackage().getName());

    /**
     * Creates a new {@code AnnotationScanner} instance. This is a much needed constructor as mojarra initializes using this
     * constructor.
     *
     * @param sc the {@code ServletContext} for the application to be scanned
     */
    public OSGiFacesAnnotationScanner(final ServletContext sc) {
        super(sc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(final Set<URI> uris) {

        Map<Class<? extends Annotation>, Set<Class<?>>> result = (Map<Class<? extends Annotation>, Set<Class<?>>>) servletContext
                .getAttribute(Constants.FACES_ANNOTATED_CLASSES);
        // clear it
        servletContext.setAttribute(Constants.FACES_ANNOTATED_CLASSES, null);
        if (result == null) {
            LOGGER.warning("Faces annotation parsing has not taken place");
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * Scan annotations.
     * 
     * @param uris a filter for the class-loader resource to be scanned
     * @param types the annotations as {@code Type}
     * @param cl class-loader
     * @return map of scanned annotations
     */
    static Map<Class<? extends Annotation>, Set<Class<? extends Object>>> scan(final Collection<URI> uris, final Types types, final ClassLoader cl) {

        // can't use ServletContext here, because it is not yet available as
        // this method is called
        // from WebModuleDecorator which is called when WebModule is being
        // created.
        // hence this is a static method.
        // CHECKSTYLE:OFF
        Map<Class<? extends Annotation>, Set<Class<? extends Object>>> result = new HashMap<Class<? extends Annotation>, Set<Class<? extends Object>>>();
        // CHECKSTYLE:ON
        Class<? extends Annotation>[] annotations = getAnnotationTypes();
        if (annotations == null) {
            return result;
        }
        int total = 0;
        for (Class<? extends Annotation> annotationType : annotations) {
            Type type = types.getBy(annotationType.getName());
            if (type instanceof AnnotationType) {
                Collection<AnnotatedElement> elements = ((AnnotationType) type).allAnnotatedTypes();
                for (AnnotatedElement element : elements) {
                    Type t;
                    if (element instanceof Member) {
                        t = ((Member) element).getDeclaringType();
                    } else {
                        t = (Type) element;
                    }
                    if (t.wasDefinedIn(uris)) {
                        Set<Class<? extends Object>> classes = result.get(annotationType);
                        if (classes == null) {
                            classes = new HashSet<Class<? extends Object>>();
                            result.put(annotationType, classes);
                        }
                        try {
                            final Class<?> aClass = cl.loadClass(t.getName());
                            LOGGER.log(Level.INFO, "{0} contains {1}", new Object[] { aClass, annotationType });
                            total++;
                            classes.add(aClass);
                        } catch (ClassNotFoundException e) {
                            LOGGER.log(Level.WARNING, "Not able to load " + t.getName(), e);
                        }
                    }
                }
            }
        }
        // TODO(Sahoo): change to finer
        LOGGER.log(Level.INFO, "total number of classes with faces annotation = {0}", total);
        return result;
    }

    /**
     * Get the annotation {@code Type} for the annotations to scan.
     * 
     * @return annotation types
     */
    @SuppressWarnings({ "unchecked", "deprecation", "checkstyle:magicnumber" })
    private static Class<Annotation>[] getAnnotationTypes() {
        HashSet<Class<? extends Annotation>> annotationInstances = new HashSet<Class<? extends Annotation>>(8, 1.0f);
        Collections.addAll(annotationInstances, FacesComponent.class, FacesConverter.class, FacesValidator.class, FacesRenderer.class,
                javax.faces.bean.ManagedBean.class, NamedEvent.class, FacesBehavior.class, FacesBehaviorRenderer.class);
        return annotationInstances.toArray(new Class[0]);
    }
}
