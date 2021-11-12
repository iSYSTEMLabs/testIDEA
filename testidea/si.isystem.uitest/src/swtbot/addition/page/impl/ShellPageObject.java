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
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;

import swtbot.addition.condition.ShellCondition;
import swtbot.addition.page.IShellPageObject;

/**
 * base class for a page object which represent a shell
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public abstract class ShellPageObject<T extends SWTBot> extends AbstractPageObject<T> implements IShellPageObject
{
	protected String itsTitle;

	/**
	 * constructor
	 * @param testCase - the test for which the page object has been created
	 *  @param bot - the bot
	 */
	public ShellPageObject(T bot, String title)
	{
		super(bot);
		
		itsTitle = title;
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IShellPageObject#waitForPageToOpen()
	 */
	public void waitForPageToOpen()
	{		
		itsBot.waitUntil(new ShellCondition(itsTitle, true));
	}
	
	/**
	 * wait for the shell represented by the page to close
	 */
	protected void waitForPageToClose()
	{		
		itsBot.waitUntil(new ShellCondition(itsTitle, false));
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IShellPageObject#closeIfOpen()
	 */
	public void closeIfOpen()
	{
		long oldTimeOut = SWTBotPreferences.TIMEOUT;

		SWTBotPreferences.TIMEOUT = 1000;
		
		try
	    {
	    	SWTBotShell[] shells = itsBot.shells();
	    	
	    	for (SWTBotShell shell: shells)
	    	{
	    		if (shell.getText().equals(itsTitle))
	    		{
	    			log.warn("force closing of still open shell\"" + shell.getText() + "\"");
	    			
	    			shell.close();
	    			
	    			itsBot.waitUntil(new ShellCondition(itsTitle, false));
	    			
	    			break;
	    		}
	    	}
	    }
	    catch (WidgetNotFoundException e)
	    {
	    	//do nothing
	    	;
	    }
	    catch(TimeoutException e)
	    {
	    	//do nothing
	    	;
	    }
	    finally
	    {
	    	SWTBotPreferences.TIMEOUT = oldTimeOut;
	    }
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IShellPageObject#getTitle()
	 */
	public String getTitle()
	{
		return itsTitle;
	}
}