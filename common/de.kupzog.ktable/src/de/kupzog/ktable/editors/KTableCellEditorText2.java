/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
    
    Author: Friederich Kupzog  
    fkmk@kupzog.de
    www.kupzog.de/fkmk
*/

package de.kupzog.ktable.editors;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;

/**
 * A simple cell editor that simply creates a text widget that allows the user
 * to type in one line of text.<p>
 * This class is very similar to <code>KTableCellEditorText</code>, but 
 * allows the navigation within the text widget using ARROW_LEFT and ARROW_RIGHT
 * keys.
 * 
 * @see de.kupzog.ktable.editors.KTableCellEditorText
 * @author Lorenz Maierhofer <lorenz.maierhofer@logicmindguide.com>
 */
public class KTableCellEditorText2 extends KTableCellEditor 
{
	protected Text m_Text;
    
    protected KeyAdapter keyListener = new KeyAdapter() {
        /**
         * Both key pressed and released events must be handled so
         * that we can extent the class later for proposal providers.
         */
        public void keyPressed(KeyEvent e) {
            try {
                onKeyPressed(e);
            } catch (Exception ex) {
                ex.printStackTrace();
                // Do nothing
            }
        }
        public void keyReleased(KeyEvent e) {
            try {
                onKeyReleased(e);
            } catch (Exception ex) {
                ex.printStackTrace();
                // Do nothing
            }
        }
    };
    
    protected TraverseListener travListener = new TraverseListener() {
        public void keyTraversed(TraverseEvent e) {
            onTraverse(e);
        }
    };

    
    private ContentProposalAdapter m_proposalAdapter;
    private ContentProposalConfig m_contentProposalConfig;

    // This flag is used to prevent propagating ENTER and ESC keys to underlying
    // base editor. Otherwise if user pressed ENTER to select proposal, editor is also 
    // closed, which is often not desired. If user pressed ESC to close content
    // proposals, editor was closed too, and last changes lost. 
    private boolean m_isBlockCR;

	

    public KTableCellEditorText2() {
    }
    

    /**
     * Creates cell editor with content proposals.
     * 
     * @param proposalProvider
     * @param proposalsAcceptanceStyle one of ContentProposalAdapter.PROPOSAL_REPLACE or
     *                      ContentProposalAdapter.PROPOSAL_INSERT
     */
    public KTableCellEditorText2(String [] proposals, int proposalsAcceptanceStyle) {

        SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(proposals);
        proposalProvider.setFiltering(true);
        
        m_contentProposalConfig = new ContentProposalConfig(proposalProvider,
                                                            new TextContentAdapter());
        
        m_contentProposalConfig.setProposalsAcceptanceStyle(proposalsAcceptanceStyle);
    }

    
    /**
     * Creates cell editor with content proposals.
     * 
     * @param proposalProvider
     * @param proposalsAcceptanceStyle one of ContentProposalAdapter.PROPOSAL_REPLACE or
     *                      ContentProposalAdapter.PROPOSAL_INSERT
     */
    public KTableCellEditorText2(IContentProposalProvider proposalProvider, 
                                 int proposalsAcceptanceStyle) {
        m_contentProposalConfig = new ContentProposalConfig(proposalProvider,
                                                            new TextContentAdapter());
        m_contentProposalConfig.setProposalsAcceptanceStyle(proposalsAcceptanceStyle);
    }

    
    public KTableCellEditorText2(ContentProposalConfig cfg) {
        m_contentProposalConfig = cfg;
    }
    
    
    public void open(KTable table, int col, int row, Rectangle rect) {
		super.open(table, col, row, rect);
		m_Text.setText(getEditorContent());
		m_Text.selectAll();
		m_Text.setVisible(true);
		m_Text.setFocus();
	}

    
    @Override
    public boolean isProposalPopupOpen() {
        return m_proposalAdapter != null  &&  m_proposalAdapter.isProposalPopupOpen();
    }
    
    
	/**
	 * Overwrite this method if you do not want to use the content provided by the model.
	 * 
	 * @return The content for the editor.
	 */
	protected String getEditorContent() {
		return m_Model.getContentAt(m_Col, m_Row).toString();
	}

	public void close(boolean save) {
        if (m_Text != null) { // MK: m_Text may be null, when user presses ENTER at end of edit and then
                              // focus of table is changed programmatically. Rare event, but not impossible.
            String content = m_Text.getText();
            m_Text.removeKeyListener(keyListener);
            m_Text.removeTraverseListener(travListener);
            super.close(save);
            m_Text = null;
            
            // modify model only after the editor is closed, otherwise we get 
            // recursive loop resulting in stack overflow 
            if (save) {
                m_Model.setContentAt(m_Col, m_Row, content);
            }
        }
    }


