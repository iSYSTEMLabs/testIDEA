package si.isystem.itest.xls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SException;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.tbltableeditor.ArrayTableCell;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.HeaderNode;
import si.isystem.tbltableeditor.SectionNames;

abstract public class TableExporter {

    // deprecated or otherwise unused sections in Excel export 
    public static final int[] REMOVED_TEST_SPEC_SECTIONS = 
                    new int[]{SectionIds.E_SECTION_BASE_ID.swigValue(),
                              SectionIds.E_SECTION_PARAMS_PRIVATE.swigValue(),
                              SectionIds.E_SECTION_TRACE.swigValue(), // deprecated, moved to ANALYZER
                              SectionIds.E_SECTION_COVERAGE.swigValue(), // deprecated, moved to ANALYZER
                              SectionIds.E_SECTION_PROFILER.swigValue(), // deprecated, moved to ANALYZER
                              SectionIds.E_SECTION_TESTS.swigValue()};
    
    abstract protected void createSheet(String testId, int numHeaderRows) throws IOException;
    abstract protected void formatSheet(HeaderNode header,
                                        int numHeaderRows,
                                        int freezeRows,
                                        int freezeColumns) throws IOException;
    abstract protected void writeOutput(String fileName) throws IOException;

    abstract protected void initColors(HSSFColorTableModel visibilityAndColors, 
                                       CTestSpecification testSpec);
    
