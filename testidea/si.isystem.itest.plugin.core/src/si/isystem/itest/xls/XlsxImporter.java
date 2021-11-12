package si.isystem.itest.xls;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;

/**
 * This class is currently not used. It is intended for reading BIG 
 * XLSX files, because it uses SAX for parsing so memory footprint is low.
 * However, currently (nov. 2013) XSSF event API does not support cell comments, see XSSFReader API:
 *   http://poi.apache.org/apidocs/org/apache/poi/xssf/eventusermodel/XSSFReader.html#getThemesData%28%29
 * 
 * When reading of really large files will be required, open XML files in xlsx
 * file (use 7-zip to unpack xlsx archive). This class also prints events and contents of the file.
 * Implement reading of comments by extracting comments1.xml from xlsx file and
 * parse it with normal SAX parser.
 * 
 * Note: SXSSF API can be used for writing (does not support reading).
 * 
 * Event API doc:
 *   http://poi.apache.org/spreadsheet/how-to.html#xssf_sax_api
 *   http://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/xssf/eventusermodel/XLSX2CSV.java
 * 
 * @author markok
 *
 */
public class XlsxImporter extends TableImporter {

    private OPCPackage m_opcPkg;


    @Override
    protected int createWorkbook(String fileName) throws Exception {
        m_opcPkg = OPCPackage.open(fileName, PackageAccess.READ);
        XSSFReader reader = new XSSFReader(m_opcPkg);
        SharedStringsTable sst = reader.getSharedStringsTable();

        XMLReader parser = fetchSheetParser(sst);

        try {
            Iterator<InputStream> sheets = reader.getSheetsData();
            while(sheets.hasNext()) {
                System.out.println("Processing new sheet:\n");
                try (InputStream sheet = sheets.next()) {
                    InputSource sheetSource = new InputSource(sheet);
                    parser.parse(sheetSource);
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Error on XLSX import!", ex);
        } finally {
            m_opcPkg.close();
        }
        
        return 0;
    }


    private XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        ContentHandler handler = new SheetHandler(sst);
        parser.setContentHandler(handler);
        return parser;
    }


    @Override
    protected int getNumAllRows() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected String setCurrentSheet(int i) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    protected int getNumHeaderRows() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected List<List<String>> getHeaderCells(int numHeaderRows) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    protected int getNumRowsInCurrentSheet() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected int setCurrentRow(int row) {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected String getCellValue(int col, StringBuilder outComment) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    protected void close() throws IOException {
        m_opcPkg.close();
    }

    
    /** 
     * See org.xml.sax.helpers.DefaultHandler javadocs 
     */
    private static class SheetHandler extends DefaultHandler {
        private SharedStringsTable sst;
        private String lastContents;
        private boolean m_isNextString;
        
        private SheetHandler(SharedStringsTable sst) {
            this.sst = sst;
        }
        
        
        @Override
        public void startElement(String uri, String localName, String name,
                                 Attributes attributes) throws SAXException {
            System.out.println("startElement, name: " + name);
            int numAttrs = attributes.getLength();
            for (int idx = 0; idx < numAttrs; idx++) {
                String localAttrName = attributes.getLocalName(idx);
                String qAttrNAme = attributes.getQName(idx);
                String attrType = attributes.getType(idx);
                String attrVal = attributes.getValue(idx);
                
                System.out.printf("    Attrs(lName, qName, type, val): %s, %s, %s, %s\n",
                                  localAttrName, qAttrNAme, attrType, attrVal);
            }
            
            // c => cell
            if(name.equals("c")) {
                // Figure out if the value is an index in the SST
                String cellType = attributes.getValue("t");
                if(cellType != null && cellType.equals("s")) {
                    m_isNextString = true;
                } else {
                    m_isNextString = false;
                }
            }
            // Clear contents cache
            lastContents = "";
        }
        
        
        @Override
        public void endElement(String uri, String localName, String name)
                                                          throws SAXException {
            
            System.out.printf("endElement, name: %s\n", name);
            // Process the last contents as required.
            // Do now, as characters() may be called more than once
            if (m_isNextString) {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                m_isNextString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if(name.equals("v")) {
                System.out.println("endElement, contents: " + lastContents);
            }
        }

        
        @Override
        public void characters(char[] ch, int start, int length)
                                                          throws SAXException {
            lastContents += new String(ch, start, length);
            System.out.println("chars: " + lastContents);
        }
    }
}
