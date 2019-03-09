/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app16.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Entity implementation class for Entity: Message.
 */
@Entity
@SuppressWarnings("checkstyle:DesignForExtension")
public class Message implements Serializable {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Message id.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Message value.
     */
    private String val;

    /**
     * Create a new instance.
     */
    public Message() {
        super();
    }

    /**
     * Get the message id.
     * @return Long
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Set the message id.
     * @param newId id to set
     */
    public void setId(final Long newId) {
        this.id = newId;
    }

    /**
     * Get the message value.
     * @return String
     */
    public String getValue() {
        return this.val;
    }

    /**
     * Set the message value.
     * @param newValue value to set
     */
    public void setValue(final String newValue) {
        this.val = newValue;
    }
}
