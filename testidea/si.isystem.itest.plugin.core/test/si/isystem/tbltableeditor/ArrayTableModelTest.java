package si.isystem.tbltableeditor;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CLineDescription.EMatchingType;
import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CLineDescription.ESearchContext;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.ArrayTableCell;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.HeaderNode;


public class ArrayTableModelTest {

    @BeforeClass 
    public static void loadLib() {
        
        String libraryName = null;
        
        try {
            String architecture = System.getProperty("sun.arch.data.model");


            if (architecture.equals("64")) {
                libraryName = "lib/IConnectJNIx64";
            } else if (architecture.equals("32")) {
                libraryName = "lib/IConnectJNI";
            } else {
                throw new IllegalStateException("Unknown 32/64 bit architecture:" + architecture);
            }

            System.out.println("java.library.path = " + System.getProperty("java.library.path"));
            System.out.println("Loading native library: " + libraryName);
            System.loadLibrary(libraryName);
            System.out.println("Native library loaded.");
        } catch (Throwable thr) {
            System.err.println("Error loading library: " + libraryName);
            System.err.println("Error: " + thr.toString());
            thr.printStackTrace();
            return;
        }
    }
    
    @Test
    public void testModelFull() {
        
        CTestSpecification testSpec = new CTestSpecification(null);
        int sectionId = SectionIds.E_SECTION_TEST_POINTS.swigValue();
        CTestBaseList testPointList = testSpec.getTestBaseList(sectionId,
                                                               false);
        
        /*
         * testPointId | isActive | condCount | condExpr | scriptF | LOCATION                                                                             | LOG            | STEPS                                          |
         *             |          |           |          |         | resType | resName | line | isSearch | range | context | matchType | pattern | offset | numSteps | Before | After | expect            | assign  | script p. | next |
         *             |          |           |          |         |         |         |      |          |       |         |           |         |        |          | 0      | 0     | 0       | 1       | g_char1 | 0         |      |
         * ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         *     TP1     |  true    |    98     |  i < 4   | scriptF |  file   | myFName | 567  |          |   99  | code    | regEx     | searchP |    4   | 2        | g_char3| g_int9| a == 35 | ep < 56 |  'a'    | 10        | 23   |
         */
        
        CTestPoint tp1 = new CTestPoint();
        tp1.setId("TP1");
        tp1.setActive(ETristate.E_TRUE);
        tp1.setConditionCount(98);
        tp1.setConditionExpr("i < 4");
        tp1.setScriptFunc("scriptFunc");
        
        CTestLocation loc = tp1.getLocation(false);
        loc.setResourceType(EResourceType.E_RESOURCE_FILE);
        loc.setResourceName("myFileName");
        loc.setLine(567);
        loc.setSearch(ETristate.E_TRUE);
        loc.setLinesRange(99);
        loc.setMatchingType(EMatchingType.E_MATCH_REG_EX);
        loc.setSearchContext(ESearchContext.E_SEARCH_CODE);
        loc.setSearchPattern("searchPattern");
        loc.setLineOffset(4);
        loc.setNumSteps(2);
        
        CTestLog log = tp1.getLogConfig(false);
        CSequenceAdapter before = log.getExpressions(ESectionsLog.E_SECTION_BEFORE, false);
        before.add(-1, "g_char3");
        CSequenceAdapter after = log.getExpressions(ESectionsLog.E_SECTION_AFTER, false);
        after.add(-1, "g_int9");
        
        
        CTestBaseList steps = tp1.getSteps(false);
        CTestEvalAssignStep step = new CTestEvalAssignStep();
        
        CSequenceAdapter seq = step.getExpectedExpressions(false);
        seq.setValue(-1, "a == 35");    
        seq.setValue(-1, "ep < 56");
        
        CMapAdapter assign = step.getAssignments(false);
        assign.setValue("g_char1", "'a'");
        
        CSequenceAdapter scriptParams = step.getScriptParams(false);
        scriptParams.setValue(-1, "10");

        step.setStepIdx(23);

        steps.add(-1, step);
        
        testPointList.add(-1, tp1);
        
        ArrayTableModel atm = new ArrayTableModel(ENodeId.TEST_POINT_NODE, false);
        atm.setModelData(testSpec, sectionId);
        
        int numRows = atm.getRowCount();
        int numColumns = atm.getColumnCount();
        
        assertEquals(1, numRows);
        assertEquals(22, numColumns);
        
        String[] expected = {
            "TP1", "true", "98", "i < 4", "scriptFunc", 
            "file", "myFileName", "567", "true", "99", "code", "regEx", "searchPattern", "4", "2",  // location 
            "g_char3", "g_int9",                                                            // log 
            "a == 35", "ep < 56", "'a'", "10", "23"                                         // steps
            }; 
        int rowIndex = 0;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            String value = atm.getDataValue(columnIndex, rowIndex).getValue();
            if (value == null  &&  expected[columnIndex] == null) {
                continue;
            }
            // System.out.println(columnIndex + "__val = " + value);
            assertEquals(expected[columnIndex], value);
        }
    }
    
    
    @Test
    public void testModelAlmostFull() {
        
        CTestSpecification testSpec = new CTestSpecification(null);
        int sectionId = SectionIds.E_SECTION_TEST_POINTS.swigValue();
        CTestBaseList testPointList = testSpec.getTestBaseList(sectionId,
                                                               false);
        /*
         * testPointId | isActive | condCount | condExpr | scriptF | LOCATION                                                                             | LOG            | STEPS                                          |
         *             |          |           |          |         | resType | resName | line | isSearch | range | context | matchType | pattern | offset | numSteps | Before | After | expect            | assign  | script p. | next |
         *             |          |           |          |         |         |         |      |          |       |         |           |         |        |          |        |       | 0       | 1       | g_char1 | 0         |      |
         * ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         *     TP1     |  true    |    98     |  i < 4   | scriptF |  file   | myFName | 567  |          |   99  | code    |           | searchP |    4   |          |        |       | a == 35 | ep < 56 |  'a'    | 10        | 23   |
         */
        
        CTestPoint tp1 = new CTestPoint();
        tp1.setId("TP1");
        tp1.setActive(ETristate.E_TRUE);
        tp1.setConditionCount(98);
        tp1.setConditionExpr("i < 4");
        tp1.setScriptFunc("scriptFunc");
        
        CTestLocation loc = tp1.getLocation(false);
        loc.setResourceType(EResourceType.E_RESOURCE_FILE);
        loc.setResourceName("myFileName");
        loc.setLine(567);
        loc.setLineOffset(4);
        loc.setLinesRange(99);
        loc.setSearchContext(ESearchContext.E_SEARCH_CODE);
        loc.setSearchPattern("searchPattern");
        
        CTestBaseList steps = tp1.getSteps(false);
        CTestEvalAssignStep step = new CTestEvalAssignStep();
        
        CSequenceAdapter seq = step.getExpectedExpressions(false);
        seq.setValue(-1, "a == 35");    
        seq.setValue(-1, "ep < 56");
        
        CMapAdapter assign = step.getAssignments(false);
        assign.setValue("g_char1", "'a'");
        
        CSequenceAdapter scriptParams = step.getScriptParams(false);
        scriptParams.setValue(-1, "10");

        step.setStepIdx(23);

        steps.add(-1, step);
        
        testPointList.add(-1, tp1);
        
        ArrayTableModel atm = new ArrayTableModel(ENodeId.TEST_POINT_NODE, false);
        atm.setModelData(testSpec, sectionId);
        
        int numRows = atm.getRowCount();
        int numColumns = atm.getColumnCount();
        
        assertEquals(1, numRows);
        assertEquals(22, numColumns);
        
        String[] expected = {
            "TP1", "true", "98", "i < 4", "scriptFunc", 
            "file", "myFileName", "567", "", "99", "code", "", "searchPattern", "4", "",  // location 
            "", "",                                                               // log 
            "a == 35", "ep < 56", "'a'", "10", "23"                                   // steps
            }; 
        int rowIndex = 0;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            ArrayTableCell arrayCell = atm.getDataValue(columnIndex, rowIndex);
            if (arrayCell == null) {
                continue;
            }
            String value = arrayCell.getValue();
            if (value == null  &&  expected[columnIndex] == null) {
                continue;
            }
            // System.out.println(columnIndex + "__val = " + value);
            assertEquals(expected[columnIndex], value);
        }
    }
    
    
    @Test
    public void testModelSparse() {
        
        CTestSpecification testSpec = new CTestSpecification();
        int sectionId = SectionIds.E_SECTION_TEST_POINTS.swigValue();
        CTestBaseList testPointList = testSpec.getTestBaseList(sectionId,
                                                               false);
        
        /*
         * testPointId | isActive | condCount | condExpr | scriptF | LOCATION                                                                             | LOG            | STEPS                                |
         *             |          |           |          |         | resType | resName | line | isSearch | range | context | matchType | pattern | offset | numSteps | Before | After | expect  | assign  | script p. | next |
         *             |          |           |          |         |         |         |      |          |       |         |           |         |        |          |        |       | 0       |         |           |      |
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         *             |          |    98     |          | scriptF |         |         | 567  |          |       |         |           |         |        |          |        |       | ep < 56 |         |           |      |
         */
        
        CTestPoint tp1 = new CTestPoint();
        tp1.setConditionCount(98);
        tp1.setScriptFunc("scriptFunc");
        
        CTestLocation loc = tp1.getLocation(false);
        loc.setLine(567);
        
        CTestBaseList steps = tp1.getSteps(false);
        CTestEvalAssignStep step = new CTestEvalAssignStep();
        
        CSequenceAdapter seq = step.getExpectedExpressions(false);
        seq.setValue(-1, "ep < 56");
        
        steps.add(-1, step);
        
        testPointList.add(-1, tp1);
        
        ArrayTableModel atm = new ArrayTableModel(ENodeId.TEST_POINT_NODE, false);
        atm.setModelData(testSpec, sectionId);
        
        int numRows = atm.getRowCount();
        int numColumns = atm.getColumnCount();
        
        assertEquals(1, numRows);
        assertEquals(21, numColumns);
        
        String[] expected = {
            "", "", "98", "", "scriptFunc", 
            "", "", "567", "", "", "", "", "", "", "",  // location 
            "", "",                                                               // log 
            "ep < 56", "", "", ""                                   // steps
            }; 
        int rowIndex = 0;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            ArrayTableCell arrayCell = atm.getDataValue(columnIndex, rowIndex);
            if (arrayCell == null) {
                continue;
            }
            String value = arrayCell.getValue();
            if (value == null  &&  expected[columnIndex] == null) {
                continue;
            }
            // System.out.println(columnIndex + "__val = " + value);
            assertEquals(expected[columnIndex], value);
        }
    }

    
    @Test
    public void testModelEmpty() {
        
        CTestSpecification testSpec = new CTestSpecification();
        int sectionId = SectionIds.E_SECTION_TEST_POINTS.swigValue();
        CTestBaseList testPointList = testSpec.getTestBaseList(sectionId,
                                                               false);
        
        /*
         * tpId | isActive | condCount | condExpr | scriptF | LOCATION                                                                                        | LOG            | STEPS                                |
         *      |          |           |          |         | resType | resName | line | isSearch | range | context | matchType | pattern | offset | numSteps | Before | After | expect  | assign  | script p. | next |
         *      |          |           |          |         |         |         |      |          |       |         |           |         |        |          |        |       |         |         |           |      |
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         *      |          |           |          |         |         |         |      |          |       |         |           |         |        |          |        |       |         |         |           |      |
         */
        
        CTestPoint tp1 = new CTestPoint();
        CTestBaseList steps = tp1.getSteps(false);
        CTestEvalAssignStep step = new CTestEvalAssignStep();

        // we have to add at least one step, even if empty, to get items in table.
        steps.add(-1, step);
        testPointList.add(-1, tp1);
        
        ArrayTableModel atm = new ArrayTableModel(ENodeId.TEST_POINT_NODE, false);
        atm.setModelData(testSpec, sectionId);
        
        int numRows = atm.getRowCount();
        int numColumns = atm.getColumnCount();
        
        assertEquals(1, numRows);
        assertEquals(21, numColumns);
        
        String[] expected = {
                "", "", "", "", "", 
                "", "", "", "", "", "", "", "", "", "",  // location 
                "", "",                              // log 
                "", "", "", ""                     // steps
            }; 
        int rowIndex = 0;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            ArrayTableCell arrayCell = atm.getDataValue(columnIndex, rowIndex);
            if (arrayCell == null) {
                continue;
            }
            String value = arrayCell.getValue();
            if (value == null  &&  expected[columnIndex] == null) {
                continue;
            }
            // System.out.println(columnIndex + "__val = " + value);
            assertEquals(expected[columnIndex], value);
        }
        
        HeaderNode header = atm.getHeader();
        System.out.println(header.toTable());
        assertEquals(3, header.getRowCount());
        assertEquals("isActive", header.getChild("isActive").getName());
        assertEquals(5, header.getChild("location").getChildColumnIndex(0));
        assertEquals(5, header.getChild("location").getChildColumnIndex("resourceType"));
        assertEquals(7, header.getChild("location").getChildColumnIndex(2));
        assertEquals(7, header.getChild("location").getChildColumnIndex("line"));
        assertEquals(4, header.getChildIndex("scriptFunc"));
        assertEquals(8, header.getNumChildren());
        assertEquals(21, header.getRequiredNumOfHeaderCells());
        
        assertEquals("tpId", header.getDataValue(0, 0));
        assertEquals("isActive", header.getDataValue(1, 0));
        assertEquals("conditionCount", header.getDataValue(2, 0));
        assertEquals("steps", header.getDataValue(17, 0));

        assertEquals("", header.getDataValue(0, 1));
        assertEquals("", header.getDataValue(2, 1));
        assertEquals("resourceType", header.getDataValue(5, 1));
        assertEquals("line", header.getDataValue(7, 1));
        assertEquals("next", header.getDataValue(20, 2));
    }
    
}
