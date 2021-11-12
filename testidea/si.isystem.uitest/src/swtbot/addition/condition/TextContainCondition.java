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
 * ICondition implementation to wait for the content of a text control to contain or not to contain a specific string value
 * This is useful when the content change of a text takes a while after the initiating
 * user action has been carried out
 * @author  Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class TextContainCondition  implements ICondition
{
	private final String itsContent;
	private final SWTBotText itsText;
	private final boolean itsContain;
	
	/**
	 * the constructor
	 *
	 * @param text - the text control which should contain a specific value after a certain time span
	 * @param content - the content the text control should contain after a certain time span
	 * @param contain - the contain flag
	 */
	public TextContainCondition(SWTBotText text, String content, boolean contain)
	{
		this.itsContent = content;
		this.itsText = text;
		this.itsContain = contain;
	}

	public String getFailureMessage()
	{
		if (itsContain)
		{
			return "wait for text content contains " + itsContent + " failed"; //$NON-NLS-1$
		}
		else
		{
			return "wait for text content contains not " + itsContent + " failed"; //$NON-NLS-1$
		}
	}

	public void init(SWTBot bot)
	{
	}

	public boolean test() throws Exception
	{
		return itsText.getText().contains(itsContent)==itsContain;
	}
}
