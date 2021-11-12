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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.SWTX;

/**
 * @author Friederich Kupzog
 */
public class TownExampleRenderer implements KTableCellRenderer {

	protected Display m_Display;
	
	
	public TownExampleRenderer() 
	{
		m_Display = Display.getCurrent();
	}
	
	public int getOptimalWidth(
		GC gc, 
		int col, 
		int row, 
		Object content, 
		boolean fixed, 
		KTableModel model)
	{
		return Math.max(gc.stringExtent(content.toString()).x + 8, 120);
	}
	
	
	public void drawCell(GC gc, 
		Rectangle rect, 
		int col, 
		int row, 
		Object content, 
		boolean focus, 
		boolean fixed,
		boolean clicked, 
		KTableModel model)
	{
		Color textColor;
		Color backColor;
		Color borderColor;
		TownExampleContent myContent = (TownExampleContent)content;

		if (focus) {
			textColor = m_Display.getSystemColor(SWT.COLOR_BLUE);
		} 
		else
		{
			textColor = m_Display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		}
		backColor = (m_Display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		borderColor = m_Display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		
		gc.setForeground(borderColor);
		gc.drawLine(rect.x,rect.y+rect.height,rect.x+rect.width,rect.y+rect.height);

		gc.setForeground(borderColor);
		gc.drawLine(rect.x+rect.width,rect.y,rect.x+rect.width,rect.y+rect.height);
	
		if (col == 0)
		{
			gc.setBackground(m_Display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			textColor = m_Display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
			gc.setForeground(textColor);
			
			
			gc.drawImage((myContent.image),rect.x, rect.y);
	
			rect.y += 120;
			rect.height -= 120;
			gc.fillRectangle(rect);
			gc.drawText((myContent.name),rect.x+25, rect.y+2);
		}


		else if (col == 1)
		{
			gc.setBackground(backColor);
			gc.setForeground(textColor);
			
			gc.fillRectangle(rect);

			SWTX.drawTextImage(
				gc,
				myContent.country,
				SWTX.ALIGN_HORIZONTAL_LEFT | SWTX.ALIGN_VERTICAL_TOP,
				null,
				SWTX.ALIGN_HORIZONTAL_LEFT | SWTX.ALIGN_VERTICAL_CENTER,
				rect.x+3,
				rect.y,
				rect.width-3,
				rect.height
				);
			
		}
		
		else if (col == 2)
		{
			gc.setBackground(backColor);
			gc.setForeground(textColor);
			
			gc.fillRectangle(rect);
			Rectangle save = gc.getClipping();
			gc.setClipping(rect);
			gc.drawText((myContent.notes),rect.x+3, rect.y);
			gc.setClipping(save);
			
		}	
	}

    @Override
    public void setEnabled(boolean isEnabled) {
        // TODO Auto-generated method stub
        
    }
}
