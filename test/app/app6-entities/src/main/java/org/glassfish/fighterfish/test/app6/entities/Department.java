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

package org.glassfish.fighterfish.test.app6.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Department entity.
 */
@Entity
@SuppressWarnings("checkstyle:DesignForExtension")
public class Department implements java.io.Serializable {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = -6707620916711314356L;

    /**
     * Department name.
     */
    // Absence of @GeneratedValue annotation means
    // user is responsible for setting id.
    // name is the PK of this entity.
    @Id
    private String name;

    /**
     * Employees in the department.
     */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Collection<Employee> employees = new ArrayList<Employee>();

    /**
     * Create a new instance.
     */
    protected Department() {
        // Every entity bean must have a default public or protected
        // constructor
    }

    /**
     * Create a new instance.
     * @param depName department name
     */
    public Department(final String depName) {
        if (depName == null || depName.length() == 0) {
            throw new IllegalArgumentException("name is empty");
        }
        this.name = depName;
    }

    /**
     * Get the department name.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Add an employee to the department.
     * @param emp employee to add
     */
    public void addEmployee(final Employee emp) {
        employees.add(emp);
    }

    /**
     * Get the employees in the department.
     * @return collection of employee
     */
    public Collection<Employee> getEmployees() {
        return employees;
    }

    @Override
    public String toString() {
        return "Department(" + name + ", " + employees + ")";
    }
}
