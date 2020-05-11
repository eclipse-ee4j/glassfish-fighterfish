/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.app8.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Employee entity.
 */
@Entity
@SuppressWarnings("checkstyle:DesignForExtension")
public class Employee implements java.io.Serializable {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = -6829839191194419668L;

    /**
     * Employee id.
     */
    private int id;

    /**
     * Employee department.
     */
    private Department department;

    /**
     * Get the employee id.
     * @return int
     */
    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    /**
     * Set the employee id.
     * @param newId the new employee id
     */
    public void setId(final int newId) {
        this.id = newId;
    }

    /**
     * Get the employee department.
     * @return Department
     */
    @ManyToOne(fetch = FetchType.LAZY)
    public Department getDepartment() {
        return department;
    }

    /**
     * Set the employee department.
     * @param newDept the new department
     */
    public void setDepartment(final Department newDept) {
        this.department = newDept;
    }

    @Override
    public String toString() {
        return "Employee(" + id
        // + ", Department(" + (department != null ? (department.getName() +
        // ", " + department.getEmployees().size()) : "null") + ")
                + ")";
    }
}
