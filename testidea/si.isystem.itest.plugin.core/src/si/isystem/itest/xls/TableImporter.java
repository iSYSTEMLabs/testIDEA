package si.isystem.itest.xls;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.HeaderNode;

abstract public class TableImporter {

    private static final char COMMENT_CHAR = '#';
    public static final String NUM_HEADER_ROWS = "hr: ";
    protected static final int TEST_CASE_NUM_LIMIT_FOR_UNDO_CLEAR = 10000;
    
    // node ID is not used here, because table importer has no undo 
    private ENodeId m_defaultNodeId = null;
    
    private Exception m_exception;
    private GroupAction m_importAction;
    
    // do not reorder - see ExportImport dialog!
    public enum EImportScope {ECreateNewTestCases,  // all imported test cases will create new test cases in testIDEA
                              EToExistingAndNew,    // imported test cases which IDs match existing will overwrite
                                                    // existing derived tests, new ones will be added
                              EToExistingTestCases, // only existing tests which ID matched 
                                                    // with the imported ones, will get derived tests imported
                              EToSelectedTestCases, // only selected tests will get derived tests imported 
                              };
                              
    /** Returns number of sheets. */                              
    abstract protected int createWorkbook(String fileName) throws Exception;
    /** Returns number of rows in all sheets. */
    abstract protected int getNumAllRows();
    /** Selects sheet at index i, returns its name. */
    abstract protected String setCurrentSheet(int i) throws IOException;
    /** Returns the number of header rows in a selected sheet. */
    abstract protected int getNumHeaderRows();
    /** Returns header cells. */
    abstract protected List<List<String>> getHeaderCells(int numHeaderRows) throws IOException;
    /** Returns the number of rows in the selected sheet. */
    abstract protected int getNumRowsInCurrentSheet() throws IOException;
    /** Selects the row at index 'row', returns the number of columns in the row. */
    abstract protected int setCurrentRow(int row);
    /** Returns cell value in the selected sheet in the selected row in the given column. */
    abstract protected String getCellValue(int col, StringBuilder outComment);
    abstract protected void close() throws IOException;

