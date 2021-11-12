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
package swtbot.addition.condition;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

import static org.junit.Assert.fail;

/**
 * ICondition implementation to wait for a view to become visible or not visible
 * This is useful when the creation of a view takes a while after the initiating
 * user action has been carried out
 * @author  Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class ViewVisibleCondition implements ICondition
{
	private final String itsViewIdentifier;
	
	private SWTWorkbenchBot itsWorkbenchBot;
	
	private final boolean itsVisible;
	private final boolean itsIsId;
	
	/**
	 * 
	 * @param viewIdentifier - the identifier of the view which should be visible/not visible after a certain time span
	 * @param visible - flag whether view should be visible or not
	 * @param isId - flag whether viewIdentifier is the id (true) or the title (false) of the view
	 */
	public ViewVisibleCondition(String viewIdentifier, boolean visible, boolean isId)
	{
		this.itsViewIdentifier = viewIdentifier;
		
		this.itsVisible = visible;
		
		this.itsIsId = isId;
	}

	public String getFailureMessage()
	{
		if (itsVisible)
		{
			return "wait for view " + itsViewIdentifier + " is visible failed"; //$NON-NLS-1$
		}
		else
		{
			return "wait for view " + itsViewIdentifier + " is not visible failed"; //$NON-NLS-1$
		}
	}

	public void init(SWTBot bot)
	{
		if (bot instanceof SWTWorkbenchBot)
		{
			this.itsWorkbenchBot = SWTWorkbenchBot.class.cast(bot);
		}
		else
		{
			fail("init with wrong bot class");
		}
	}

	public boolean test() throws Exception
	{
		return viewIsVisible() == itsVisible;
	}
	
	private SWTBotView getView()
	{
		long oldTimeOut = SWTBotPreferences.TIMEOUT;
		
		SWTBotPreferences.TIMEOUT = 1000;
		
		SWTBotView view = null;
		
		try
		{
			if (itsIsId)
			{
				view = itsWorkbenchBot.viewById(itsViewIdentifier);
			}
			else
			{
				view = itsWorkbenchBot.viewByTitle(itsViewIdentifier);
			}
			
		}
		catch (WidgetNotFoundException e)
		{
			//do nothing
			;
		}
		finally
		{
			SWTBotPreferences.TIMEOUT = oldTimeOut;
		}
		
		return view;
	}
	
	private boolean viewIsVisible()
	{
		final SWTBotView view = getView();
		
		if (view!=null)
		{
			return UIThreadRunnable.syncExec(new BoolResult()
			{
				public Boolean run()
				{
					if (view.getWidget() instanceof Control)
					{
						return ((Control) view.getWidget()).isVisible();
					}
					else
					{
						return false;
					}
				}
			});
		}
		
		return false;
	}
}
