package si.isystem.commons.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import si.isystem.exceptions.SException;

public class ISysXlsxExporter extends ISysExporterAdapter {
	private String m_sheetName;
    private XSSFWorkbook m_workbook;
    private XSSFSheet m_sheet;
    private FileOutputStream m_fileOut;
    private int m_rowCount = 0;
    private XSSFCellStyle m_styleGray;

    public ISysXlsxExporter(File excelFileName, String[] headers, boolean showApproximations) throws SException {
        super(showApproximations);
        m_sheetName = "Analyzer export";
        m_workbook = new XSSFWorkbook();
        
        m_styleGray = m_workbook.createCellStyle();
        XSSFFont font = m_workbook.createFont();
        font.setColor(HSSFColor.GREY_40_PERCENT.index);
        m_styleGray.setFont(font);
        
        m_sheet = m_workbook.createSheet(m_sheetName) ;

        try {
            m_fileOut = new FileOutputStream(excelFileName);
            flushHeader(headers);
        }
        catch (IOException ioe) {
            throw new SException(ioe);
        }
	}
	
	@Override
    public void flushHeader(String[] headers) throws SException {
        XSSFRow row = m_sheet.createRow(m_rowCount++);
        for (int ci = 0; ci < headers.length; ci++) {
            XSSFCell cell = row.createCell(ci);
            cell.setCellValue(headers[ci]);
        }
    }

    @Override
    public void flushLine(String[] values, boolean[] validity) throws SException {
        XSSFRow row = m_sheet.createRow(m_rowCount++);
        for (int ci = 0; ci < values.length; ci++) {
            XSSFCell cell = row.createCell(ci);
            if (validity[ci]) {
                cell.setCellValue(values[ci]);
            }
            else {
                if (isShowApproximations()) {
                    cell.setCellValue(values[ci]);
                    cell.setCellStyle(m_styleGray);
                }
                else {
                    cell.setCellValue(Cell.CELL_TYPE_BLANK);
                    cell.setCellValue("");
                }
            }
        }
    }

	@Override
	public void endExport() {
	    try {
	        m_workbook.write(m_fileOut);
            m_fileOut.flush();
            m_fileOut.close();	    
        }
        catch (IOException ioe) {
            throw new SException(ioe);
        }
	}
}