    /**
     * 
     * @param containerTestSpec container test spec, which contains all test specs, which
     * should get derived test specs from XLS. 
     * @param fileName
     * @param searchDepth defines how deep the containerTestSpec is searched for
     *                    derived test specifications. 0 means the first level only,
     *                    -1 means all levels.
     * 
     * @return array of strings with warnings, which have occurred during import
     * 
     * @throws IOException
     * @throws InvalidFormatException
     * @throws InterruptedException 
     * @throws InvocationTargetException 
     */
    public String[] importFromFile(final CTestSpecification containerTestSpec, 
                                   final String fileName,
                                   final EImportScope importScope) throws Exception {

        final TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        final List<String> warnings = new ArrayList<String>();
        m_exception = null;

        final int numSheets = createWorkbook(fileName);
        final int sumRows = getNumAllRows();

        boolean _isClearUndoRedoHistory = false;
        if (sumRows > TEST_CASE_NUM_LIMIT_FOR_UNDO_CLEAR) {
            if (MessageDialog.openQuestion(Activator.getShell(), 
                                      "Large import",
                                      "Importing large number of test specifications - " + 
                                      "Undo/Redo histoy will be cleared!\nContinue?") == false) {
                return null;
            }
            model.clearUndoRedoHistory();
            _isClearUndoRedoHistory = true;
        }

        final boolean isClearUndoRedoHistory = _isClearUndoRedoHistory;
        m_importAction = new GroupAction("Excel Import");

        PlatformUI.getWorkbench().getProgressService()
        .busyCursorWhile(new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
                try {

                    monitor.beginTask("Importing test cases from '" + fileName + "'", numSheets);

                    for (int i = 0; i < numSheets; i++) {
                        
                        String testId = setCurrentSheet(i);
                        
                        CTestSpecification testSpec = null;
                        boolean isImportBase = false; // if true, the base test spec is also imported
                        
                        switch (importScope) {
                        case ECreateNewTestCases:
                            testSpec = createAndAddTestSpec(model, 
                                                            containerTestSpec,
                                                            isClearUndoRedoHistory);
                            isImportBase = true;
                            break;
                        case EToExistingAndNew:
                            testSpec = containerTestSpec.findDerivedTestSpec(testId, 
                                                                             -1);
                            if (testSpec == null) {
                                testSpec = createAndAddTestSpec(model,
                                                                containerTestSpec,
                                                                isClearUndoRedoHistory);
                                isImportBase = true;
                            }
                            break;
                        case EToExistingTestCases:
                            testSpec = containerTestSpec.findDerivedTestSpec(testId, 
                                       -1); // import to all test specs, which IDs match, 
                                            // regardless of their position in hierarchy
                            break;
                        case EToSelectedTestCases:
                            testSpec = containerTestSpec.findDerivedTestSpec(testId, 
                                         0); // import only to the selected test specs
                            break;
                        default:
                            break;
                        
                        }

                        monitor.subTask("Importing sheet " + testId + ", " + i + "/" + numSheets);
                        monitor.worked(1);
                        if (testSpec != null) {
                            xls2ArrayTableModel(model, testSpec, isImportBase,
                                                isClearUndoRedoHistory);
                        } else {
                            warnings.add("Test spec with id '" + testId + "' not imported, because " +
                                    "there is no test spec with this ID,\nor no such test spec. was selected.");
                        }
                    }

                    monitor.done();

                } catch (Exception ex) {
                    m_exception = ex;
                } finally {
                    try {
                        close();
                    } catch (Exception ex) {
                        throw new InvocationTargetException(ex, "Can not close input file!");
                    }
                }
            }
        });
        
        if (isClearUndoRedoHistory) {
            model.getEventDispatcher().fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
                                                                       null, 
                                                                       null));
        }
        
        if (m_exception != null) {
            throw m_exception;
        }
        
        if (m_importAction != null) {
            m_importAction.addAllFireEventTypes();
            model.execAction(m_importAction);
        }
        
        return warnings.toArray(new String[0]);
    }

    
    private void xls2ArrayTableModel(TestSpecificationModel model, 
                                     CTestSpecification testSpec, 
                                     boolean isImportBase, 
                                     boolean isClearUndoRedoHistory) throws IOException {

        int numHeaderRows = getNumHeaderRows();
        
        List<List<String>> headerCells = getHeaderCells(numHeaderRows);

        // this instance is used only to obtain section IDs and create new instances of children
        CTestSpecification testBase = new CTestSpecification();
        
        testSpec.deleteAllDerivedTestSpecs();
        HeaderNode rootNode = new HeaderNode(ETestObjType.ETestBase, "", m_defaultNodeId);
        rowsToHeader(0, 0, headerCells, rootNode, testBase, numHeaderRows);
        // System.out.println("rootNode table:\n" + rootNode.toTable());
        rowsToTestSpecs(numHeaderRows, rootNode, model, testSpec, isImportBase,
                        isClearUndoRedoHistory);
    }
    
    
    /**
     * This method converts list of cells to hierarchical structure of HeaderNode-s.
     * It is called recursively for all rows in a header.
     * 
     * @param row row in header to convert to HeaderNode-s
     * @param startColumn column to start conversion
     * @param headerCells string matrix with names of itest tags or mapping items 
     * @param currentHNode 
     * @param testBase
     * @param numHeaderRows
     * 
     * @return column number where parsing stopped. Next cell may continue info
     *                for the next HeaderNode.
     */
    private int rowsToHeader(int row, 
                             int startColumn,
                             List<List<String>> headerCells,
                             HeaderNode currentHNode,
                             CTestBase testBase,
                             int numHeaderRows) {
        
        HeaderNode childNode = null;
        List<String> cellRow = headerCells.get(row);
        int numHeaderItems = cellRow.size();
        
        for (int column = startColumn; column < numHeaderItems; column++) {
            String sectionName = cellRow.get(column);
            // System.out.println("sectionName: " + sectionName + " / " + column);
            
            String sectionAbove = null;
            if (row > 0  &&  column > startColumn) {
                sectionAbove = headerCells.get(row - 1).get(column);
            }
            
            if (isNullOrEmpty(sectionName)) {
                // no more info for the current HeaderNode
                if (column == startColumn) {
                    // Prevent endless loop in the case of corrupt data.
                    // This happens, if test base name is in the table, but the row below it is empty.
                    // Example of this problem appeared when there were too many header rows specified in 
                    // the first cell comment - 4, but only 3 were present. This method cyled indefinitely 
                    // in this case.
                    throw new SIllegalStateException("Illegal data format! Cell of subsection is empty!").
                        add("column", column).
                        add("rowIdx", row).
                        add("row", cellRow);
                }
                return column - 1;
            }
            
            if (!isNullOrEmpty(sectionAbove)) {
                // no more info for the current HeaderNode
                return column - 1;
            }
            
            int sectionId = testBase.getSectionId(sectionName);

            ETestObjType sectionType = testBase.getSectionType(sectionId);

            switch (sectionType) {

            case EYAMLBool:
            case EYAMLEnum:
            case EYAMLScalar:
                currentHNode.add(sectionName,
                                 sectionId, sectionType, m_defaultNodeId);
                break;

            case EYAMLMap:
                childNode = currentHNode.add(sectionName, 
                                             sectionId, sectionType, m_defaultNodeId);
                if (row < numHeaderRows - 1) { // it is not the last header 
                    // row without values for mapping
                    List<String> mapKeyRow = headerCells.get(row + 1);
                    String mapKeyStr = mapKeyRow.get(column);
                    if (mapKeyStr == null) {
                        mapKeyStr = "";
                    }
                    
                    childNode.add(mapKeyStr, sectionId, ETestObjType.EYAMLScalar, m_defaultNodeId);
                    column++;
                    if (column < numHeaderItems) {

                        mapKeyStr = mapKeyRow.get(column);

                        while (isNullOrEmpty(cellRow.get(column))) {

                            if (mapKeyStr == null) {  // one empty item in mapping is allowed
                                mapKeyStr = "";
                            }

                            childNode.add(mapKeyStr, sectionId, ETestObjType.EYAMLScalar, m_defaultNodeId);
                            column++;
                            if (column >= numHeaderItems) {
                                break;
                            }
                            mapKeyStr = mapKeyRow.get(column);
                        }
                    }
                    column--;
                }
                break;

            case EYAMLSeqence:
                childNode = currentHNode.add(sectionName, sectionId, sectionType, m_defaultNodeId);

                if (row < numHeaderRows - 1) { // it is not the last header 
                    // row without values for sequence
                    List<String> seqIdxRow = headerCells.get(row + 1);
                    String seqIdxStr = seqIdxRow.get(column);
                    if (!isNullOrEmpty(seqIdxStr)) {
                        childNode.add("0", sectionId, ETestObjType.EYAMLScalar, m_defaultNodeId);
                        int seqStartCol = column; // use this index, not content of cells, 
                        // because cells may contain other text, for example 
                        // names of function parameters 
                        column++;
                        if (column < numHeaderItems) {
                            seqIdxStr = seqIdxRow.get(column);

                            while (!isNullOrEmpty(seqIdxStr)  &&  isNullOrEmpty(cellRow.get(column))) {

                                childNode.add(String.valueOf(column - seqStartCol), 
                                              sectionId, // seq. items refer
                                              // to the same section as parent sequence object
                                              ETestObjType.EYAMLScalar, m_defaultNodeId);
                                column++;
                                
                                if (column >= numHeaderItems) {
                                    break;
                                }

                                seqIdxStr = seqIdxRow.get(column);
                            }
                        }
                        column--;
                    }
                }
                break;

            case ETestBase:
                CTestBase childTb = testBase.createTestBase(sectionId);
                childNode = currentHNode.add(sectionName, sectionId, sectionType, m_defaultNodeId);
                column = rowsToHeader(row + 1, column, headerCells, childNode, 
                                      childTb, numHeaderRows);
                break;

            case ETestBaseList:
                childNode = currentHNode.add(sectionName, sectionId, sectionType, m_defaultNodeId);

                if (row < numHeaderRows - 1) { // it is not the last header 
                    // row without values for sequence
                    List<String> seqIdxRow = headerCells.get(row + 1);
                    String seqIdxStr = seqIdxRow.get(column);
                    if (!isNullOrEmpty(seqIdxStr)) {
                        int seqStartCol = column; // use this index, not content of cells, 
                        // because cells may contain other text, for example 
                        // names of function parameters 
                        column = addTestBaseToList(row,
                                                   headerCells,
                                                   testBase,
                                                   numHeaderRows,
                                                   childNode,
                                                   column,
                                                   sectionId,
                                                   seqStartCol);
                        
                        column++;
                        
                        if (column < numHeaderItems) {
                            
                            seqIdxStr = seqIdxRow.get(column);
                            
                            while (!isNullOrEmpty(seqIdxStr)  &&  isNullOrEmpty(cellRow.get(column))) {
                                column = addTestBaseToList(row,
                                                           headerCells,
                                                           testBase,
                                                           numHeaderRows,
                                                           childNode,
                                                           column,
                                                           sectionId,
                                                           seqStartCol);
                                column++;
                                
                                if (column >= numHeaderItems) {
                                    break;
                                }
                                seqIdxStr = seqIdxRow.get(column);
                            }
                        }
                        column--;
                    }
                }
                break;
            default:
                break;
            }
        }
        
        return numHeaderItems;
    }


    private int addTestBaseToList(int row,
                                  List<List<String>> headerCells,
                                  CTestBase testBase,
                                  int numHeaderRows,
                                  HeaderNode childNode,
                                  int column,
                                  int sectionId,
                                  int seqStartCol) {
        HeaderNode arrayElemNode = 
                childNode.add(String.valueOf(column - seqStartCol), 
                              sectionId, // seq. items refer
                                         // to the same section as parent sequence object
                              ETestObjType.ETestBase, m_defaultNodeId);
        
        CTestBase listTestBase = testBase.createTestBase(sectionId);
        column = rowsToHeader(row + 2, column, headerCells, 
                              arrayElemNode, listTestBase, numHeaderRows);
        return column;
    }
    
    
    private boolean isNullOrEmpty(String str) {
    
            return str == null  ||  str.isEmpty();
    }
    
    
    private void rowsToTestSpecs(int startRow,
                                 HeaderNode header,
                                 TestSpecificationModel model, 
                                 CTestSpecification parentTestSpec, 
                                 boolean isImportBase, 
                                 boolean isClearUndoRedoHistory) throws IOException {

        if (!isImportBase) {
            startRow++; // skip the base test in table
        }
        
        int numRows = getNumRowsInCurrentSheet();
        GroupAction importAction = null;
        
        for (int row = startRow; row < numRows; row++) {
            CTestSpecification testSpec;

            if (isImportBase) {
                // use the given test spec 
                testSpec = parentTestSpec;
                isImportBase = false;
                
            } else {
                
                if (isClearUndoRedoHistory) {
                    parentTestSpec.deleteAllDerivedTestSpecs();
                } else {
                    importAction = new GroupAction("Import");
                    CTestBench tb = new CTestBench();
                    tb.getTestSpecification(false).getChildren(false).assign(parentTestSpec.getChildren(true));
                    DeleteTestTreeNodeAction.fillGroupAction(importAction, tb);
                }
                
                testSpec = createAndAddTestSpec(model,
                                                parentTestSpec,
                                                isClearUndoRedoHistory);
            }

            rowToTestBase(row, 0, header, testSpec);
        }
    }

    
    private void rowToTestBase(int row,
                               int startColumn,
                               HeaderNode header,
                               CTestSpecification testSpec) {

        int numColumns = setCurrentRow(row);

        for (int col = startColumn; col < numColumns; col++) {
            
            String value = null;
            
            try {
                StringBuilder comment = new StringBuilder();
                value = getCellValue(col, comment);
                // Cell cell = dataRow.getCell(col);
                // String value = cell.getStringCellValue().trim();

                if (value == null  ||  value.isEmpty()) {
                    continue;
                }

                if (value.charAt(0) == COMMENT_CHAR) { // skip comment till the end of line
                    break;
                }

                HeaderNode columnHeader = header.getFirstNonEmptyCellBottomUp(col);
                if (columnHeader == null) {
                    throw new IllegalArgumentException("Missing header! Make sure there are no empty columns in the table.");
                }
                int sectionId = columnHeader.getSectionId();

                CTestBase testBase = columnHeader.getTestBase(testSpec, false);
                ETestObjType sectionType = testBase.getSectionType(sectionId);

                switch (sectionType) {

                case EYAMLBool:
                    // 'true' and 'false' are converted to 'yes' and 'no' on
                    // export (see TebleExporter.java) becasue Excel treats true and false
                    // in a special way. Revert conversion here.
                    if (value.equals("yes")) {
                        value = "true";
                    } else if (value.equals("no")) {
                        value = "false";
                    }
                case EYAMLEnum:
                case EYAMLScalar:
                    testBase.setTagValue(sectionId, value);
                    setMixedComment(testBase, sectionId, comment.toString());
                    break;

                case EYAMLMap:
                    String key = columnHeader.getName();
                    testBase.setTagValue(sectionId, key, value);
                    setUserMappingComment(testBase, key, comment.toString());
                    break;

                case EYAMLSeqence:
                    int seqIdx = Integer.parseInt(columnHeader.getName());
                    CSequenceAdapter seq = new CSequenceAdapter(testBase, sectionId, false);
                    while (seq.size() < seqIdx) {
                        seq.add(-1, "");
                    }
                    seq.add(-1, value);
                    setListItemComment(testBase, sectionId, seqIdx, comment.toString());
                    break;

                case ETestBase:
                case ETestBaseList:
                    // if the bottom most header cell is of one of these two types,
                    // it means empty test base or list. If not empty, scalar is the
                    // bottom most type
                    break;
                default:
                    break;
                }
            } catch (Exception ex) {
                throw new SIOException("Import from Excel failed!", ex)
                         .add("column", col)
                         .add("row", row)
                         .add("contents", value);
            }
        }

    }


    private CTestSpecification createAndAddTestSpec(TestSpecificationModel model,
                                                    final CTestSpecification containerTestSpec,
                                                    final boolean isClearUndoRedoHistory) {
        
        CTestSpecification testSpec = new CTestSpecification(containerTestSpec);
        
        if (isClearUndoRedoHistory) {
            containerTestSpec.addDerivedTestSpec(-1, testSpec);
        } else {
            m_importAction.add(new AddTestTreeNodeAction(model,
                                                         containerTestSpec, 
                                                         -1, 
                                                         testSpec));
        }
        
        return testSpec;
    }
    
    
    private void setMixedComment(CTestBase testBase, int section, String excelComment) {
        
        if (excelComment != null  &&  !excelComment.isEmpty()) {
            String[] comments = formatComments(excelComment);
            
            testBase.setComment(section,
                                SpecDataType.KEY, CommentType.NEW_LINE_COMMENT,
                                comments[0]);
            
            if (!comments[1].isEmpty()) {
                testBase.setComment(section,
                                    SpecDataType.VALUE, CommentType.END_OF_LINE_COMMENT,
                                    comments[1]);
            }
        }
    }

    
    private void setUserMappingComment(CTestBase testBase, String key, 
                                       String excelComment) {
        
        if (excelComment != null  &&  !excelComment.isEmpty()) {
            String[] comments = formatComments(excelComment);
      
            testBase.setComment(SectionIds.E_SECTION_INIT.swigValue(),
                                key, comments[0], comments[1]);
        }
    }

    
    private void setListItemComment(CTestBase testBase, int section, int idx, String excelComment) {
        
        if (excelComment != null  &&  !excelComment.isEmpty()) {
            String[] comments = formatComments(excelComment);
            
            testBase.setComment(section,
                                idx,
                                comments[0],
                                comments[1]);
        }
    }
    
    
    private String[] formatComments(String excelComment) {
        
        String comments[] = excelComment.split(YamlScalar.NL_EOL_COMMENT_SEPARATOR);
        String nlComment = UiUtils.addCommentChar(comments[0], 
                                                  ValueAndCommentEditor.DEFAULT_NEW_LINE_INDENT_STEP);
        String eolComment = "";
        
        if (comments.length > 1) {
            eolComment = UiUtils.addCommentChar(comments[1], 
                                                ValueAndCommentEditor.DEFAULT_EOL_INDENT);
        }
        
        return new String[]{nlComment, eolComment};
    }
}
