/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app20;

import java.util.Set;

/**
 * A service that provides quotes for a provided symbol.
 */
public interface StockQuoteService {

    /**
     * Get a quote.
     * @param symbol stock symbol
     * @return Double
     */
    Double getQuote(String symbol);

    /**
     * Get all symbols.
     * @return set of symbol
     */
    Set<String> getSymbols();

    /**
     * Used For Tesing GLASSFISH-18370.
     * @return set of {@code null} symbol
     */
    Set<String> getNullSymbols();
}
