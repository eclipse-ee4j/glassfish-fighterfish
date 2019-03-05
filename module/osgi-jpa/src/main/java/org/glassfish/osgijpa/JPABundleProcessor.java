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
package org.glassfish.osgijpa;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.glassfish.osgijpa.dd.PersistenceXMLReaderWriter;
import org.glassfish.osgijpa.dd.Persistence;
import org.glassfish.osgijavaeebase.BundleResource;
import org.glassfish.osgijavaeebase.OSGiBundleArchive;
import org.osgi.framework.BundleReference;

import java.io.Serializable;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.IOException;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class JPABundleProcessor implements Serializable {

    private static final Logger LOGGER
            = Logger.getLogger(JPABundleProcessor.class.getPackage().getName());

    public static final String PXML_PATH = "META-INF/persistence.xml";

    private static final String ECLIPSELINK_JPA_PROVIDER
            = "org.eclipselink.jpa.PersistenceProvider";

    // A marker header to indicate that a bundle has been statically weaved
    // This is used to avoid updating infinitely
    public static final String STATICALLY_WEAVED = "GlassFish-StaticallyWeaved";

    // store the id so that we don't have a hard reference to bundle
    private final long bundleId;

    private List<Persistence> persistenceXMLs;
    private static final long serialVersionUID = -1293408086392301220L;

    JPABundleProcessor(Bundle b) {
        this.bundleId = b.getBundleId();
    }

    boolean isJPABundle() {
        if (persistenceXMLs == null) {
            discoverPxmls();
        }
        return !persistenceXMLs.isEmpty();
    }

    void discoverPxmls() {
        assert (persistenceXMLs == null);
        persistenceXMLs = new ArrayList<Persistence>();
        if (isFragment()) {
            return;
        }
        for (BundleResource r : new OSGiBundleArchive(getBundle())) {
            if (PXML_PATH.equals(r.getPath())) {
                URL pxmlURL;
                try {
                    pxmlURL = r.getUri().toURL();
                } catch (MalformedURLException e) {
                    // TODO(Sahoo): Proper Exception Handling
                    throw new RuntimeException(e);
                }
                InputStream is = null;
                try {
                    is = pxmlURL.openStream();
                    Persistence persistence = new PersistenceXMLReaderWriter()
                            .read(is);
                    persistence.setUrl(pxmlURL);
                    persistence.setPURoot(r.getArchivePath());
                    persistenceXMLs.add(persistence);
                } catch (IOException ioe) {
                    LOGGER.logp(Level.WARNING, "JPABundleProcessor",
                            "discoverPxmls",
                            "Exception occurred while processing " + pxmlURL,
                            ioe);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ioe) {
                        }
                    }
                }
            }
        }
    }

    boolean validate(List<Persistence> persistenceList) {
        for (Persistence persistence : persistenceList) {
            for (Persistence.PersistenceUnit pu : persistence
                    .getPersistenceUnit()) {
                if (pu.getProvider() == null) {
                    continue;
                }
                if (ECLIPSELINK_JPA_PROVIDER.equals(pu.getProvider())) {
                    return false;
                } else {
                    LOGGER.logp(Level.INFO, "JPABundleProcessor", "validate",
                            "{0} has a persistence-unit which does not use {1}"
                            + " as provider",
                            new Object[]{persistence, ECLIPSELINK_JPA_PROVIDER});
                }

            }
        }
        return true;
    }

    InputStream enhance() throws BundleException, IOException {
        JPAEnhancer enhancer = new EclipseLinkEnhancer();
        InputStream enhancedStream = enhancer.enhance(getBundle(),
                persistenceXMLs);
        return enhancedStream;
    }

    public boolean isEnhanced() {
        return getBundle().getHeaders().get(STATICALLY_WEAVED) != null;
    }

    private boolean isFragment() {
        return getBundle().getHeaders()
                .get(org.osgi.framework.Constants.FRAGMENT_HOST) != null;
    }

    public Bundle getBundle() {
        Bundle b = getBundleContext().getBundle(bundleId);
        if (b == null) {
            throw new RuntimeException("Bundle with id " + bundleId
                    + " has already been uninstalled");
        }
        return b;
    }

    private BundleContext getBundleContext() {
        return BundleReference.class.cast(getClass().getClassLoader())
                .getBundle().getBundleContext();
    }

    /* package */ long getBundleId() {
        return bundleId;
    }
}
