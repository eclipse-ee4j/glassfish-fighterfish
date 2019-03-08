/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.util;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.BundleContext;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EnterpriseResourceProvisioner {

    /**
     * List of config changes made by a test method.
     */
    protected List<RestorableDomainConfiguration> rdcs =
            new ArrayList<RestorableDomainConfiguration>();
    private boolean inMemoryDerbyDb;
    private File derbyDbRootDir;

    private static final Logger LOGGER = Logger.getLogger(
            EnterpriseResourceProvisioner.class.getPackage().getName());

    public EnterpriseResourceProvisioner(BundleContext ctx) {
        String dbRootDir = ctx.getProperty(
                Constants.FIGHTERFISH_TEST_DERBY_DB_ROOT_DIR);
        if (dbRootDir == null || dbRootDir.isEmpty()) {
            inMemoryDerbyDb = true;
        } else {
            this.derbyDbRootDir = new File(dbRootDir);
            if (!this.derbyDbRootDir.isDirectory()
                    && !this.derbyDbRootDir.mkdirs()) {
                throw new RuntimeException("Can't create a directory called "
                        + this.derbyDbRootDir.getAbsolutePath());
            }
        }
    }

    private File getDerbyDBRootDir() {
        return derbyDbRootDir;
    }

    private boolean isInmemoryDerbyDb() {
        return inMemoryDerbyDb;
    }

    protected void restoreDomainConfiguration() throws GlassFishException {
        for (RestorableDomainConfiguration rdc : rdcs) {
            try {
                rdc.restore();
            } catch (GlassFishException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Configures jdbc/__default datasource to use a custom pool that uses
     * embedded derby.
     * @param gf
     * @param poolName
     * @param db
     * @return
     * @throws org.glassfish.embeddable.GlassFishException
     */
    protected RestorableDomainConfiguration configureEmbeddedDerby(
            final GlassFish gf, final String poolName, String db)
            throws GlassFishException {

        if (isInmemoryDerbyDb()) {
            createPoolForInmemoryEmbeddedDerbyDb(gf, poolName, db);
        } else {
            createPoolForEmbeddedDerbyDb(gf, poolName, db);
        }
        final String poolNameProperty = "resources.jdbc-resource."
                + Constants.DEFAULT_DS + ".pool-name";
        execute(gf, "set", poolNameProperty + "=" + poolName);
        final RestorableDomainConfiguration rdc =
                new RestorableDomainConfiguration() {
            @Override
            public void restore() throws GlassFishException {
                CommandResult result = gf.getCommandRunner().run("set",
                        poolNameProperty + "=" + Constants.DEFAULT_POOL);
                if (result.getExitStatus() == CommandResult.ExitStatus.FAILURE) {
                    Assert.fail(result.getOutput());
                }
                result = gf.getCommandRunner().run("delete-jdbc-connection-pool",
                        poolName);
                if (result.getExitStatus() == CommandResult.ExitStatus.FAILURE) {
                    Assert.fail(result.getOutput());
                }
            }
        };
        rdcs.add(rdc);
        return rdc;
    }

    /**
     * This method creates a connection pool that uses embedded Derby driver to
     * talk to a directory based Derby database.
     * @param gf GlassFish object
     * @param poolName name of connection pool
     * @param db database name
     * @throws GlassFishException
     */
    private void createPoolForEmbeddedDerbyDb(GlassFish gf, String poolName,
            String db)
            throws GlassFishException {

        String dbDir = new File(getDerbyDBRootDir(), db).getAbsolutePath();
        if (System.getProperty("os.name", "generic").toLowerCase()
                .startsWith("windows")) {
            // We need to escape : as well as backslashes.
            // So, it should something like like C\:\\temp\\foo
            dbDir = dbDir.replace("\\", "\\\\").replace(":", "\\:");
        }
        final String poolProps = "databaseName=" + dbDir + ":"
                + "connectionAttributes=;create\\=true";
        execute(gf,
                "create-jdbc-connection-pool",
                "--ping",
                "--restype=javax.sql.XADataSource",
                "--datasourceclassname=org.apache.derby.jdbc.EmbeddedXADataSource",
                "--property",
                poolProps,
                poolName);
    }

    /**
     * This method creates a connection pool that uses embedded Derby driver to
     * talk to an in-memory Derby database.
     * @param gf GlassFish object
     * @param poolName name of connection pool
     * @param db name of the database
     * @throws GlassFishException
     */
    private void createPoolForInmemoryEmbeddedDerbyDb(GlassFish gf,
            String poolName, String db)
            throws GlassFishException {

        // According to Derby guide available at
        // http://db.apache.org/derby/docs/10.7/devguide/cdevdvlpinmemdb.html#cdevdvlpinmemdb ,
        // an in-memory databae url is of the form: jdbc:derby:memory:db;create=true
        // The above syntax works if we create a java.sql.Driver type resource, but not with DataSource type.
        // That's because url is not a valid property while creating DataSource type resource.
        // So, we use memory protocol in databaseName.
        String poolProps = "databaseName=memory\\:" + db + ":"
                + "connectionAttributes=;create\\=true";
        execute(gf,
                "create-jdbc-connection-pool",
                "--ping",
                "--restype=javax.sql.XADataSource",
                "--datasourceclassname=org.apache.derby.jdbc.EmbeddedXADataSource",
                "--property",
                poolProps,
                poolName);
    }

    protected RestorableDomainConfiguration createJmsCF(final GlassFish gf,
            final String cfName)
            throws GlassFishException {

        final RestorableDomainConfiguration rdc = createJmsResource(gf, cfName,
                "javax.jms.ConnectionFactory");
        rdcs.add(rdc);
        return rdc;
    }

    protected RestorableDomainConfiguration createJmsTopic(final GlassFish gf,
            final String topicName)
            throws GlassFishException {

        final RestorableDomainConfiguration rdc = createJmsResource(gf,
                topicName, "javax.jms.Topic");
        rdcs.add(rdc);
        return rdc;
    }

    protected RestorableDomainConfiguration createJmsQueue(final GlassFish gf,
            final String topicName)
            throws GlassFishException {

        final RestorableDomainConfiguration rdc = createJmsResource(gf,
                topicName, "javax.jms.Queue");
        rdcs.add(rdc);
        return rdc;
    }

    private RestorableDomainConfiguration createJmsResource(final GlassFish gf,
            final String resName, final String resType)
            throws GlassFishException {

        execute(gf, "create-jms-resource", "--restype", resType, resName);
        return new RestorableDomainConfiguration() {
            @Override
            public void restore() throws GlassFishException {
                gf.getCommandRunner().run("delete-jms-resource", resName);
            }
        };
    }

    private CommandResult execute(GlassFish gf, String cmd, String... args)
            throws GlassFishException {

        LOGGER.logp(Level.INFO, "EnterpriseResourceProvisioner", "execute",
                "cmd = {0}, args = {1}",
                new Object[]{cmd, Arrays.toString(args)});
        CommandResult result = gf.getCommandRunner().run(cmd, args);
        if (result.getExitStatus() == CommandResult.ExitStatus.FAILURE) {
            Assert.fail(result.getOutput());
        }
        return result;
    }

}
