/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * A service that provides quotes for a provided symbol 
 * [the osgi service interface]
 * 
 * @author Sivakumar Thyagarajan
 * @author Tang Yong
 */
public interface StockQuoteService {
    public Double getQuote(String symbol);
    public Set<String> getSymbols();
    
    //Used For Tesing GLASSFISH-18370
    public Set<String> getNullSymbols();
}
