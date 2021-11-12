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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;

import swtbot.addition.condition.ViewVisibleCondition;
import swtbot.addition.page.IViewPageObject;

/**
 * base class for page objects representing an eclipse view
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 */
public class ViewPageObject extends AbstractPageObject<SWTWorkbenchBot> implements IViewPageObject
{
	private final String itsViewIdentifier;
	
	private final boolean itsIsId;

	/**
	 * 
	 * @param bot - the bot
 	 * @param viewIdentifier - the identifier of the view which should be visible/not visible after a certain time span
	 * @param isId - flag whether viewIdentifier is the id (true) or the title (false) of the view
	 */
	public ViewPageObject(SWTWorkbenchBot bot, String viewIdentifier, boolean isId)
	{
		super(bot);
		
		this.itsViewIdentifier = viewIdentifier;
		
		this.itsIsId = isId;
	}
	
	public ViewPageObject(SWTWorkbenchBot bot, String viewIdentifier)
	{
		this(bot, viewIdentifier, true);
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IPartPageObject#close()
	 */
	public void close()
	{
		getView().close();
		
		itsBot.waitUntil(new ViewVisibleCondition(itsViewIdentifier, false, itsIsId));
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IPartPageObject#waitForPageToOpen()
	 */
	public void waitForPageToOpen()
	{
		itsBot.waitUntil(new ViewVisibleCondition(itsViewIdentifier, true, itsIsId));
	}
	
	protected SWTBotView getView()
	{
		if (itsIsId)
		{
			return itsBot.viewById(itsViewIdentifier);
		}
		else
		{
			return itsBot.viewByTitle(itsViewIdentifier);
		}
	}
}
