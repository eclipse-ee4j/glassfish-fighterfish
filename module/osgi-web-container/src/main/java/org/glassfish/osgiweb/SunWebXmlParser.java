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

package org.glassfish.osgiweb;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import static javax.xml.stream.XMLStreamConstants.*;
import java.io.InputStream;

/**
 * A mini parser to parse sun-web.xml and glassfish-web.xml for entries of interest 
 * to us.
 * Currently, we only read context-root value.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class SunWebXmlParser
{
    private static XMLInputFactory xmlIf = null;

    static {
        xmlIf = XMLInputFactory.newInstance();
        xmlIf.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    String contextRoot;

    /**
     * The caller should close the input stream.
     * @param in InputStream for sun-web.xml or glassfish-web.xml
     */
    SunWebXmlParser(InputStream in) throws XMLStreamException
    {
        XMLStreamReader reader = xmlIf.createXMLStreamReader(in);
        try {
            int event;
            while (reader.hasNext() && (event = reader.next()) != END_DOCUMENT) {
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

    public String getContextRoot()
    {
        return contextRoot;
    }
}
