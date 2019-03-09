/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * A mini parser to parse sun-web.xml and glassfish-web.xml for entries of
 * interest to us. Currently, we only read context-root value.
 */
final class SunWebXmlParser {

    /**
     * XML input factory.
     */
    private static final XMLInputFactory XMLIF;

    static {
        XMLIF = XMLInputFactory.newInstance();
        XMLIF.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /**
     * Context root.
     */
    private String contextRoot;

    /**
     * The caller should close the input stream.
     * @param in InputStream for sun-web.xml or glassfish-web.xml
     * @throws XMLStreamException if a parsing error occurs
     */
    @SuppressWarnings("checkstyle:innerassignment")
    SunWebXmlParser(final InputStream in) throws XMLStreamException {
        XMLStreamReader reader = XMLIF.createXMLStreamReader(in);
        try {
            int event;
            while (reader.hasNext() && (event = reader.next())
                    != END_DOCUMENT) {
                if (event == START_ELEMENT) {
                    String element = reader.getLocalName();
                    if (element.equals("context-root")) {
                        contextRoot = reader.getElementText();
                        break;
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Get the context root.
     * @return context root
     */
    public String getContextRoot() {
        return contextRoot;
    }
}
