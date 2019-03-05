/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgijpa.dd;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ClassLoaderHierarchy;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceXMLReaderWriter
{
    private static final Logger LOGGER = Logger.getLogger(
            PersistenceXMLReaderWriter.class.getPackage().getName());

    public Persistence read(URL pxmlURL) throws IOException
    {
        InputStream is = pxmlURL.openStream();
        try {
            return read(is);
        } finally {
            is.close();
        }
    }

    public Persistence read(InputStream is) throws IOException {
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
            return (Persistence) unmarshaller.unmarshal(is);
        } catch (JAXBException je) {
            je.printStackTrace();
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }
    }

    public void write(Persistence persistence, OutputStream os)
            throws IOException {
        try {
            getMarshaller(persistence.getClass()).marshal(persistence,
                    os);
        } catch (JAXBException je) {
            je.printStackTrace();
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }
    }

    public void write(Persistence persistence, Writer writer)
            throws IOException {
        try {
            getMarshaller(persistence.getClass()).marshal(persistence,
                    writer);
        } catch (JAXBException je) {
            je.printStackTrace();
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }
    }

    private Marshaller getMarshaller(Class<?> clazz) throws JAXBException {
         JAXBContext jc = getJAXBContext();
         Marshaller marshaller = jc.createMarshaller();
         marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                 Boolean.TRUE);
         return marshaller;
     }

    private Unmarshaller getUnmarshaller() throws JAXBException {
        JAXBContext jc = getJAXBContext();
        return jc.createUnmarshaller();
    }

    private JAXBContext getJAXBContext() throws JAXBException {
        // We need to set context class loader to be CommonClassLoader,
        // otherwise our stupid JAXB implementation
        // won't be able to locate the default JAXB context factory class.
        final Thread thread = Thread.currentThread();
        ClassLoader oldCL = thread.getContextClassLoader();
        try {
            ClassLoader ccl = Globals.get(ClassLoaderHierarchy.class)
                    .getCommonClassLoader();
            thread.setContextClassLoader(ccl);
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
        return jc;
        } finally {
            thread.setContextClassLoader(oldCL);
        }
    }

}
