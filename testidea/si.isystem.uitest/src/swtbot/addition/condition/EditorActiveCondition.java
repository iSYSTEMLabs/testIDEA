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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

import static org.junit.Assert.fail;

/**
 * ICondition implementation to wait for an editor to become active or inactive
 * This is useful when the creation of an editor takes a while after the initiating
 * user action has been carried out
 * @author  Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public class EditorActiveCondition implements ICondition
{
	private final String itsEditorName;
	
	private SWTWorkbenchBot itsBot;
	
	private final boolean itsActive;
	
	/**
	 * the constructor
	 * @param editorName - the name of the editor which should be active/not active after a certain time span
	 * @param active - the flag whether the editor should be active (true) or inactive
	 */
	public EditorActiveCondition(String editorName, boolean active)
	{
		this.itsEditorName = editorName;
		
		this.itsActive = active;
	}
	
	public String getFailureMessage()
	{
		if (itsActive)
		{
			return "wait for editor " + itsEditorName + " is active failed"; //$NON-NLS-1$
		}
		else
		{
			return "wait for editor " + itsEditorName + " is not active failed"; //$NON-NLS-1$
		}
	}

	public void init(SWTBot bot)
	{
		if (bot instanceof SWTWorkbenchBot)
		{
			this.itsBot = SWTWorkbenchBot.class.cast(bot);
		}
		else
		{
			fail("init with wrong bot class");
		}
	}

	public boolean test() throws Exception
	{
		boolean ret = editorIsActive(itsEditorName) == itsActive;
		
		return ret;
	}

	private SWTBotEditor getEditor(String editorName)
	{
		long oldTimeOut = SWTBotPreferences.TIMEOUT;
		
		SWTBotPreferences.TIMEOUT = 1000;
		
		try
		{
			
			SWTBotEditor editor = itsBot.editorByTitle(editorName);
			
			return editor;
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
		
		return null;
	}
	
	private boolean editorIsActive(String editorName)
	{
		final SWTBotEditor editor = getEditor(editorName);
		
		if (editor!=null)
		{
			return UIThreadRunnable.syncExec(new BoolResult()
			{
				public Boolean run()
				{
					return editor.isActive();
				}
			});
		}
		
		return false;
	}
}