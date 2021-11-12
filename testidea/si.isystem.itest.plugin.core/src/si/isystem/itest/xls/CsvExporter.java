package si.isystem.itest.xls;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import si.isystem.connect.CTestSpecification;
import si.isystem.tbltableeditor.HeaderNode;

/**
 * This class exports test specifications to CSV file in UTF-8 encoding.
 * 
 * The generated CSV file is compliant to http://tools.ietf.org/html/rfc4180#page-2
 * with the following exceptions:
 * 1. If there is more than one test specification exported, the number of
 *    fields may not be the same for all rows in a file. However, it is the same
 *    inside one test specification.
 * 2. Header repeats for each test. Each test specification is preceded by line
 *    containing the test specification ID with '@' used as a prefix, and the 
 *    number of header rows, for example: @test28 hr: 5
 *    
 * @author markok
 *
 */
public class CsvExporter extends TableExporter {
    private BufferedWriter m_writer;
    private boolean m_isRowStart;

    
    public void export(CTestSpecification testSpec, 
                       String fileName,
                       XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        
        Charset charset = Charset.forName("UTF-8");
        m_writer = Files.newBufferedWriter(Paths.get(fileName), charset);
        
        toTable(testSpec, fileName, xlsExportLookAndFeel);
    }
    

    @Override
    protected void createSheet(String testId, int numHeaderRows) throws IOException {

        m_isRowStart = true;
        
        writeCell(CsvImporter.CSV_TEST_ID_PREFIX + testId);
        writeCell(TableImporter.NUM_HEADER_ROWS + String.valueOf(numHeaderRows));
    }


    @Override
    protected void formatSheet(HeaderNode header,
                               int numHeaderRows,
                               int freezeRows,
                               int freezeColumns) throws IOException {
        // no formatting is available in CSV format
    }


    @Override
    protected void writeOutput(String fileName) throws IOException {
        m_writer.write("\n");  // LF following the last record in file
        m_writer.close();
    }


    @Override
    protected void initColors(HSSFColorTableModel visibilityAndColors,
                              CTestSpecification testSpec) {
        // there are no colors in CSV
    }


    @Override
    protected void formatHeaderCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                                    int sectionId,
                                    int col) {
        // there are no text styles (fonts, colors, ...) in CSV
    }


    @Override
    protected void formatBodyCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                                  int sectionId,
                                  boolean isMediumBoldBorder,
                                  int bodyRow,
                                  int col) {
        // there are no text styles (fonts, colors, ...) in CSV
    }


    @Override
    protected void createCurrentRow(int row) throws IOException {
        m_writer.write("\n");
        m_isRowStart = true;
    }


    @Override
    protected void setCellValue(double dblVal) throws IOException {
        NumberFormat fmt = DecimalFormat.getNumberInstance(Locale.US);
        fmt.setMaximumFractionDigits(32);
        fmt.setMinimumFractionDigits(0);
        fmt.setGroupingUsed(false);
        writeCell(fmt.format(dblVal));
    }


    @Override
    protected void setCellValue(String value) throws IOException {
        if (value != null) {
            writeCell(value);
        } else {
            writeCell("");
        }
    }


    @Override
    protected void setCellComment(String commentText) {
        // there are no comments in CSV
    }


    @Override
    protected void addComboBox(int row, int column, String[] choices) {
        // there are no combos in CSV
    }

    
    /**
     * Escapes double quotes and adds double quotes.
     * @param writer
     * @param contents
     * @throws IOException
     */
    private void writeCell(String contents) throws IOException {
        StringBuilder sb = new StringBuilder("\"");
        
        if (!m_isRowStart) {
            sb.insert(0, ",");
        } else {
            m_isRowStart = false;
        }
        
        for (int i = 0; i < contents.length(); i++) {
            char ch = contents.charAt(i);
            if (ch != '"') {
                sb.append(ch);
            } else {
                sb.append("\"\"");
            }
        }
        sb.append('"');
        
        m_writer.write(sb.toString());
    }
}
