/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package sahoo.metainfservicetest;

import javax.xml.bind.JAXBContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator.
 */
public final class Activator implements BundleActivator {

    @Override
    public void start(final BundleContext ctx) throws Exception {
        try {
            System.out.println("MY CLASSLOADER " + Thread.currentThread()
                    .getContextClassLoader());

            JAXBContext jc = JAXBContext.newInstance(Persistence.class);

            // create an object and marshall it.
            Persistence testObject = new Persistence();
            testObject.setVersion("3.0");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            jc.createMarshaller().marshal(testObject, out);

            // unmarshall it
            byte[] data = out.toByteArray();
            ByteArrayInputStream istream = new ByteArrayInputStream(data);
            Persistence unmarshalledObject = (Persistence)
                    jc.createUnmarshaller().unmarshal(istream);

            // ensure the version value remains the same
            final String unmarshalledVersion = unmarshalledObject
                    .getVersion();
             if ("3.0".equals(unmarshalledVersion)) {
                 System.out.println("Marshall and UnMarshall Success.");
             } else {
                 throw new RuntimeException(
                         "Marshall/UnMarshall of Persistence Object Failed.");
             }

        } catch (Exception e) {
             throw new RuntimeException(
                     "Marshall/UnMarshall of Persistence Object Failed.");
        }

        // This works, because GlassFish uses StAX from JRE.
        try {
            javax.xml.stream.XMLInputFactory.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop(final BundleContext ctx) {
    }
}
