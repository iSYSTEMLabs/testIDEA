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

import swtbot.addition.page.IConfirmPageObject;

/**
 * base class for a page object representing a shell with a confirmation button (dialog or wizard)
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 * 
 * @param <T> - the bot class
 */
public class ConfirmPageObject<T extends SWTBot> extends ClosingButtonPageObject<T> implements IConfirmPageObject
{
	protected final String itsConfirmButtonText;

	/**
	 * constructor for the page object
	 * @param bot - the bot
	 * @param title - the shell title
	 * @param confirmButtonText - the text for the confirm button
	 */
	public ConfirmPageObject(T bot, String title, String confirmButtonText)
	{
		super(bot, title);
		
		itsConfirmButtonText = confirmButtonText;
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IConfirmPageObject#confirm()
	 */
	public void confirm()
	{
		clickClosingButton(confirmButton());
	}
	
	/*
	 * 
	 */
	protected SWTBotButton confirmButton()
	{
		return itsBot.button(itsConfirmButtonText);
	}
}
