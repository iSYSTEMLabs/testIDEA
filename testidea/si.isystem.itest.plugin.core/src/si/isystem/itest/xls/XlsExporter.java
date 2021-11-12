package si.isystem.itest.xls;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFBorderFormatting;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import si.isystem.connect.CTestSpecification;
import si.isystem.tbltableeditor.HeaderNode;


/**
 * This class exports the test specification to XLS file. The top level test spec.
 * is only a container. The first level test specs are written as main specs,
 * then the second level derived test specs are
 * written too, but only data from some fields. If there are other fields defined,
 * user gets a warning in xls file. If there are no derived test specs, only
 * headers and 3 empty lines are added. Test results can also be written, if they exist.  
 *  
 * @author markok
 */
public class XlsExporter extends TableExporter {

    private Workbook m_workbook;
    private Sheet m_currentSheet;

    private Map<Integer /* section id as int */, Short> m_sectionColors;
    private Row m_currentRow;
    private Cell m_currentCell;

    
    public void exportXLSX(CTestSpecification testSpec, 
                           String fileName,
                           XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        toXSSF(testSpec, fileName, xlsExportLookAndFeel);
    }
    
    
    public void exportXLS(CTestSpecification testSpec, 
                          String fileName,
                          XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        toHSSF(testSpec, fileName, xlsExportLookAndFeel);
    }
    
    
    /** Exports to XLSX or XLS based on filename extension - xlsx or anything else. */
    public void export(CTestSpecification testSpec, 
                       String fileName,
                       XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        if (fileName.toLowerCase().endsWith("xlsx")) {
            toXSSF(testSpec, fileName, xlsExportLookAndFeel);
        } else {
            toHSSF(testSpec, fileName, xlsExportLookAndFeel);
        }
    }
    
    
    public void toHSSF(CTestSpecification testSpec, 
                       String fileName,
                       XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        m_workbook = new HSSFWorkbook();
        toTable(testSpec, fileName, xlsExportLookAndFeel);
    }
    
    
    public void toXSSF(CTestSpecification testSpec, 
                       String fileName,
                       XLSExportLookAndFeel xlsExportLookAndFeel) throws IOException {
        m_workbook = new XSSFWorkbook();
        toTable(testSpec, fileName, xlsExportLookAndFeel);
    }

    
    @Override
    protected void createSheet(String testId, int numHeaderRows) {
        m_currentSheet = m_workbook.createSheet(testId);
    }
    
    
    @Override
    protected void formatSheet(HeaderNode header,
                             int numHeaderRows,
                             int freezeRows,
                             int freezeColumns) {
        
        m_currentSheet.createFreezePane(freezeColumns, freezeRows);  // keep testID column and the first 3 rows

        setCellComment(m_workbook, m_currentSheet, m_currentSheet.getRow(0).getCell(0), 
                       "This comment contains testIDEA internal info - do NOT modify!\n" +
                       TableImporter.NUM_HEADER_ROWS + String.valueOf(header.getRowCount()) + '\n');
        
        autoResizeColumns(m_currentSheet, numHeaderRows);
    }
    
    
    @Override
    protected void writeOutput(String fileName) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            m_workbook.write(fileOut);
        }
    }
    

    @Override
    protected void initColors(HSSFColorTableModel visibilityAndColors, CTestSpecification testSpec) {
        m_sectionColors = new TreeMap<>();
        
        int numRows = visibilityAndColors.getRowCount() - 1; // -1 for header row
        for (int row = 0; row < numRows; row++) {

            String sectionName = visibilityAndColors.getSectionName(row);
            try {
                int id = testSpec.getSectionId(sectionName);
                m_sectionColors.put(id, visibilityAndColors.getColor(row).getIndex());
            } catch (Exception ex) {
                ex.printStackTrace(); // ignore sections in model, which no longer exist in
                                      // test spec
            }
        }
    }

    
    @Override
    protected void createCurrentRow(int row) {
        m_currentRow = m_currentSheet.createRow(row);
    }
    
    
    @Override
    protected void formatHeaderCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                              int sectionId,
                              int col) {
        
        m_currentCell = m_currentRow.createCell(col);
        
        CellStyle style = m_workbook.createCellStyle();

        Font font = m_workbook.createFont();
        // font.setFontHeightInPoints((short)12);
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        // font.setFontName("Courier New");
        // font.setStrikeout(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        style.setFont(font);
        
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setRotation(xlsExportLookAndFeel.getIdentifiersTextAngle());
        
        // color of columns depends on the main section
        setFillColor(style, sectionId, xlsExportLookAndFeel, false);
        m_currentCell.setCellStyle(style);
    }

    
    @Override
    protected void formatBodyCell(XLSExportLookAndFeel xlsExportLookAndFeel,
                                  int sectionId,
                                  boolean isMediumBoldBorder,
                                  int bodyRow,
                                  int col) {
        
        m_currentCell = m_currentRow.createCell(col);
          
        CellStyle style = m_workbook.createCellStyle();

        setFillColor(style, sectionId, xlsExportLookAndFeel, isMediumBoldBorder);
        setRowGroupBorder(xlsExportLookAndFeel, style, bodyRow);
        m_currentCell.setCellStyle(style);
    }
      
      
    @Override
    protected void setCellValue(double dblVal) {
        m_currentCell.setCellValue(dblVal);
    }
    
    
    @Override
    protected void setCellValue(String value) {
        if (value != null) {
            m_currentCell.setCellValue(value);
        }
    }


    @Override
    protected void setCellComment(String commentText) {
        setCellComment(m_workbook, m_currentSheet, m_currentCell, commentText);
    }
    
    
    @Override
    protected void addComboBox(int row, int column, String [] choices) {
        
        DataValidationHelper dvHelper = m_currentSheet.getDataValidationHelper();
        DataValidationConstraint dvConstraint = 
                              dvHelper.createExplicitListConstraint(choices);
        
        CellRangeAddressList addressList = new CellRangeAddressList(row, row, column, column);            
        DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
        // Note the check on the actual type of the DataValidation object.
        // If it is an instance of the XSSFDataValidation class then the
        // boolean value 'false' must be passed to the setSuppressDropDownArrow()
        // method and an explicit call made to the setShowErrorBox() method.
        if(validation instanceof XSSFDataValidation) {
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
        } else {
            // If the Datavalidation contains an instance of the HSSFDataValidation
            // class then 'true' should be passed to the setSuppressDropDownArrow()
            // method and the call to setShowErrorBox() is not necessary.
            validation.setSuppressDropDownArrow(false);
        }
        m_currentSheet.addValidationData(validation);
    }
    
    
    private void autoResizeColumns(Sheet sheet, int numHeaderRows) {
        
        int numcolumns = 0;
        for (int row = 0; row < numHeaderRows; row++) {
            Row xlsRow = sheet.getRow(row);
            numcolumns = Math.max(numcolumns, xlsRow.getLastCellNum()); 
        }
        
        for (int col = 0; col < numcolumns; col++) {
            sheet.autoSizeColumn(col);
        }
    }
    
    
    private void setCellComment(Workbook wb, Sheet sheet, Cell cell, String commentText) {
        
        if (commentText == null  ||  commentText.isEmpty()) {
            return;
        }
        
        CreationHelper factory = wb.getCreationHelper();
        
        Drawing drawing = sheet.createDrawingPatriarch();

        // When the comment box is visible, have it show in a 1x3 space
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 5);

        // Create the comment and set the text+author
        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(commentText);
        comment.setString(str);

        // Assign the comment to the cell
        cell.setCellComment(comment);
    }


    private void setFillColor(CellStyle style, 
                              int sectionId, 
                              XLSExportLookAndFeel xlsExportLookAndFeel,
                              boolean isMediumBorder) {

        if (xlsExportLookAndFeel.isUseColors()) {
            short bkgColor = HSSFColor.WHITE.index; 
            bkgColor = getSectionBkgColor(sectionId);
            
            style.setFillForegroundColor(bkgColor);
            style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            
            style.setBorderRight(HSSFBorderFormatting.BORDER_THIN);
            style.setRightBorderColor(HSSFColor.GREY_25_PERCENT.index);
        }
        
        if (isMediumBorder) {
            style.setBorderBottom(HSSFBorderFormatting.BORDER_MEDIUM);
            style.setBottomBorderColor(HSSFColor.BLACK.index);
        } else {
            style.setBorderBottom(HSSFBorderFormatting.BORDER_THIN);
            style.setBottomBorderColor(HSSFColor.GREY_25_PERCENT.index);
        }
    }

    
    private short getSectionBkgColor(int section) {
        
        /* see
        // http://stackoverflow.com/questions/10912578/apache-poi-xssfcolor-from-hex-code
        // for a very useful description of using colors in HSSF and XSSF
        // Some excerpts:
        // If you are using XSSF:
             XSSFCellStyle style = (XSSFCellStyle)cell.getCellStyle();
             XSSFColor myColor = new XSSFColor(Color.RED);
             style.setFillForegroundColor(myColor);
             
           HSSF uses a color palette:
           
             HSSFWorkbook hwb = new HSSFWorkbook();
             HSSFPalette palette = hwb.getCustomPalette();
             // get the color which most closely matches the color you want to use
             HSSFColor myColor = palette.findSimilarColor(255, 0, 0);
             // get the palette index of that color 
             short palIndex = myColor.getIndex();
             // code to get the style for the cell goes here
             style.setFillForegroundColor(palIndex);

           If you want to use custom colors not already in the default palette, 
           then you have to add them to the palette. The javadoc for HSSFPalette 
           defines the methods you can use for doing so.
        */
        
        Short bkgColor = m_sectionColors.get(section);
        
        if (bkgColor == null) {
            return HSSFColor.WHITE.index; // if color for section is not found, 
                                          // let it be white 
        }
        
        return bkgColor;
    }
    
    
    // sets row border for every N rows, as set by the user 
    private void setRowGroupBorder(XLSExportLookAndFeel xlsExportLookAndFeel,
                                   CellStyle style, int row) {
        
        if (row == 0) {
            return; // do not override border below header
        }
        if (xlsExportLookAndFeel.isUseBottomCellBorder()  &&  
            row % xlsExportLookAndFeel.getCellBorderRowStep() == 0) {
            
            style.setBorderBottom(HSSFBorderFormatting.BORDER_THIN);
            style.setBottomBorderColor(HSSFColor.BLACK.index);
        }
    }
}
