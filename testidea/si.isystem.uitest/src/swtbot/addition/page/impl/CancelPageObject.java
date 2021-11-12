/*******************************************************************************
* Copyright (c) 2008 Ketan Padegaonkar and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Kay-Uwe Graw - initial API and implementation

*******************************************************************************/
package swtbot.addition.page.impl;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;

import swtbot.addition.page.ICancelPageObject;

/**
 * **
 * base class for a page object representing a shell with a title and a cancel button (Dialog or Wizard)
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 * 
 * @param <T> - the bot class
 */
public class CancelPageObject<T extends SWTBot> extends ClosingButtonPageObject<T> implements ICancelPageObject
{
	protected final String itsCancelButtonText;
	
	/**
	 * constructor for the page object
	 * @param bot - the bot
	 * @param title - the shell title
	 * @param cancelButtonText - the text for the cancel button
	 */
	public CancelPageObject(T bot, String title, String cancelButtonText)
	{
		super(bot, title);
		
		itsCancelButtonText = cancelButtonText;
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.ICancelPageObject#cancel()
	 */
	public void cancel()
	{
		clickClosingButton(cancelButton());
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.partmaster.swtbot.extended.page.ICancelPageObject#cancelButton()
	 */
	protected SWTBotButton cancelButton()
	{
		return itsBot.button(itsCancelButtonText);
	}
}