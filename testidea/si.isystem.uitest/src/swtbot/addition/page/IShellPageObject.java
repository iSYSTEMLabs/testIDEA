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
package swtbot.addition.page;

/**
 * interface for a shell page object, e.g. a page which represents a shell with a title either
 * the main application, a dialog or wizard
 * @author Kay-Uwe Graw &lt;kugraw [at] web [dot] de&gt;
 *
 */
public interface IShellPageObject extends IOpeningPageObject
{	
	/**
	 * close the shell corresponding to this page if it is still open
	 * should be used only in test teardown, in case that the shell may still be open after a failed test
	 */
	public void closeIfOpen();
	
	/**
	 * 
	 * @return - the title of the shell page
	 */
	public String getTitle();
}
