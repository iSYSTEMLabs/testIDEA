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

import swtbot.addition.page.IWizardPageObject;

/**
 * base class for page objects representing a wizard
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class WizardPageObject<T extends SWTBot> extends CancelPageObject<T> implements IWizardPageObject
{
	private final String itsFinishButtonText;
	private final String itsBackButtonText;
	private final String itsNextButtonText;

	/**
	 * the constructor
	 * @param bot - the bot
	 * @param title - the (start) title of the wizard
	 * @param cancelButtonText - the text of the cancel button
	 * @param finishButtonText - the text of the finish button
	 * @param backButtonText - the text of the back button
	 * @param nextButtonText - the text of the next button
	 */
	public WizardPageObject(T bot, String title, String cancelButtonText, String finishButtonText,
			String backButtonText, String nextButtonText)
	{
		super(bot, title, cancelButtonText);

		itsFinishButtonText = finishButtonText;

		itsBackButtonText = backButtonText;

		itsNextButtonText = nextButtonText;
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IWizardPageObject#finish()
	 */
	public void finish()
	{
		clickClosingButton(finishButton());
	}
	
	protected SWTBotButton finishButton()
	{
		return itsBot.button(itsFinishButtonText);
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IWizardPageObject#back()
	 */
	public void back()
	{
		clickButton(backButton());
	}
	
	protected SWTBotButton backButton()
	{
		return itsBot.button(itsBackButtonText);
	}
	
	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IWizardPageObject#next()
	 */
	public void next()
	{
		clickButton(nextButton());
	}
	
	protected SWTBotButton nextButton()
	{
		return itsBot.button(itsNextButtonText);
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IWizardPageObject#setTitle(java.lang.String)
	 */
	public void setTitle(String title)
	{
		itsTitle = title;
	}
}
