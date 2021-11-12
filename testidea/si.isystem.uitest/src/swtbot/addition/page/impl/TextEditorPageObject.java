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

import swtbot.addition.page.ITextEditorPageObject;

/**
 * base implementation of the ITextEditorPageObject interface
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 */
public class TextEditorPageObject extends EditorPageObject implements ITextEditorPageObject
{
	/**
	 * constructor
	 * @param bot - the bot
	 * @param editorName - the part name of the editor
	 */
	public TextEditorPageObject(SWTWorkbenchBot bot, String editorName)
	{
		super(bot, editorName);
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.ITextEditorPageObject#getText()
	 */
	public String getText()
	{
		return getEditor().toTextEditor().getText();
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.ITextEditorPageObject#setText(java.lang.String)
	 */
	public void setText(String text)
	{
		getEditor().toTextEditor().setText(text);
	}
}
