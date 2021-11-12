package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CLogResult;
import si.isystem.connect.CLogResult.ETestResultSections;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.StrVector;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;

/**
 * This dialog displays two KTables, each with 2 columns. The left one contains
 * expressions and values logged BEFORE test has been started, and the second 
 * one contains expressions and values logged AFTEr test has been executed.
 *  
 * @author markok
 *
 */
public class LogViewDialog extends Dialog {

    private CLogResult m_testBase;
    // static to preserve 
    private static MutableInt m_col_0_WidthBefore = new MutableInt(100);
    private static MutableInt m_col_0_WidthAfter = new MutableInt(100);


    public LogViewDialog(Shell parentShell, CLogResult testBase) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_testBase = testBase;
    }


    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Log");

        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.heightHint = 510; // sets initial dialog size
        gridData.widthHint = 800;
        mainDlgPanel.setLayoutData(gridData);

        mainDlgPanel.setLayout(new MigLayout("fill",
                                             "[fill, fill]",
                                             "[min!][fill]"));

        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.label("Before:");
        builder.label("After:", "gapleft 5,wrap");
        
        KTable table = createTable(mainDlgPanel, 
                                   ETestResultSections.E_SECTION_BEFORE_ASSIGN,
                                   m_col_0_WidthBefore);
        table.setLayoutData("gapright 5");

        table = createTable(mainDlgPanel, 
                            ETestResultSections.E_SECTION_AFTER_ASSIGN,
                            m_col_0_WidthAfter);
        table.setLayoutData("gapleft 5");
        
        return composite;
    }


    private KTable createTable(Composite mainDlgPanel, 
                               ETestResultSections section,
                               MutableInt colWidth0) {
        CMapAdapter map = new CMapAdapter(m_testBase, 
                                          section.swigValue(), 
                                          true);
        
        KTable ktable = new KTable(mainDlgPanel, 
                                   true, 
                                   SWTX.AUTO_SCROLL | SWTX.FILL_WITH_LASTCOL);
        ktable.setModel(new LogTableModel(map, colWidth0));
        return ktable;
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent,
                     IDialogConstants.OK_ID,
                     IDialogConstants.OK_LABEL,
                     true);
    }
}


class LogTableModel extends KTableModelAdapter {
    
    private static final int NUM_HEADER_ROWS = 1;
    private static final String[]m_header = new String[]{"Expression", "Value"};
    private CMapAdapter m_contents;
    private StrVector m_expressions;
    private MutableInt m_colWidth0;
    
    LogTableModel(CMapAdapter contents, MutableInt colWidth0) {
        m_contents = contents;
        m_expressions = new StrVector();
        m_contents.getKeys(m_expressions);
        m_colWidth0 = colWidth0;
    }

    
    @Override
    public int getFixedHeaderColumnCount() {
        return 0;
    }

    
    @Override
    public int getInitialRowHeight(int row) {
        return FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        return m_colWidth0.intValue();
    }
    
    
    @Override
    public void setColumnWidth(int col, int value) {
        if (col == 0) {
            m_colWidth0.setValue(value);
        }
    }

    
    @Override
    public int doGetRowCount() {
        return (int)m_contents.size() + NUM_HEADER_ROWS;
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {
        if (row == 0) {
            return m_header[col];
        }
        if (col == 0) {
            return m_expressions.get(row - NUM_HEADER_ROWS);
        }
        return m_contents.getValue(m_expressions.get(row - NUM_HEADER_ROWS));
    }
    
    
    @Override
    public int doGetColumnCount() {
        return 2;
    }
};
