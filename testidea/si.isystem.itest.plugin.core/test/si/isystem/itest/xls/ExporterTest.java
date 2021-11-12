package si.isystem.itest.xls;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.poi.hssf.util.HSSFColor;
import org.junit.BeforeClass;
import org.junit.Test;

import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.IntVector;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.xls.TableImporter.EImportScope;

public class ExporterTest {

    private static TestSpecificationModel m_model;


    public void loadNativeLibrary() {
        String architecture = System.getProperty("osgi.arch");
        
        if (architecture == null) {  // happens during testing
            architecture = "x86"; // default is the most common platform
        }
        
        String libraryName;
        
        if (architecture.equals("x86_64")) {
            libraryName = "lib/IConnectJNIx64";
        } else if (architecture.equals("x86")) {
            libraryName = "lib/IConnectJNI";
        } else {
            throw new IllegalStateException("Unknown 32/64 bit architecture:" + architecture);
        }
        
        System.loadLibrary(libraryName);
    }
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        m_model = TestSpecificationModel.getActiveModel();
    }

    
    @Test
    public void testExport() throws IOException {
        m_model.openTestSpec("test/resources/exportImportTest.iyaml", 0);
        
        CTestSpecification containerTestSpec = m_model.getRootTestSpecification();

        String outFileName = "test/results/testExport.xls";

        XlsExporter exporter = new XlsExporter();
        XLSExportLookAndFeel xlsExportLookAndFeel = new XLSExportLookAndFeel((short)45, true, true, true, true, 5);

        HSSFColorTableModel visibilityAndColors = new HSSFColorTableModel();
        IntVector sections = new IntVector();
        containerTestSpec.getSectionIds(sections);
        for (int idx = 0; idx < sections.size(); idx++) {
            String sectionName = containerTestSpec.getTagName(sections.get(idx));
            boolean isVisible = true;
            HSSFColor color = new HSSFColor.AQUA();
            
            visibilityAndColors.addRow(sectionName, isVisible, color);
        }        

        exporter.export(containerTestSpec, outFileName, xlsExportLookAndFeel);


        outFileName = "test/results/testExport.csv";
        CsvExporter csvExporter = new CsvExporter();
        csvExporter.export(containerTestSpec, outFileName, xlsExportLookAndFeel);
    }    
    
    
    @Test
    public void testImport() throws Exception {
        try {
            m_model.openTestSpec("test/resources/exportImportTest.iyaml", 0);

            CTestSpecification containerTestSpec = m_model.getRootTestSpecification();
            deleteDerivedOfDerivedTestSpecs(containerTestSpec);

            String inFileName = "test/results/testExport.xls";

            XlsImporter xlsImporter = new XlsImporter();
            String[] warnings = xlsImporter.importFromFile(containerTestSpec, 
                                                           inFileName, 
                                                           EImportScope.ECreateNewTestCases);
            assertEquals(0, warnings.length);

            CTestBench testBench = CTestBench.load("test/resources/exportImportTest.iyaml", 0);

            CTestSpecification originalTestSpec = testBench.getTestSpecification(false);
            // save to enable easy comparison with KDiff3
            originalTestSpec.save("test/results/originalTestSpec.iyaml");
            containerTestSpec.save("test/results/importedTestSpec.iyaml");
            assertEquals(true, originalTestSpec.equalsData(containerTestSpec));

            
            containerTestSpec = m_model.getRootTestSpecification();
            deleteDerivedOfDerivedTestSpecs(containerTestSpec);
            inFileName = "test/results/testExport.csv";
            CsvImporter csvImporter = new CsvImporter();
            warnings = csvImporter.importFromFile(containerTestSpec, inFileName, 
                                                  EImportScope.ECreateNewTestCases);
            assertEquals(0, warnings.length);

            // save to enable easy comparison with KDiff3
            containerTestSpec.save("test/results/importedTestSpecCsv.iyaml");
            assertEquals(true, originalTestSpec.equalsData(containerTestSpec));
            
            
        } catch (SException ex) {
            System.err.print(SEFormatter.getInfoWithStackTrace(ex, 5));
            throw ex;
        }
    }


    private void deleteDerivedOfDerivedTestSpecs(CTestSpecification testSpec) {
        int numTestSpecs = testSpec.getNoOfDerivedSpecs();
        for (int i = 0; i < numTestSpecs; i++) {
            CTestSpecification derived = testSpec.getDerivedTestSpec(i);
            derived.deleteAllDerivedTestSpecs();
        }
    }
}
