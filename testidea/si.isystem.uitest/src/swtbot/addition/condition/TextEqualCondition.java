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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

/**
 * ICondition implementation to wait for the content of text control to become equal or unequal to a specific string value
 * This is useful when the content change of a text takes a while after the initiating
 * user action has been carried out
 * @author  Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class TextEqualCondition implements ICondition
{
	private final String itsContent;
	private final SWTBotText itsText;
	private final boolean itsEqual;
	
	/**
	 * the constructor
	 *
	 * @param text - the text control which should contain a specific value after a certain time span
	 * @param content - the content the text control should contain after a certain time span
	 * @param equal - the equal condition
	 */
	public TextEqualCondition(SWTBotText text, String content, boolean equal)
	{
		this.itsContent = content;
		this.itsText = text;
		this.itsEqual = equal;
	}

	public String getFailureMessage()
	{
		if (itsEqual)
		{
			return "wait for text content equals " + itsContent + " failed"; //$NON-NLS-1$
		}
		else
		{
			return "wait for text content equals not " + itsContent + " failed"; //$NON-NLS-1$
		}
	}

	public void init(SWTBot bot)
	{
	}

	public boolean test() throws Exception
	{
		return itsText.getText().equals(itsContent)==itsEqual;
	}
}
