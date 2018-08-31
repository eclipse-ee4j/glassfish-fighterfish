/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.fighterfish.sample.embeddedgf.webosgibridge;


import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author sanjeeb.sahoo@oracle.com
 */

@WebListener
public class WebappMain implements ServletContextListener {
    private OSGiFrameworkProvisioner frameworkProvisioner;

    /**
     * @see OSGiFrameworkProvisioner#osgiFrameWorkJndiName
     */
    private String osgiFrameWorkJndiName = OSGiFrameworkProvisioner.FW_JNDI_NAME_DEFAULT;

    /**
     * @see OSGiFrameworkProvisioner#osgiFrameworkConfigFilePath
     */
    private String osgiFrameworkConfigFilePath = OSGiFrameworkProvisioner.FW_CONFIG_FILE_DEFAULT;

    @Resource
    public void setOsgiFrameWorkJndiName(String osgiFrameWorkJndiName) {
        this.osgiFrameWorkJndiName = osgiFrameWorkJndiName;
    }

    @Resource
    public void setOsgiFrameworkConfigFilePath(String osgiFrameworkConfigFilePath) {
        this.osgiFrameworkConfigFilePath = osgiFrameworkConfigFilePath;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        frameworkProvisioner = new OSGiFrameworkProvisioner(servletContextEvent.getServletContext(),
                osgiFrameworkConfigFilePath,
                osgiFrameWorkJndiName);
        frameworkProvisioner.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        frameworkProvisioner.stop();
        frameworkProvisioner = null;
    }
}
