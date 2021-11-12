package si.isystem.itest.xls;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.model.actions.GroupAction;

public class CsvImporter extends TableImporter {

    static final char CSV_TEST_ID_PREFIX = '@';
    static final char CSV_COMMENT_CHAR = '#';
    protected static final int CSV_FILE_SIZE_LIMIT_FOR_UNDO_CLEAR = 1000000;

    private int m_lineNo;
    private int m_numAllLines;
    protected Exception m_exception;
    protected GroupAction m_importAction;
    private BufferedReader m_reader;
    private int m_numHeaderRows;
    List<List<String>> m_bodyCells;
    private List<String> m_currentRow;

    
    @Override
    protected int createWorkbook(String fileName) throws Exception {

        // reads file in advance to count the number of sheet equivalents in
        // CSV file (each sheet starts with "@<testId>", "hr: <numHeaderRows>")
        // Not very efficient, but until there are performance problems, keep it that
        // way. Otherwise add the number of sheets and all lines as the first line in CSV.
        
        Charset charset = Charset.forName("UTF-8");
        m_reader = Files.newBufferedReader(Paths.get(fileName), charset);
        
        m_lineNo = 0;
        m_numAllLines = 0;
        int numWorkbooks = 0;
        String line;
        do {
            line = m_reader.readLine();

            if (line != null) {
            
                m_numAllLines++;
                line = line.trim();
                if (line.length() > 1  &&  line.charAt(0) == '"'  &&  line.charAt(1) == CSV_TEST_ID_PREFIX) {
                    numWorkbooks++;
                }
            }
            
        } while (line != null);
        
        m_reader.close();

        // reopen for content reading
        m_reader = Files.newBufferedReader(Paths.get(fileName), charset);

        return numWorkbooks;
    }


    @Override
    protected int getNumAllRows() {
        return m_numAllLines;
    }


    @Override
    protected String setCurrentSheet(int i) throws IOException {
        
        String line = readNextLine(true, "@<testId>, hr: <numHeaderRows>");
        
        List<String> fields = parseLine(line);
        if (!isSheetStart(fields)) {
            throw new SIOException("Expected line with test ID and number of header rows.").
                      add("line", line);
        }

        String metaXlsInfo = fields.get(1);
        int nhrIdx = metaXlsInfo.indexOf(NUM_HEADER_ROWS);
        if (nhrIdx < 0) {
            throw new SIOException("Import from CSV failed, invalid meta info in the first line!\n"
                    + "Only CSV files exported with the recent version of testIDEA can be imported.").
                    add("field", metaXlsInfo);
        }
        
        nhrIdx += NUM_HEADER_ROWS.length();
        m_numHeaderRows = 0;
        try {
            m_numHeaderRows = Integer.valueOf(metaXlsInfo.substring(nhrIdx)).intValue();
        } catch (NumberFormatException ex) {
            throw new SIOException("Import from CSV failed, invalid meta info (number of rows) in the first line!\n"
                    + "Only CSV files exported with the recent version of testIDEA can be imported.", ex).
                    add("field", metaXlsInfo);
        }
        
        return fields.get(0);
    }


    @Override
    protected int getNumHeaderRows() {
        return m_numHeaderRows;
    }


    @Override
    protected List<List<String>> getHeaderCells(int numHeaderRows) throws IOException {
        
        List<List<String>> headerCells = new ArrayList<>();
        for (int row = 0; row < m_numHeaderRows; row++) {
            String line = readNextLine(true, "Header line " + row + " of " + 
                                       m_numHeaderRows + " expected.");
            headerCells.add(parseLine(line));
        }
        
        return headerCells;
    }


    @Override
    protected int getNumRowsInCurrentSheet() throws IOException {
        
        m_bodyCells = new ArrayList<>();
        String line;
        
        do {
            line = readNextLine(false, "");
            if (line != null) {
                m_bodyCells.add(parseLine(line));
            }
            
        } while (line != null);
        
        return m_bodyCells.size() + m_numHeaderRows;
    }


    @Override
    protected int setCurrentRow(int row) {
        m_currentRow = m_bodyCells.get(row - m_numHeaderRows);
        return m_currentRow.size();
    }


    @Override
    protected String getCellValue(int col, StringBuilder outComment) {
        outComment.delete(0, outComment.length());
        return m_currentRow.get(col);
    }

    
    @Override
    protected void close() throws IOException {
        m_reader.close();
    }
    
    
    private boolean isSheetStart(List<String> strRow) {
        return strRow.size() == 2  &&  
            strRow.get(0).length() > 1 &&  
            strRow.get(0).charAt(0) == CSV_TEST_ID_PREFIX &&
            strRow.get(1).startsWith(TableImporter.NUM_HEADER_ROWS);
            
    }


    private String readNextLine(boolean isCheckForNull,
                                String expectedMsg) throws IOException {
        String line;
        do {
            line = m_reader.readLine();
            m_lineNo++;
            if (isCheckForNull  &&  line == null) {
                throw new SIllegalArgumentException("Unexpected end of file!")
                .add("expected", expectedMsg)
                .add("lineNo", m_lineNo);
            }
        } while (line != null  &&  (line.isEmpty()  ||  line.charAt(0) == CSV_COMMENT_CHAR));
        
        if (line != null) {
            line = line.trim();
        }
        
        return line;
    }

    
    /* private called from unit tests */ List<String> parseLine(String line) {
        boolean inString = false;
        List<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            
            if (line.charAt(i) == '"') {
                inString = !inString;
                sb.append(line.charAt(i));
                continue;
            } 

            if (line.charAt(i) == ','  &&  !inString) {
                // cut away quotes
                String token = sb.substring(1, sb.length() - 1);
                token = token.replace("\"\"", "\"");
                tokens.add(token);
                sb.setLength(0);
                continue;
            }
            
            if (!inString) {
                if (line.charAt(i) == ' '  ||  line.charAt(i) == '\t') {
                    // skip whitespaces out of strings
                    continue;
                } else {
                    throw new SIllegalArgumentException("Invalid character out of string!")
                    .add("lineNo", m_lineNo)
                    .add("invalidChar", line.charAt(i));
                }
            }
            
            sb.append(line.charAt(i));
        }

        // add the token after the last comma 
        if (!inString) {
            // cut away quotes
            if (sb.length() > 1) {
                String token = sb.substring(1, sb.length() - 1);
                token = token.replace("\"\"", "\"");
                tokens.add(token);
            } else if (sb.length() == 1) {
                throw new SIllegalArgumentException("Invalid line format - all tokens must be quoted!")
                .add("lineNo", m_lineNo);
            }
        } else {
            throw new SIllegalArgumentException("Invalid line end - missing quote!")
                .add("lineNo", m_lineNo);
        }
        
        return tokens;
    }
}
