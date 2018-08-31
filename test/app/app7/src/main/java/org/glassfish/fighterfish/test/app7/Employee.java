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

package org.glassfish.fighterfish.test.app7;

/**
 * @author sanjeeb.sahoo@oracle.com
 *
 */
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Employee implements java.io.Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -6829839191194419668L;
    int id;
    Department department;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department u) {
        this.department = u;
    }

    @Override
    public String toString() {
        return "Employee(" + id
        // + ", Department(" + (department != null ? (department.getName() +
        // ", " + department.getEmployees().size()) : "null") + ")
                + ")";
    }
}
