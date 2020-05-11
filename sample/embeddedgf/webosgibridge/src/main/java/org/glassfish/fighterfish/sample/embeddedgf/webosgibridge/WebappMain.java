/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.embeddedgf.webosgibridge;

import javax.annotation.Resource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Main entry point.
 */
@WebListener
public final class WebappMain implements ServletContextListener {

    /**
     * OSGi framework provisioner.
     */
    private OSGiFrameworkProvisioner frameworkProvisioner;

    /**
     * OSGi framework JDNI name.
     *
     * @see OSGiFrameworkProvisioner#osgiFrameWorkJndiName
     */
    private String osgiFrameWorkJndiName = OSGiFrameworkProvisioner.FW_JNDI_NAME_DEFAULT;

    /**
     * OSGi framework config file path.
     *
     * @see OSGiFrameworkProvisioner#osgiFrameworkConfigFilePath
     */
    private String osgiFrameworkConfigFilePath = OSGiFrameworkProvisioner.FW_CONFIG_FILE_DEFAULT;

    /**
     * Set the OSGi framework name.
     *
     * @param jndiName JNDI name
     */
    @Resource
    public void setOsgiFrameWorkJndiName(final String jndiName) {
        this.osgiFrameWorkJndiName = jndiName;
    }

    /**
     * Set the OSGi framework config file path.
     *
     * @param configFilePath config file path
     */
    @Resource
    public void setOsgiFrameworkConfigFilePath(final String configFilePath) {

        this.osgiFrameworkConfigFilePath = configFilePath;
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {

        frameworkProvisioner = new OSGiFrameworkProvisioner(servletContextEvent.getServletContext(), osgiFrameworkConfigFilePath, osgiFrameWorkJndiName);
        frameworkProvisioner.start();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {

        frameworkProvisioner.stop();
        frameworkProvisioner = null;
    }
}
