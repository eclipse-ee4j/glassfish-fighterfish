/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.osgi.framework.*;
import javax.xml.bind.JAXBContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Activator implements BundleActivator {
    public void start(BundleContext ctx) throws Exception {
        try {
            System.out.println("MY CLASSLOADER " + Thread.currentThread().getContextClassLoader());

            JAXBContext jc = JAXBContext.newInstance(Persistence.class);
            Persistence test_object = new Persistence();
            test_object.setVersion("3.0");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            jc.createMarshaller().marshal(test_object,out);
            byte[] data = out.toByteArray();
            ByteArrayInputStream istream = new ByteArrayInputStream(data);
            Persistence out_object = (Persistence) jc.createUnmarshaller().unmarshal(istream);

            final String O2_version = out_object.getVersion();

             if("3.0".equals(O2_version)){System.out.println("Marshall and UnMarshall Success.");}

             else { throw new RuntimeException("Marshall/UnMarshall of Persistence Object Failed.");}

        } catch(Exception e) {
             throw new RuntimeException("Marshall/UnMarshall of Persistence Object Failed.");
        }

        // This works, because GlassFish uses StAX from JRE.
        try {
            javax.xml.stream.XMLInputFactory.newInstance();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
 
    public void stop(BundleContext ctx) {
    }
}
