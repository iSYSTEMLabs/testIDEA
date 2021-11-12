package si.isystem.swtbot.utils;

/*******************************************************************************
 * Copyright (c) 2008 Ketan Padegaonkar and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ketan Padegaonkar - initial API and implementation
 *     Marko Klopicic - adapted for tristate check box.
 *******************************************************************************/

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swtbot.swt.finder.ReferenceBy;
import org.eclipse.swtbot.swt.finder.SWTBotWidget;
import org.eclipse.swtbot.swt.finder.Style;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.utils.internal.Assert;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToggleButton;
import org.hamcrest.SelfDescribing;

/**
 * Represents a checkbox {@link Button} of type {@link SWT#CHECK}.
 *
 * @author Ketan Padegaonkar &lt;KetanPadegaonkar [at] gmail [dot] com&gt;
 * @version $Id$
 * @see SWTBotButton
 * @see SWTBotRadio
 * @see SWTBotToggleButton
 */
@SWTBotWidget(clasz = Button.class, style = @Style(name = "SWT.CHECK", value = SWT.CHECK), preferredName = "checkBox", referenceBy = { ReferenceBy.LABEL, ReferenceBy.MNEMONIC, ReferenceBy.TOOLTIP })//$NON-NLS-1$
public class SWTBotTristateCheckBox extends SWTBotCheckBox {

    /**
     * Constructs an instance of this object with the given button (Checkbox)
     *
     * @param w the widget.
     * @throws WidgetNotFoundException if the widget is <code>null</code> or widget has been disposed.
     * @since 1.0
     */
    public SWTBotTristateCheckBox(Button w) throws WidgetNotFoundException {
        this(w, null);
    }

    /**
     * Constructs an instance of this object with the given button (Checkbox)
     *
     * @param w the widget.
     * @param description the description of the widget, this will be reported by {@link #toString()}
     * @throws WidgetNotFoundException if the widget is <code>null</code> or widget has been disposed.
     * @since 1.0
     */
    public SWTBotTristateCheckBox(Button w, SelfDescribing description) throws WidgetNotFoundException {
        super(w, description);
        Assert.isTrue(SWTUtils.hasStyle(w, SWT.CHECK), "Expecting a checkbox."); //$NON-NLS-1$
    }


    /**
     * Deselect the checkbox.
     */
    public void deselect() {
        log.debug(MessageFormat.format("Deselecting {0}", this)); //$NON-NLS-1$
        waitForEnabled();
        if (!isChecked()) {
            log.debug(MessageFormat.format("Widget {0} already deselected, not deselecting again.", this)); //$NON-NLS-1$
            return;
        }
        asyncExec(new VoidResult() {
            public void run() {
                log.debug(MessageFormat.format("Deselecting {0}", this)); //$NON-NLS-1$
                // to understand settings in the following 2 lines, see also 
                // TBControlTristateCheckBox.DefaultSelectionListener.widgetSelected()
                widget.setGrayed(false);
                widget.setSelection(false);
            }
        });
        notifyListeners();
    }

    
    /**
     * Select the checkbox.
     */
    public void select() {
        log.debug(MessageFormat.format("Selecting {0}", this)); //$NON-NLS-1$
        waitForEnabled();
        if (isChecked()  &&  !isGrayed()) {
            log.debug(MessageFormat.format("Widget {0} already selected, not selecting again.", this)); //$NON-NLS-1$
            return;
        }
        
        asyncExec(new VoidResult() {
            public void run() {
                log.debug(MessageFormat.format("Selecting {0}", this)); //$NON-NLS-1$
                // to understand settings in the following 2 lines, see also 
                // TBControlTristateCheckBox.DefaultSelectionListener.widgetSelected()
                widget.setGrayed(true);
                widget.setSelection(false);
            }
        });
        notifyListeners();
    }

    
    public void setGrayed() {
        log.debug(MessageFormat.format("Selecting {0}", this)); //$NON-NLS-1$
        waitForEnabled();
        if (isGrayed()) {
            log.debug(MessageFormat.format("Widget {0} already grayed, not selecting again.", this)); //$NON-NLS-1$
            return;
        }
        asyncExec(new VoidResult() {
            public void run() {
                log.debug(MessageFormat.format("Selecting {0}", this)); //$NON-NLS-1$
                // to understand settings in the following 2 lines, see also 
                // TBControlTristateCheckBox.DefaultSelectionListener.widgetSelected()
                widget.setSelection(false);
                widget.setGrayed(false);
            }
        });
        notifyListeners();
    }
    
    
    /**
     * Toggles the checkbox from deselected to grayed to selected to deselected...
     */
    protected void toggle() {
        waitForEnabled();
        
        if (isGrayed()) {
            select();
        } else if (isChecked()) {
            deselect();
        } else {
            setGrayed();
        }
        /*
        asyncExec(new VoidResult() {
            public void run() {
                log.debug(MessageFormat.format("Toggling state on {0}. Setting state to {1}", widget, (!widget.getSelection() ? "selected" //$NON-NLS-1$ //$NON-NLS-2$
                        : "unselected"))); //$NON-NLS-1$
                if (!widget.getSelection()) {
                    widget.setGrayed(true);
                    widget.setSelection(true);
                } else if (widget.getGrayed()) {
                    widget.setGrayed(false);
                    widget.setSelection(true);
                } else {
                    widget.setGrayed(false);
                    widget.setSelection(false);
                }
            }
        });
        notifyListeners(); */
    }

    
    public boolean isGrayed() {
        return syncExec(new BoolResult() {
            public Boolean run() {
                return widget.getGrayed()  &&  widget.getSelection();
            }
        });
    }
}
