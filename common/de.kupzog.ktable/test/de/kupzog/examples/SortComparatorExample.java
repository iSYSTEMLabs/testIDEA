/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.kupzog.examples;

import de.kupzog.ktable.KTableSortComparator;
import de.kupzog.ktable.KTableSortedModel;

/**
 * 
 * @author Lorenz Maierhofer <lorenz.maierhofer@logicmindguide.com>
 */
public class SortComparatorExample extends KTableSortComparator {

    public SortComparatorExample(KTableSortedModel model, int columnIndex, int direction) {
        super(model, columnIndex, direction);
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableSortComparator#doCompare(java.lang.Object, java.lang.Object, int, int)
     */
    public int doCompare(Object o1, Object o2, int row1, int row2) {
        String s1 = (String)o1;
        int v1;
        try {
             v1 = Integer.parseInt(s1.substring(0, s1.indexOf(' ')));
        } catch (NumberFormatException ex) {
            return 1;
        }
        
        String s2 = (String)o2;
        int v2;
        try {
            v2 = Integer.parseInt(s2.substring(0, s2.indexOf(' ')));
        } catch (NumberFormatException ex) {
            return -1;
        }
        
        if (v1<v2) return -1;
        if (v1>v2) return +1;
        return 0;
    }

}
