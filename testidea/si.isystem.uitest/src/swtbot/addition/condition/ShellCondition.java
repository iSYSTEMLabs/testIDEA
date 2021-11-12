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

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

/**
 * ICondition implementation to wait for active shell to have a specific title or not
 * @author  Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class ShellCondition implements ICondition
{
	private final String itsTitle;
	private SWTBot itsBot;
	private final boolean itsEqual;
	
	/**
	 * constructor
	 * @param titleName - the title name
	 * @param equal - flag whether title of active shell should be equal or not to title name parameter
	 */
	public ShellCondition(String titleName, boolean equal)
	{	
		this.itsTitle = titleName;
		
		this.itsEqual = equal;
	}

	public String getFailureMessage()
	{
		if (itsEqual)
		{
			return "shell \"" + itsTitle + "\" still not active";
		}
		else
		{
			return "shell \"" + itsTitle + "\" still active";
		}
	}

	public void init(SWTBot bot)
	{
		itsBot = bot;
	}

	public boolean test() throws Exception
	{
		
		return itsBot.activeShell().getText().equals(itsTitle) == itsEqual;
	}
}
