/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.app1;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.sql.DataSource;

/**
 * Session Bean implementation class UserAuthServiceEJB
 */
@Stateless
@LocalBean
public class UserAuthServiceEJB 
{

    @Resource(mappedName= TestApp1Activator.dsName)
    private DataSource ds;

    @PostConstruct
    public void postConstruct() {
        System.out.println("UserAuthServiceEJB.postConstruct");
    }

    public boolean login(String name, String password)
    {
        System.out.println("UserAuthServiceEJBuser: logging in " + name);
        Connection c = null;
        Statement s = null;
        try
        {
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "select count(*) as record_count from " +
                    TestApp1Activator.tableName +" where name = '" + name +
                    "' and password= '" + password + "'";
            System.out.println("sql = " + sql);
            ResultSet rs = s.executeQuery(sql);
            rs.next();
            if (rs.getInt("record_count") == 1) {
                System.out.println("Login successful");
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (c!= null) c.close();
                if (s!=null) s.close();
            }
            catch (Exception e)
            {
            }
        }
        return false;
    }

    public boolean register(String name, String password)
    {
        System.out.println("UserAuthServiceEJB: registering " + name);
        Connection c = null;
        Statement s = null;
        try
        {
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "insert into " + TestApp1Activator.tableName +
                    " values('" + name + "', '" + password + "')";
            System.out.println("sql = " + sql);
            s.executeUpdate(sql);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (c!= null) c.close();
                if (s!=null) s.close();
            }
            catch (Exception e)
            {
            }
        }
        return false;
    }
}
