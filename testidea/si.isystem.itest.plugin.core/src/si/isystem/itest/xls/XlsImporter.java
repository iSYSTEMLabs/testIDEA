package si.isystem.itest.xls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;

public class XlsImporter extends TableImporter {

    private Workbook m_workbook;
    private Sheet m_currentSheet;
    private Row m_currentRow;
    private NPOIFSFileSystem m_hssfFile;
    private OPCPackage m_xssfFile;

    @Override
    protected int createWorkbook(String fileName) throws Exception {
        
        // m_file = new FileInputStream(fileName);

        // pass File instead of FileInputStream as parameter, as memory consumption
        // is much lower in this case (FileInputStream required buffer for the entire file).
        // For example, with File it was possible to read and parse about 39000 test specs,
        // while with FileInputStream only 29000 were read, parsing didn't even start.
        // See: http://poi.apache.org/spreadsheet/quick-guide.html#FileInputStream
        if (fileName.endsWith(".xls")) {
            m_hssfFile = new NPOIFSFileSystem(new File(fileName));
            m_workbook = WorkbookFactory.create(m_hssfFile);
        } else { 
            // make xlsx format the default
            m_xssfFile = OPCPackage.open(new File(fileName));
            m_workbook = WorkbookFactory.create(m_xssfFile);
        }
        
        return m_workbook.getNumberOfSheets();
    }
    
    
    @Override
    protected int getNumAllRows() {
        
        int numSheets = m_workbook.getNumberOfSheets();
        int allRows = 0;
        for (int i = 0; i < numSheets; i++) {
            Sheet sheet = m_workbook.getSheetAt(i);
            allRows += sheet.getLastRowNum() - 1; // headers lines are excluded
        }

        return allRows;
    }
    

    @Override
    protected String setCurrentSheet(int i) {
        m_currentSheet = m_workbook.getSheetAt(i);
        return m_currentSheet.getSheetName();
    }


    @Override
    protected int getNumHeaderRows() {
        
        Comment metaXlsInfoC = m_currentSheet.getCellComment(0, 0);
        if (metaXlsInfoC == null) {
            throw new SIOException("Import from Excel failed, missing meta info in the first cell!\n"
                    + "Only excel files exported with the recent version of testIDEA can be imported.");
        }
        
        RichTextString metaXlsInfoRTS = metaXlsInfoC.getString();
        String metaXlsInfo = metaXlsInfoRTS.getString();
        int nhrIdx = metaXlsInfo.indexOf(NUM_HEADER_ROWS);
        if (nhrIdx < 0) {
            throw new SIOException("Import from Excel failed, invalid meta info in the first cell!\n"
                    + "Only excel files exported with the recent version of testIDEA can be imported.").
                    add("metaInfo", metaXlsInfo);
        }
        
        int endIndex = metaXlsInfo.indexOf('\n', nhrIdx);
        if (endIndex < 0) {
            throw new SIOException("Import from Excel failed, error in meta info in the first cell!\n"
                    + "Only excel files exported with the recent version of testIDEA can be imported.").
                    add("metaInfo", metaXlsInfo);
        }
        
        nhrIdx += NUM_HEADER_ROWS.length();
        int numHeaderRows = 0;
        try {
            numHeaderRows = Integer.valueOf(metaXlsInfo.substring(nhrIdx, endIndex)).intValue();
        } catch (NumberFormatException ex) {
            throw new SIOException("Import from Excel failed, invalid meta info (number of rows) in the first cell!\n"
                    + "Only excel files exported with the recent version of testIDEA can be imported.", ex).
                    add("metaInfo", metaXlsInfo);
        }
        
        return numHeaderRows;
    }
    

    @Override
    protected List<List<String>> getHeaderCells(int numHeaderRows) {
        List<List<String>> headerCells = new ArrayList<>();
        for (int idx = 0; idx < numHeaderRows; idx++) {
            Row xlsRow = m_currentSheet.getRow(idx);
            headerCells.add(row2List(xlsRow));
        }
        return headerCells;
    }


    @Override
    protected int getNumRowsInCurrentSheet() {
        return m_currentSheet.getLastRowNum() + 1;
    }
    

    @Override
    protected int setCurrentRow(int row) {
        m_currentRow = m_currentSheet.getRow(row);
        return m_currentRow.getLastCellNum();
    }

    
    @Override
    protected String getCellValue(int col, StringBuilder outComment) {
        return getCellValue(m_currentRow, col, outComment);
    }


    @Override
    protected void close() throws IOException {
        try {
            if (m_hssfFile != null) {
                m_hssfFile.close();
            }
            if (m_xssfFile != null) {
                m_xssfFile.close();
            }
        } catch (FileNotFoundException ex) {
            // ignore exception, as the file is properly closed, but the exception is thrown
            // anyway (Apache POI 3.9). If close() is missing, then user can no longer
            // save files from Excel until testIDEA is opened.
            // ex.printStackTrace();
        }
    }
    
    
    private String getCellValue(Row xlsRow, int col, StringBuilder outComment) {
        
        Cell cell = xlsRow.getCell(col);
        if (cell == null) { // the cell is not defined
            return null;
        }
        
        int cellType = cell.getCellType();
        
        if (cellType == Cell.CELL_TYPE_FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        if (outComment != null) {
            outComment.delete(0, outComment.length());
            
            Comment cellComment = cell.getCellComment();
            if (cellComment != null) {
                outComment.append(cellComment.getString().getString());
            }
        }
        
        switch (cellType) {
        case Cell.CELL_TYPE_BLANK:
            return null;
            
        case Cell.CELL_TYPE_NUMERIC:
            NumberFormat fmt = DecimalFormat.getNumberInstance(Locale.US);
            fmt.setMaximumFractionDigits(32);
            fmt.setMinimumFractionDigits(0);
            fmt.setGroupingUsed(false);
            String cellString = fmt.format(cell.getNumericCellValue());
            return cellString;
            
        case Cell.CELL_TYPE_STRING:
            cellString = cell.getStringCellValue().trim();
            return cellString;
        case Cell.CELL_TYPE_FORMULA:
        case Cell.CELL_TYPE_BOOLEAN:
        case Cell.CELL_TYPE_ERROR:
            throw new SIllegalArgumentException("Invalid cell type: " + cellType);
        }
        
        throw new SIllegalArgumentException("Unknown cell type: " + cellType);
    }

    
    private List<String> row2List(Row sectionsRow) {
        int numColumns = sectionsRow.getLastCellNum();
        List<String> cells = new ArrayList<String>();
        for (int i = 0; i < numColumns; i++) {
            cells.add(getCellValue(sectionsRow, i, null));
        }
        return cells;
    }
}
