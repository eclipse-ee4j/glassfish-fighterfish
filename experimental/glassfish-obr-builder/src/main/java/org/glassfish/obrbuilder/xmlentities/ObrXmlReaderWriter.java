/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.obrbuilder.xmlentities;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.URI;

/**
 * Utility class to marshall and unmarshall to and from an obr.xml.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ObrXmlReaderWriter {
    /*
     * It uses JAXB to do the necessary XML marshalling/unmasrshalling
     */

    /**
     * Reads the XML document from the input stream and maps it to a Java object graph.
     * Closing the stream is caller's responsibility.
     *
     * @param is InputStream to read the XML content from
     * @return an inmemory representation of the XML content
     * @throws IOException
     */
    public Repository read(InputStream is) throws IOException {
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
            return Repository.class.cast(unmarshaller.unmarshal(is));
        } catch (JAXBException je) {
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }

    }

    /**
     * @see #read(java.io.InputStream)
     */
    public Repository read(URI input) throws IOException {
        InputStream is = new BufferedInputStream(input.toURL().openStream());
        try {
            return read(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * @see #read(java.io.InputStream) 
     */
    public Repository read(File input) throws IOException {
        return read(input.toURI());
    }

    /**
     * Writes  a Java object graph in XML format.
     * Closing the stream is caller's responsibility.
     *
     * @param repository Repository object to be written out
     * @param os         target stream to write to
     * @throws IOException
     */
    public void write(Repository repository, OutputStream os) throws IOException {
        try {
            getMarshaller(repository.getClass()).marshal(repository, os);
        } catch (JAXBException je) {
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }

    }

    /**
     * @see #write(Repository, java.io.OutputStream)
     */
    public void write(Repository repository, File out) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(out));
        try {
            write(repository, os);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
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
        return JAXBContext.newInstance(ObjectFactory.class);
    }


}