	protected Control createControl() {
		
	    m_Text = new Text(m_Table, SWT.NONE);
        m_Text.addKeyListener(keyListener);
        m_Text.addTraverseListener(travListener);
        
        if (m_contentProposalConfig != null) {
            m_proposalAdapter = 
                    new ContentProposalAdapter(m_Text, 
                                               m_contentProposalConfig.getControlContentAdapter(),
                                               m_contentProposalConfig.getProposalProvider(), 
                                               m_contentProposalConfig.getKeyStroke(),
                                               m_contentProposalConfig.m_autoActivationCharacters);
            m_proposalAdapter.setPropagateKeys(true);
            m_proposalAdapter.setProposalAcceptanceStyle(m_contentProposalConfig.getProposalsAcceptanceStyle());
            m_proposalAdapter.setEnabled(true);
        }
        
		return m_Text;
	}
	
	/**
	 * Implement In-Textfield navigation with the keys... 
	 * @see de.kupzog.ktable.KTableCellEditor#onTraverse(org.eclipse.swt.events.TraverseEvent)
	 */
	protected void onTraverse(TraverseEvent e) {
	    
	    if (m_proposalAdapter != null  &&  m_proposalAdapter.isProposalPopupOpen()) {
	        if (e.keyCode == SWT.ARROW_UP  ||
	                e.keyCode == SWT.ARROW_DOWN  ||
	                e.keyCode == SWT.ARROW_LEFT  ||
	                e.keyCode == SWT.ARROW_RIGHT  ||
	                e.keyCode == SWT.CR ||
	                e.keyCode == SWT.KEYPAD_CR) {

	            e.detail = SWT.TRAVERSE_NONE;
	            e.doit = false;
	            return;
	        }
	        
            super.onTraverse(e);
            return; 
	    }
	    
	    
	    if (e.keyCode == SWT.ARROW_LEFT) {
	        if (m_Text.getCaretPosition() == 0  &&  m_Text.getSelectionCount() == 0)
	            super.onTraverse(e); // moves to cell to the left
	    } else if (e.keyCode == SWT.ARROW_RIGHT) {
	        if (m_Text.getCaretPosition() == m_Text.getText().length()  &&  m_Text.getSelectionCount() == 0) {
	            super.onTraverse(e); // moves to cell to the right
	        }
        } else if (e.keyCode == SWT.CR  ||  e.keyCode == SWT.KEYPAD_CR  ||  e.keyCode == SWT.ESC) {
            super.onTraverse(e);
            // when table is opened in dialog, prevent ENTER propagating to dialog,
            // because it closes the dialog! 
            e.doit = false;
	    } else {
            // handle the event within the text widget!
	        super.onTraverse(e);
	    }
	}
	
	
	 @Override
     protected void onKeyPressed(KeyEvent e) {

         m_isBlockCR = m_proposalAdapter != null  
                       && m_proposalAdapter.isProposalPopupOpen() 
                       && (e.keyCode == SWT.CR  ||  e.keyCode == SWT.ESC);

         if (!m_isBlockCR) {
             super.onKeyPressed(e);
         }
     }
	protected void onKeyReleased(KeyEvent e) {
	    
	    if ((e.keyCode == SWT.CR  ||  e.keyCode == SWT.ESC)  &&  m_isBlockCR) {
	        return;
	    }
	    
		if (e.keyCode == SWT.CR  &&  (e.stateMask & SWT.SHIFT) == 0) {
			close(true);
			// move one row below!
//			if (m_Row<m_Model.getRowCount())
//			    m_Table.setSelection(m_Col, m_Row+1, true);
		} else {
		    /*
		     * The key RELEASE method calls the key PRESSED method
		     * on purpose - this is required for proposal providers.
		     */
		    super.onKeyPressed(e);
		}
	}
	
	/* 
	 * overridden from superclass
	 */
	public void setBounds(Rectangle rect) 
	{
		super.setBounds(new Rectangle(rect.x, rect.y + 2,
                                      rect.width, rect.height));
	}
	
	/* (non-Javadoc)
     * @see de.kupzog.ktable.KTableCellEditor#setContent(java.lang.Object)
     */
    public void setContent(Object content) {
        m_Text.setText(content.toString());
        m_Text.setSelection(content.toString().length());
    }

    
    public void notifyListeners(int eventType, Event event) {
        m_Text.notifyListeners(eventType, event);
    }
}