    abstract protected void formatHeaderCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                                             int sectionId,
                                             int col);

    abstract protected void createCurrentRow(int row) throws IOException;

    abstract protected void setCellValue(double dblVal) throws IOException;
    abstract protected void setCellValue(String value) throws IOException;
    
    abstract protected void formatBodyCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                                           int sectionId,
                                           boolean isMediumBoldBorder,
                                           int bodyRow,
                                           int col);
    
    abstract protected void setCellComment(String commentText);
    
    abstract protected void addComboBox(int row, int column, String [] choices);
    
    
    public void toTable(CTestSpecification testSpec, 
                        String fileName,
                        XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        
        HSSFColorTableModel hssfModel = xlsExportLookAndFeel.getVisibilityAndColors();

        initColors(hssfModel, testSpec);
        
        TreeMap<String, int[]> removedSections = getRemovedSections(testSpec);
        TreeMap<String, int[]> removedEmptySections = getRemovedEmptySections(hssfModel, 
                                                                              testSpec);
        TreeMap<String, int[]> rootOrderedSections = getOrderedSections(hssfModel,
                                                                        testSpec);
        
        
        for (int specIdx = 0; specIdx < testSpec.getNoOfDerivedSpecs(); specIdx++) {
            CTestSpecification selectedTS = testSpec.getDerivedTestSpec(specIdx);
            
            ArrayTableModel tableModel = new ArrayTableModel(null, false);
            tableModel.setSubtractiveFilters(removedSections, 
                                             removedEmptySections, 
                                             rootOrderedSections);
            
            // create model with the parent test spec in the first row
            tableModel.setModelData(selectedTS, SectionIds.E_SECTION_TESTS.swigValue(), selectedTS);
            
            HeaderNode header = tableModel.getHeader();
            if (selectedTS.getTestScope() == ETestScope.E_UNIT_TEST) {
                functionParamsToHeader(selectedTS, header);
            }
            
            int numHeaderRows = header.getRowCount();
            createSheet(selectedTS.getTestId(), numHeaderRows);

            for (int row = 0; row < numHeaderRows; row++) {
                createHeaderRow(row, header, xlsExportLookAndFeel);
            }
            
            int numBodyRows = tableModel.getRowCount();
            for (int row = 0; row < numBodyRows; row++) {
                createBodyRow(row + numHeaderRows, header, tableModel,
                              xlsExportLookAndFeel);
            }

            int freezeRows = 0;
            int freezeColumns = 0;
            if (xlsExportLookAndFeel.isFreezeHeaderRows()) {
                freezeRows = numHeaderRows + 1; // add also row with values from base test spec 
            }
            if (xlsExportLookAndFeel.isFreezeTestIdColumn()) {
                freezeColumns = 1; // the first column (test ID)
            }
            formatSheet(header, numHeaderRows, freezeRows, freezeColumns);
        }
        
        // Write the output to a file
        writeOutput(fileName);
    }
    

    /**
     * This mapping contains name of parent section as key, and list of sections to show for
     * children. This works at the moment, but in the future it may be a problem if there are
     * two sections in different CTestBase classes with the same name. Proper solution would be 
     * to introduce paths, for example '/analyzer/profiler/codeAreas/0/netTime', and then
     * get removed/sorted/visible sections based on regex, for example:
     *   /analyzer/profiler/codeAreas/\d+  
     * 
     * @param testSpec
     * @return
     */
    private TreeMap<String, int[]> getRemovedSections(CTestSpecification testSpec) {
        TreeMap<String, int[]> removedSections = new TreeMap<>();
        
        removedSections.put("",  // root header node has no section name assigned 
                            REMOVED_TEST_SPEC_SECTIONS);
        
        // ret val name has no meaning for script functions
        removedSections.put(testSpec.getTagName(SectionIds.E_SECTION_INIT_TARGET.swigValue()),   
                            new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        removedSections.put(testSpec.getTagName(SectionIds.E_SECTION_INITFUNC.swigValue()),   
                            new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        removedSections.put(testSpec.getTagName(SectionIds.E_SECTION_ENDFUNC.swigValue()),   
                            new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        removedSections.put(testSpec.getTagName(SectionIds.E_SECTION_RESTORE_TARGET.swigValue()),   
                            new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        return removedSections;
    }

    
    private TreeMap<String, int[]> getOrderedSections(HSSFColorTableModel hssfModel,
                                                      CTestSpecification testSpec) {
        
        TreeMap<String, int[]> rootOrderedSections = new TreeMap<>();
        int numOrderedSectins = hssfModel.getRowCount() - 1; // -1 for header row in table
        int [] orderedSections = new int[numOrderedSectins];
        
        for (int row = 0; row < numOrderedSectins; row++) {
            String sectionName = hssfModel.getSectionName(row);
            int sectionId = testSpec.getSectionId(sectionName);
            orderedSections[row] = sectionId;
        }
        
        rootOrderedSections.put(SectionNames.TEST_SPEC.getClassName(), orderedSections);
        
        return rootOrderedSections;
    }

    
    /** 
     * Converts pairs [sectionName, isVisible] to list of <sectionId as int> for isVisible == false.
     *  
     * @param visibilityAndColors contains information provided by the user with selecting wanted 
     *                            sections in a dialog. 
     * @param testSpec used to convert 'String section name' to 'int section id'. 
     */
    private TreeMap<String, int[]> getRemovedEmptySections(HSSFColorTableModel visibilityAndColors, 
                                                           CTestSpecification testSpec) {
        
        TreeMap<String, int[]> removedEmptySections = new TreeMap<>();
        List<Integer> removedSections = new ArrayList<>();
        
        int numRows = visibilityAndColors.getRowCount() - 1; // -1 for header row
        for (int row = 0; row < numRows; row++) {

            if (!visibilityAndColors.isVisible(row).booleanValue()) {
                String sectionName = visibilityAndColors.getSectionName(row);
                try {
                    int id = testSpec.getSectionId(sectionName);
                    removedSections.add(id);
                } catch (Exception ex) {
                    ex.printStackTrace(); // ignore sections in model, which no longer exist in
                                          //  test spec
                }
            }
        }
        
        removedEmptySections.put("",  // root header node has no section name assigned 
                                 ArrayUtils.toPrimitive(removedSections.toArray(new Integer[0])));

        return removedEmptySections;
    }

    
    private void createHeaderRow(int row,
                                 HeaderNode header, 
                                 XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {

        createCurrentRow(row);
        int numColumns = header.getColumnCount();
        int sectionId = 0;
        
        for (int col = 0; col < numColumns; col++) {

            sectionId = getMainSectionId(header, sectionId, col);

            formatHeaderCell(xlsExportLookAndFeel, sectionId, col);
            
            HeaderNode node = header.getNode(col, row);
            
            if (node != null) {
                String value = node.getVisibleName();
                try {
                    double dblVal = Double.parseDouble(value);
                    setCellValue(dblVal);
                } catch (NumberFormatException ex) {
                    setCellValue(value);
                }
            } else {
                setCellValue(null);
            }
        }
    }

    
    private void createBodyRow(int row,
                               HeaderNode header,
                               ArrayTableModel tableModel,
                               XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {

        createCurrentRow(row);
        
        int numColumns = tableModel.getColumnCount();
        int sectionId = 0;
        boolean isMediumBoldBorder = row == header.getRowCount();
        int bodyRow = row - header.getRowCount();
        
        for (int col = 0; col < numColumns; col++) {
            
            sectionId = getMainSectionId(header, sectionId, col);
            
            formatBodyCell(xlsExportLookAndFeel,
                           sectionId,
                           isMediumBoldBorder,
                           bodyRow,
                           col);
            
            ArrayTableCell arrayCell = tableModel.getDataValue(col, row - header.getRowCount());
            
            if (arrayCell != null  &&  arrayCell.existsInModel()) {

                setCellComment(arrayCell.getComment());

                String value = arrayCell.getValue();
                
                switch (arrayCell.getEditorType()) {
                case ECombo:
                    addComboBox(row, col, arrayCell.getComboItems());
                    setCellValue(value);
                    break;
                case ETristate:
                    // replace bool values true/false with yes/no, because 
                    // Excel treats true and false in a special way                    
                    String [] tristate;
                    if (arrayCell.getDefaultForTristate()) {
                        tristate = new String[]{"__yes__", "no", "yes"};
                    } else {
                        tristate = new String[]{"__no__", "no", "yes"};
                    }
                    addComboBox(row, col, tristate);
                    switch (value) {
                    case "true":
                        value = "yes";
                        break;
                    case "false":
                        value = "no";
                        break;
                    case "":
                        /* default value should be empty in table also - otherwise
                         * it seems full of values even when they are not set.
                         * if (tableCell.getDefaultForTristate()) { value = "__yes__";
                           } else { value = "__no__"; } */
                    }
                    setCellValue(value);
                    break;
                case EText:
                default:
                    try {
                        double dblVal = Double.parseDouble(value);
                        setCellValue(dblVal);
                    } catch (NumberFormatException ex) {
                        setCellValue(value);
                    }
                }
                
            } else {
                setCellValue(null);
            }
        }
    }
    
    
    private int getMainSectionId(HeaderNode header, int sectionId, int col) {
        HeaderNode headerCell = header.getNode(col, 0);
        if (headerCell != null) {
            sectionId = headerCell.getSectionId();
        }
        return sectionId;
    }
    
    
    /**
     * Sets names of parameters as obtained from winIDEA symbol info to header.
     * @param testSpec test spec containing function under test
     * @param header
     */
    private void functionParamsToHeader(CTestSpecification testSpec, HeaderNode header) {
        String functionName = testSpec.getFunctionUnderTest(true).getName();

        String coreId = testSpec.getCoreId();
        coreId = TestSpecificationModel.getActiveModel().getConfiguredCoreID(coreId);
        FunctionGlobalsProvider funcProvider = GlobalsConfiguration.instance().
                           getGlobalContainer().getFuncGlobalsProvider(coreId);
        
        JFunction funcInfo = null;
        try {
            funcInfo = funcProvider.getCachedFunction(functionName);
        } catch (SException ex) {
            // ignore, user should notice error message in status view and Function section
            // there is no proper place to put the error message. Maybe rethrow exception?
        }
        
        JVariable[] paramInfo = null;
        if (funcInfo != null) {
            paramInfo = funcInfo.getParameters();
        }
        
        HeaderNode headerFunc = header.getChild(testSpec.getTagName(SectionIds.E_SECTION_FUNC.swigValue()));
        CTestFunction func = testSpec.getFunctionUnderTest(true);
        HeaderNode headerParams = headerFunc.getChild(func.getTagName(CTestFunction.ESection.E_SECTION_PARAMS.swigValue()));
        
        if (paramInfo != null) {
            // we have debug info, let's use it
            int numHeaderParams = headerParams.getNumChildren();
            for (int i = 0; i < paramInfo.length; i++) {
                String paramName = paramInfo[i].getVarTypeName() + " " + 
                        paramInfo[i].getName();
                if (i < numHeaderParams) {
                    headerParams.getChild(i).setVisibleName(paramName);
                }
            }
        } 
    }
}
