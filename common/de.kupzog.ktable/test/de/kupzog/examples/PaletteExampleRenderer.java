/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
    
    Author: Friederich Kupzog  
    fkmk@kupzog.de
    www.kupzog.de/fkmk
*/
package de.kupzog.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModel;
//import org.eclipse.swt.widgets.*;

/**
 * @author Friederich Kupzog
 */
public class PaletteExampleRenderer implements KTableCellRenderer {

	/**
	 * 
	 */
	public PaletteExampleRenderer() {
	}
	
	/* 
	 * overridden from superclass
	 */
	public int getOptimalWidth(
		GC gc,
		int col,
		int row,
		Object content,
		boolean fixed, 
		KTableModel model) 
	{
		return 16;
	}

	
	/* 
	 * overridden from superclass
	 */
	public void drawCell(
		GC gc,
		Rectangle rect,
		int col,
		int row,
		Object content,
		boolean focus,
		boolean fixed,
		boolean clicked, 
		KTableModel model)
	{
		// Performance test:
		/*
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		gc.fillRectangle(rect);
		
		int j=1;
		for (int i = 0; i < 10000000; i++) {
			j++;
		}
		*/
		Color color = new Color(Display.getCurrent(),(RGB)content);
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		rect.height++;
		rect.width++;
		gc.fillRectangle(rect);
		
		gc.setBackground(color);
		if (!focus)
		{
			rect.x += 1;
			rect.y += 1;
			rect.height -= 2;
			rect.width -= 2;
		}
		gc.fillRectangle(rect);
		color.dispose();
	}

    @Override
    public void setEnabled(boolean isEnabled) {
    }


}
