package si.isystem.swtbot.utils;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withId;

import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.keyboard.Keyboard;
import org.eclipse.swtbot.swt.finder.keyboard.KeyboardFactory;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.hamcrest.Matcher;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.Messages;
import si.isystem.tbltableeditor.handlers.InsertColumnHandler;
import si.isystem.uitest.UITestUtils;

public class KTableTestUtils {

    private SWTWorkbenchBot m_bot;
    private UITestUtils m_utils;
    private SWTBotKTable m_ktable;
    private Keyboard m_keyboard;

    
    public KTableTestUtils(SWTWorkbenchBot bot, UITestUtils utils) {
        m_bot = bot;
        m_utils = utils;
        m_keyboard = KeyboardFactory.getSWTKeyboard();
    }

    
    /** 
     * One of select...() methods should be called before any other method in 
     * this class and every time a new KTable is edited.
     * @param id component ID set with 
     * KTable.setData(si.isystem.itest.common.SWTBotConstants.SWT_BOT_ID_KEY, id);
     */
    public SWTBotKTable selectKTable(String id) {
        m_ktable = ktableWithId(id);
        m_ktable.setFocus();
        return m_ktable;
    }

    
    public SWTBotKTable selectKTable(int index) {
        Class<KTable> clazz = KTable.class;
        @SuppressWarnings("unchecked")
        Matcher<KTable> matcher = allOf(widgetOfType(clazz));
         
        KTable ktable = m_bot.widget(matcher, index);
        m_ktable = new SWTBotKTable(ktable, matcher);
        m_ktable.setFocus();
        return m_ktable;
    }

    
    public SWTBotKTable ktableWithId(String id) {

        Class<KTable> clazz = KTable.class;
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = allOf(widgetOfType(clazz), withId(id));
        Widget widget = m_bot.widget(matcher, 0);
        KTable ktable = (KTable)widget;
        return new SWTBotKTable(ktable, matcher);
    }
    
    
    /**
     * 
     * @param col column of the cell with given content
     * @param row row of the cell with given content
     * @param contentRegEx content to look for
     * @return
     */
    public SWTBotKTable selectKTableWithContent(int col, int row, String contentRegEx) {
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = 
        WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(Widget.class), 
                                   new KTableContentMatcher<Widget>(col, row, contentRegEx));
        KTable kTable = (KTable)m_bot.widget(matcher, 0);
        m_ktable = new SWTBotKTable(kTable, matcher);
        m_ktable.setFocus();
        return m_ktable;
    }

    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public void clickCell(int column, int row) {
        m_ktable.click(column, row);
    }
    

    /**
     * @param column column index for data cells - 0 means the first column after header column
     * @param row row index for data cells - 0 means the first row after header row
     */
    public void clickDataCell(int column, int row) {
        clickCell(column + m_ktable.getHeaderColumnCount(), 
                  row + m_ktable.getHeaderRowCount());
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public void selectCell(int column, int row) {
        m_ktable.selectCell(column, row);
    }
    
    
    /**
     * @param dataColumn column index for data cells - 0 means the first column after header column
     * @param dataRow row index for data cells - 0 means the first row after header row
     */
    public void selectDataCell(int dataColumn, int dataRow) {
        selectCell(dataColumn + m_ktable.getHeaderColumnCount(), 
                   dataRow + m_ktable.getHeaderRowCount());
    }
    
    
    public void selectDataCells(int leftDataCol, int topDataRow, int rightDataCol, int bottomDataRow) {
        int leftCol = leftDataCol + m_ktable.getHeaderColumnCount();
        int topRow = topDataRow + m_ktable.getHeaderRowCount();
        int rightCol = rightDataCol + m_ktable.getHeaderColumnCount();
        int bottomRow = bottomDataRow + m_ktable.getHeaderRowCount();
        
        m_ktable.selectCells(leftCol, topRow, rightCol, bottomRow);;
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public void setCellContent(int column, int row, String text) {
        setCellContent(column, row, text, false, 0);
    }
    
    
    /**
     * @param dataCol column index for data cells - 0 means the first column after header column
     * @param dataRow row index for data cells - 0 means the first row after header row
     */
    public void setDataCell(int dataCol, int dataRow, String text) {
        setCellContent(dataCol + m_ktable.getHeaderColumnCount(), 
                       dataRow + m_ktable.getHeaderRowCount(), 
                       text, false, 0);
    }
    
    
    /**
     * Sets text in cells, increments dataCol for each element in text.
     * 
     * @param startDataCol column index for data cells - 0 means the first column after header column
     * @param dataRow row index for data cells - 0 means the first row after header row
     */
    public void setDataRowContent(int startDataCol, int dataRow, String ... text) {
        
        int col = startDataCol + m_ktable.getHeaderColumnCount();
        
        for (String tx : text) {
            int row = dataRow + m_ktable.getHeaderRowCount(); // row idx may increment when seq/mapping item is added
            setCellContent(col++, row, tx, false, 0);
        }
    }
    
    
    /**
     * @param column column index for data cells - 0 means the first column after header column
     * @param row row index for data cells - 0 means the first row after header row
     */
    public void setDataCellContent(int column, int row, String text, 
                                   boolean isAppend, int contentProposalsRow) {
        
        setCellContent(column + m_ktable.getHeaderColumnCount(), 
                       row + m_ktable.getHeaderRowCount(), 
                       text, isAppend, contentProposalsRow);
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public void setCellContent(int column, int row, String text, 
                               boolean isAppend, int contentProposalsRow) {
        try {
            m_ktable.doubleClick(column, row);
            m_bot.sleep(100);  // otherwise the first letter is missed sometimes     
            if (isAppend) {
                m_utils.pressKey(IKeyLookup.END_NAME);
            }
            m_utils.typeText(text);

            m_bot.sleep(200); // some delay is needed for content proposals to show up
            if (contentProposalsRow > 0) {
                for (int i = 0; i < contentProposalsRow; i++) {
                    m_utils.pressKey(IKeyLookup.ARROW_DOWN_NAME);
                }
            }

            // m_utils.pressKey(SWT.ESC); // if content assist appears, ESC cancels it
            m_utils.pressKey(SWT.CR);
            m_utils.pressKey(SWT.ARROW_RIGHT);// if content assist appears, the CR
                                              // closes it, arrow ends editing            
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    

    /**
     * 
     * @param dataCol
     * @param dataRow
     * @param text
     */
    public void addUserSeqItem(int headerColWithPlus, int headerRowWithPlus, int dataRow, String text) {
        addSeqColumn(headerColWithPlus, headerRowWithPlus);
        setDataCell(headerColWithPlus, dataRow, text);
    }
    
    
    public void addUserMappingItem(int headerColWithPlus, int headerRowWithPlus, int dataRow, String key, String value) {
        addMapColumn(headerColWithPlus, headerRowWithPlus, key);
        setDataCell(headerColWithPlus, dataRow, value);
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public String getContentAsString(int column, int row) {
        try {
            return m_ktable.getContentAt(column, row).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    
    public String getDataCellAsString(int column, int row) {
        try {
            return m_ktable.getContentAt(column + m_ktable.getHeaderColumnCount(), 
                                         row + m_ktable.getHeaderRowCount()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    
    public int getRowCount() {
        return m_ktable.getRowCount();
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public Object getCellAt(int column, int row) {
        try {
            return m_ktable.getContentAt(column, row);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    
    /**
     * @param column column index including header cells
     * @param row row index including header cells
     */
    public Object getDataCellAt(int dataCol, int dataRow) {
        try {
            return m_ktable.getContentAt(dataCol + m_ktable.getHeaderColumnCount(), 
                                         dataRow + m_ktable.getHeaderRowCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    
    /**
     * Adds one line to ktable containing only one data column.
     * 
     * @param tableHeader title of the table in header call at (1, 0)
     * @param content text to be entered into the last row.
     */
    public void addLineToListTable(String tableHeader, String content) {
        selectKTableWithContent(1, 0, tableHeader);
        addRows(1);
        int numRows = getRowCount();
        setCellContent(1, numRows - 2, content);
    }
    
    
    /**
     * @param dataCol column index for data cells - 0 means the first column after header column
     * @param dataRow row index for data cells - 0 means the first row after header row
     */
    public void selectDataItemInCombo(int dataCol, int dataRow, String keySeq) {
        selectDataCell(dataCol, dataRow);
        for (char ch : keySeq.toCharArray()) {
            m_keyboard.pressShortcut(KeyStroke.getInstance(ch));
        }
        m_keyboard.pressShortcut(KeyStroke.getInstance('\n'));
    }


    /**
     * @param column column index for data cells - 0 means the first column after header column
     * @param row row index for data cells - 0 means the first row after header row
     */
    private void toggleDataCheckBox(int column, int row, int numClicks) {
        selectDataCell(column, row);
        for (int i = 0; i < numClicks; i++) {
            clickDataCell(column, row);
        }
    }

    
    public void setTristateCheckBox(int dataColumn, int dataRow, ETristate state) {
        selectDataCell(dataColumn, dataRow);
        int column = dataColumn + m_ktable.getHeaderColumnCount();
        int row = dataRow + m_ktable.getHeaderRowCount();
        TextIconsContent tableCell = (TextIconsContent)getCellAt(column, row);
        String tristateVal = tableCell.getTristateValue();
        int numClicks = 0;
        
        if (tristateVal.equals(ETristate.E_DEFAULT.toString())) {
            switch (state) {
            case E_DEFAULT:
                break;
            case E_FALSE:
                numClicks = 1;
                break;
            case E_TRUE:
                numClicks = 2;
                break;
            }
        } else if (tristateVal.equals(ETristate.E_FALSE.toString())) {
            switch (state) {
            case E_DEFAULT:
                numClicks = 2;
                break;
            case E_FALSE:
                numClicks = 0;
                break;
            case E_TRUE:
                numClicks = 1;
                break;
            }
        } else if (tristateVal.equals(ETristate.E_TRUE.toString())) {
            switch (state) {
            case E_DEFAULT:
                numClicks = 1;
                break;
            case E_FALSE:
                numClicks = 2;
                break;
            case E_TRUE:
                numClicks = 0;
                break;
            }
        }

        for (int i = 0; i < numClicks; i++) {
            clickDataCell(dataColumn, dataRow);
        }
    }
    
    
    public void addSeqColumn(int column, int row) {
        m_ktable.clickDecoration(column, row, EIconPos.ETopRight);
    }
    
    
    public void addMapColumn(int column, int row, String varName) {
        m_ktable.clickDecoration(column, row, EIconPos.ETopRight);
        m_utils.waitForShell(InsertColumnHandler.ENTER_IDENTIFIER_NAME, 3000, true);
        m_bot.textWithLabel(InsertColumnHandler.LBL_IDENTIFIER).setText(varName);
        m_bot.sleep(100);
        m_bot.button("OK").click();
        m_bot.sleep(100);
    }
    

    public void addMapColumnWValue(int dataCol, int dataRow, String key, String value) {
        setDataCell(dataCol, dataRow, value);
        m_utils.waitForShell(InsertColumnHandler.ENTER_IDENTIFIER_NAME, 3000, true);
        m_bot.textWithLabel(InsertColumnHandler.LBL_IDENTIFIER).setText(key);
        m_bot.sleep(100);
        m_bot.button("OK").click();
        m_bot.sleep(100);
    }
    

    public void setComment(int column, int row, String nlComment, String eolComment) {
        m_ktable.clickDecoration(column, row, EIconPos.ETopLeft);
        m_utils.waitForShell(Messages.TagCommentDialog_Dialog_title, 3000, true);
        m_bot.textWithLabel(Messages.TagCommentDialog_Block_comment).setText(nlComment);
        m_bot.textWithLabel(Messages.TagCommentDialog_End_of_line_comment).setText(eolComment);
        m_bot.sleep(500);
        m_bot.button("OK").click();
        m_bot.sleep(100);
    }
    

    public void setDataCellComment(int dataCol, int dataRow, String nlComment, String eolComment) {
        setComment(dataCol + m_ktable.getHeaderColumnCount(), 
                   dataRow + m_ktable.getHeaderRowCount(), 
                   nlComment, eolComment);
    }
    

    void execContextMenuCmd(int column, int row, String cmd) {
        m_ktable.selectCell(column, row);

        String menutItemText = m_utils.translateMsg(cmd);

/*        SWTBotEditor editorById = m_bot.editorById("si.isystem.itest.editors.TestCaseEditorPart");
        SWTBotEclipseEditor textEditor = editorById.toTextEditor();
        textEditor.contextMenu(menutItemText).click();
*/
        ContextMenuForControl.clickContextMenu(m_ktable, menutItemText);
       
        /*         IViewReference viewRef = viewById.getViewReference();
        IViewPart view = viewRef.getView(false);
        view. */
        /* List<SWTBotViewMenu> menus = viewById.menus();
        
        for (SWTBotViewMenu menu : menus) {
            System.out.println("menu: " + menu.getText());
        } */
        // viewById.getWidget().notifyListeners(eventType, event);
        // m_ktable.contextMenu(column, row, "Delete column");
    }


    public void cutSelection(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Cut");
    }
    
    
    public void copySelection(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Copy");
    }
    
    public void paste(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste");
    }


    // this one does not seem to work - menu is found and clicked by SWTBot, but nothing happens
    public void deleteColumnWCtxMenu(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Delete_column");
    }

    
    public void deleteColumnWIcon(int column, int row) {
        m_ktable.clickDecoration(column, row, EIconPos.EBottomRight);
    }
    
    
    
    public void deleteRow(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Delete_row");
    }
    
    public void deleteRowWClick(int column, int row) {
        m_ktable.clickDecoration(column, row, EIconPos.EBottomRight);
    }
    
    public void insertColumn(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Insert_column");
    }
    
    public void insertColumnLeft(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Insert_column_left");
    }
    
    public void insertColumnRight(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Insert_column_right");
    }
    
    public void pasteColumnsLeft(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste_columns_left");
    }
    
    public void pasteColumnsRight(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste_columns_right");
    }
    
    
    public void insertRowAbove(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Insert_row_above");
    }
    
    public void insertRowBelow(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Insert_row_below");
    }
    
    public void pasteRowsAbove(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste_rows_above");
    }
    
    public void pasteRowsBelow(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste_rows_below");
    }
    
    void pasteColumns(int column, int row) {
        execContextMenuCmd(column, row, "command.name.KTable_Paste_columns");
    }


    /**
     * This method is specific for Stub and Test point steps.
     * 
     * @param numExpect number of expected expressions
     * @param varNames assigned variables
     * @param numScriptParams number of script parameters
     * @param numRows number of rows
     */
    public void setTableDimensions(int numExpect, String [] varNames, int numScriptParams, int numRows) {
        
        // add rows
        for (int i = 0; i < numRows; i++) {
            m_ktable.click(1, i + 1); 
        }
        
        // add expect fields
        if (numExpect > 0) {
            m_ktable.clickDecoration(1, 0, EIconPos.ETopRight);
            for (int i = 1; i < numExpect; i++) {
                m_ktable.clickDecoration(1, 1, EIconPos.ETopRight);
            }
        }
        
        // add script params before assignments, because assignments are erased
        // when column is added if there is only var name without value in a table
        if (numScriptParams > 0) {
            int scriptParamsColumn = numExpect == 0 ? 3 : 2 + numExpect; 

            m_ktable.clickDecoration(scriptParamsColumn, 0, EIconPos.ETopRight);
            for (int i = 1; i < numScriptParams; i++) {
                m_ktable.clickDecoration(scriptParamsColumn, 1, EIconPos.ETopRight);
            }
        }
        
        // add assignments
        int varNamesColumn = numExpect == 0 ? 2 : 1 + numExpect; 
        if (varNames.length > 0) {
            // if numExpect == 0, one empty column is still used by top level tag
            addMapColumn(varNamesColumn, 0, varNames[0]);
            for (int i = 1; i < varNames.length; i++) {
                addMapColumn(varNamesColumn + i, 1, varNames[i]);
            }
        }
    }

    
    // this method is for backward compatibility only
    public void setSectionStubAssignments(int row, String varName, String varValue, 
                                          String []scriptParams) {
        
        if (row != 0) {
            throw new IllegalArgumentException("For compatiliby reasons row must always be set to 0!");
        }
        setTableDimensions(0, new String[]{varName}, scriptParams.length, 1);
        setDataCell(1, 0, varValue);
        
        for (int i = 0; i < scriptParams.length; i++) {
            setDataCell(2 + i, 0, scriptParams[i]);
        }
    }


    /**
     * Adds the given number of rows regardless of the number of existing rows.
     * @param numRows
     */
    public void addRows(int numRows) {
        
        int startNumRows = m_ktable.getRowCount();
        
        // add rows
        for (int i = 0; i < numRows; i++) {
            m_ktable.click(1, startNumRows + i - 1); 
        }
    }
    

    /**
     * Adds rows until the table contains the given number of rows. If the table 
     * already contains the given number of rows or more, nothing is changed.
     * @param numRows number of data rows, without header row(s)
     */
    public void addRowsUntil(int numRows) {
        
        int startNumRows = m_ktable.getRowCount() - m_ktable.getHeaderRowCount();
        addRows(numRows - startNumRows);
    }
    

    /**
     * @param column column with '+' sign to click
     * @param numRows table should contain that number of rows after this method returns
     */
    public void addRows(int column, int numRows) {
        
        int startNumRows = m_ktable.getRowCount();
        
        // add rows
        for (int i = 0; i < numRows; i++) {
            m_ktable.click(column, startNumRows + i - 1); 
        }
    }
    

    /**
     * Adds rows until the table contains the given number of rows. If the table 
     * already contains the given number of rows or more, nothing is changed.
     * 
     * @param column column with '+' sign to click
     * @param numRows table should contain that number of rows after this method returns
     */
    public void addRowsUntil(int column, int numRows) {
        
        int startNumRows = m_ktable.getRowCount() - m_ktable.getHeaderRowCount();
        addRows(column, numRows - startNumRows);
    }
    

    public void setInitSeqAction(int dataRow, String coreId, String keySeq, String ... params) {
        
        addRowsUntil(dataRow + 2); // row is 0-based, the last row contains +
        
        setDataCell(0, dataRow, coreId);
        selectDataItemInCombo(1, dataRow, keySeq);

        int paramsCol = 2;
        for (String str : params) {
            // add column if necessary
            if (paramsCol >= (m_ktable.getColumnCount() - 1)) {
                m_ktable.clickDecoration(paramsCol - 1, 1, EIconPos.ETopRight);
            }
            setDataCell(paramsCol++, dataRow, str);
        }
    }


    public void setStackUsageParams(int row, String coreId, boolean isAcive, 
                                    String baseAddr, String endAddr, String pattern) {
        
        addRowsUntil(row + 2); // row is 0-based
        
        setDataCell(0, row, coreId);

        if (isAcive) {
            toggleDataCheckBox(1, row, 1);
        }
        setDataCell(2, row, baseAddr);
        setDataCell(3, row, endAddr);
        setDataCell(4, row, pattern);
        
    }
}
