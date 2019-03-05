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

package org.glassfish.fighterfish.test.app6.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author sanjeeb.sahoo@oracle.com
 * 
 */
@Entity
public class Department implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6707620916711314356L;

    // Absence of @GeneratedValue annotation means
    // user is responsible for setting id.
    @Id
    // name is the PK of this entity.
    private String name;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Collection<Employee> employees = new ArrayList<Employee>();

    protected Department() {
        // Every entity bean must have a default public or protected constructor
    }

    public Department(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name is empty");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addEmployee(Employee e) {
        employees.add(e);
    }

    public Collection<Employee> getEmployees() {
        return employees;
    }

    @Override
    public String toString() {
        return "Department(" + name + ", " + employees + ")";
    }
}
