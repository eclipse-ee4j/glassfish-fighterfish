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

package org.glassfish.fighterfish.test.app5;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import java.security.SecureRandom;
import java.util.Random;

/**
 *  Simple bean.
 */
public final class UserNumberBean {

    /**
     * The number.
     */
    private Integer userNumber = null;

    /**
     * Random int.
     */
    private Integer randomInt = null;

    /**
     * Statuses.
     */
    private String[] status = null;

    /**
     * Max.
     */
    private int max = 0;

    /**
     * Maximum set flag.
     */
    private boolean maxSet = false;

    /**
     * Minimum.
     */
    private int min = 0;

    /**
     * Minimum set flag.
     */
    private boolean minSet = false;

    /**
     * Create a new instance.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public UserNumberBean() {
        Random randomGR = new SecureRandom();
        do {
            this.randomInt = randomGR.nextInt(10);
        } while (this.randomInt == 0);
        System.out.println("Duke's number: " + this.randomInt);
    }

    /**
     * Set the user number.
     * @param number number to set
     */
    public void setUserNumber(final Integer number) {
        this.userNumber = number;
        System.out.println("Set userNumber " + this.userNumber);
    }

    /**
     * Get the user number.
     * @return Integer
     */
    public Integer getUserNumber() {
        System.out.println("get userNumber " + this.userNumber);
        return this.userNumber;
    }

    /**
     * Get the response.
     * @return String
     */
    public String getResponse() {
        if ((this.userNumber != null)
                && (this.userNumber.compareTo(this.randomInt) == 0)) {
            return "Yay! You got it!";
        }
        if (this.userNumber == null) {
            return "Sorry, " + this.userNumber
                    + " is incorrect. Try a larger number.";
        }
        int num = this.userNumber;
        if (num > this.randomInt) {
            return "Sorry, " + this.userNumber
                    + " is incorrect. Try a smaller number.";
        }
        return "Sorry, " + this.userNumber
                + " is incorrect. Try a larger number.";
    }

    /**
     * Get the status.
     * @return String[]
     */
    public String[] getStatus() {
        return this.status;
    }

    /**
     * Set the status.
     * @param newStatus the new status to set
     */
    public void setStatus(final String[] newStatus) {
        this.status = newStatus;
    }

    /**
     * Get the max.
     * @return int
     */
    public int getMaximum() {
        return this.max;
    }

    /**
     * Set the max.
     * @param newMax the new max to set
     */
    public void setMaximum(final int newMax) {
        this.max = newMax;
        this.maxSet = true;
    }

    /**
     * Get the min.
     * @return int
     */
    public int getMinimum() {
        return this.min;
    }

    /**
     * Set the min.
     * @param newMin the new min to set
     */
    public void setMinimum(final int newMin) {
        this.min = newMin;
        this.minSet = true;
    }

    /**
     * Message ID for not in range validation.
     */
    private static final String NOT_IN_RANGE =
            "javax.faces.validator.LongRangeValidator.NOT_IN_RANGE";

    /**
     * Message id for maximum validation.
     */
    private static final String MAXIMUM =
            "javax.faces.validator.LongRangeValidator.MAXIMUM";

    /**
     * Message id for minimum validation.
     */
    private static final String MINIMUM =
            "javax.faces.validator.LongRangeValidator.MINIMUM";

    /**
     * Message id for type validation.
     */
    private static final String TYPE =
            "javax.faces.validator.LongRangeValidator.TYPE";

    /**
     * Valid the UI component.
     * @param ctx faces context
     * @param comp UI component
     * @param value user number
     * @throws ValidatorException if an error occurs
     */
    public void validate(final FacesContext ctx, final UIComponent comp,
            final Object value) throws ValidatorException {
        if ((ctx == null) || (comp == null)) {
            throw new NullPointerException();
        }
        if (value != null) {
            try {
                int converted = intValue(value);
                if ((this.maxSet) && (converted > this.max)) {
                    if (this.minSet) {
                        throw new ValidatorException(
                                MessageFactory.getMessage(ctx, NOT_IN_RANGE,
                                        new Object[]{this.min, this.max,
                                            MessageFactory.getLabel(ctx, comp)
                                        }));
                    }
                    throw new ValidatorException(
                            MessageFactory.getMessage(ctx, MAXIMUM,
                                    new Object[]{this.max,
                                        MessageFactory.getLabel(ctx, comp)
                                    }));
                }
                if ((this.minSet) && (converted < this.min)) {
                    if (this.maxSet) {
                        throw new ValidatorException(
                                MessageFactory.getMessage(ctx, NOT_IN_RANGE,
                                        new Object[]{new Double(this.min),
                                            new Double(this.max),
                                            MessageFactory.getLabel(ctx, comp)
                                        }));
                    }
                    throw new ValidatorException(
                            MessageFactory.getMessage(ctx, MINIMUM,
                                    new Object[]{this.min,
                                        MessageFactory.getLabel(ctx, comp)
                                    }));
                }
            } catch (NumberFormatException e) {
                throw new ValidatorException(MessageFactory.getMessage(ctx,
                        TYPE,
                        new Object[]{MessageFactory.getLabel(ctx, comp)}));
            }
        }
    }

    /**
     * Convert an object to an integer.
     * @param value the object to convert
     * @return int
     * @throws NumberFormatException if an error occurs
     * @throws IllegalArgumentException if value is {@code null}
     */
    private int intValue(final Object value) throws NumberFormatException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if ((value instanceof Number)) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
