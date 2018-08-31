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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple implementation of the <code>StockQuoteService</code>
 * using cached values of the quotes for a pre-defined set of symbols
 * [the osgi service implementation]
 * 
 * @author Sivakumar Thyagarajan
 * @author Tang Yong
 */
public class SimpleStockQuoteServiceImpl implements StockQuoteService{
    private Map<String, Double> quotes = new HashMap<String, Double>();
    public SimpleStockQuoteServiceImpl(){
        System.out.println("SimpleStockQuoteServiceImpl::Initializing quotes");
        quotes.put("ORCL", new Double("28.02"));
        quotes.put("HPQ", new Double("42.59"));
        quotes.put("IBM", new Double("144.52"));
        quotes.put("MSFT", new Double("25.59"));
    }

    public Double getQuote(String symbol) {
        System.out.println("SimpleStockQuoteServiceImpl::returning quotes for" + symbol);
        return quotes.get(symbol);
    }
    
    
    public Set<String> getSymbols() {
        System.out.println("SimpleStockQuoteServiceImpl::getSymbols");
        return quotes.keySet();
    }

	public Set<String> getNullSymbols() {
		quotes = null;
		return quotes.keySet();
	}

}
