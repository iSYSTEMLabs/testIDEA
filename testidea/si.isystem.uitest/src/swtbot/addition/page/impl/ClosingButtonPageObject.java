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

import swtbot.addition.condition.WidgetEnabledCondition;

/**
 * base class for a page object representing a shell with a button which closes the shell
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 * @param <T> - the bot class
 */
public abstract class ClosingButtonPageObject<T extends SWTBot> extends ShellPageObject<T>
{
	/**
	 * constructor 
	 * @param bot - the bot
	 * @param title - the title of the shell
	 */
	public ClosingButtonPageObject(T bot, String title)
	{
		super(bot, title);
	}
	

	/**
	 * click a button in the shell, which closes the shell
	 * @param button - the closing button
	 */
	protected void clickClosingButton(SWTBotButton button)
	{
		clickButton(button);
		
		waitForPageToClose();
	}
	
	protected void clickButton(SWTBotButton button)
	{
		itsBot.waitUntil(new WidgetEnabledCondition(button, true));
		
		button.click();
	}
}