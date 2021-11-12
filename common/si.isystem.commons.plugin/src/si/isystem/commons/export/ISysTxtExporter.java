package si.isystem.commons.export;

import java.io.File;

import si.isystem.exceptions.SException;

public class ISysTxtExporter extends ISysCsvExporter {
    private int spacePerValue = 0;
    private String m_headerFormat, m_valueFormatString;
    
	public ISysTxtExporter(File file, String[] headers, boolean showApproximations) throws SException {
		super(file, headers, showApproximations);
	}

    @Override
    public void flushHeader(String[] names) throws SException {
        // Make enough space per variable so that the names are alligned with data (or 14 long)
        int max = 0;
        for (String n : names) {
            max = Math.max(max, n.length());
        }
        spacePerValue = Math.max(max, 14);
        
        m_headerFormat = String.format(" %%%ds", spacePerValue);
        m_valueFormatString = String.format(" %%%ds", spacePerValue);
        
        // Print the header names
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            final String h = String.format(m_headerFormat, names[i]);
            sb.append(h);
        }
        sb.append('\n');
        
        flushLine(sb.toString());
    }

	@Override
	public void flushLine(String[] values, boolean[] validity) throws SException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
		    String str;
		    if (validity[i]  ||  isShowApproximations()) {
	            str = String.format(m_valueFormatString, values[i]);
		    }
		    else {
		        str = String.format(m_valueFormatString, "");
		    }
		    sb.append(str);
		}
		sb.append('\n');

		flushLine(sb.toString());
	}
	
//	public static void main(String[] args) {
//        File f = new File("c:\\_tmp\\test1.txt");
//        
//        ISysTxtExporter e = new ISysTxtExporter(f, new String[] {"012345678901234567890", "abc", "aaabbbcccdddeeefff"}, false);
//        e.flushLine(new double[] {123456789.0, 2, 3}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 0.000002, 0.0000000000000000000000003}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 0.000002, 0.00000000000000000003}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 0.000002, 0.000000000000003}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 2, 3}, new boolean[] {true, false, true});
//    }
}
