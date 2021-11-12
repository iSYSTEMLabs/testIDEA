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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;

import swtbot.addition.condition.EditorActiveCondition;
import swtbot.addition.page.IEditorPageObject;

/**
 * base implementation of the IEditorPageObject interface
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 */
public class EditorPageObject extends AbstractPageObject<SWTWorkbenchBot> implements IEditorPageObject
{
	private final String itsEditorName;
	
	/**
	 * constructor
	 * @param bot - the workbench bot
	 * @param editorName - the part name of the editor
	 */
	public EditorPageObject(SWTWorkbenchBot bot, String editorName)
	{
		super(bot);
		
		itsEditorName = editorName;
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IEditorPageObject#save()
	 */
	public void save()
	{
		getEditor().save();
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IPartPageObject#close()
	 */
	public void close()
	{
		getEditor().close();

		itsBot.waitUntil(new EditorActiveCondition(itsEditorName, false));
	}

	/*
	 * (non-Javadoc)
	 * @see swtbot.addition.page.IPartPageObject#waitForPageToOpen()
	 */
	public void waitForPageToOpen()
	{
		itsBot.waitUntil(new EditorActiveCondition(itsEditorName, true));
	}
	
	protected SWTBotEditor getEditor()
	{
		return itsBot.editorByTitle(itsEditorName);	
	}
}
