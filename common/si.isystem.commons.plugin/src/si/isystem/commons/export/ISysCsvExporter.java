package si.isystem.commons.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import si.isystem.commons.ui.MessageDialogSyncExec;
import si.isystem.exceptions.SException;

public class ISysCsvExporter extends ISysExporterAdapter {

	private File m_file;
	private FileWriter m_fileWriter;

	public ISysCsvExporter(File file, String[] headers, boolean showApproximations) throws SException {
	    super(showApproximations);
	    
		m_file = file;
		try {
			m_fileWriter = new FileWriter(file, false);
		} catch (IOException ioeOpen) {
			String msg = String.format("Failed to open file writer for file '%s'",
					file.getAbsolutePath());
			MessageDialogSyncExec.showError(
					"Export error", 
					msg);
			throw new SException(msg, ioeOpen);
		}
		
		flushHeader(headers);
	}

    @Override
    public void flushHeader(String[] names) throws SException {
        StringBuilder sb = new StringBuilder(names[0]);
        for (int i = 1; i < names.length; i++) {
            sb.append(',').append(names[i]);
        }
        sb.append('\n');
        
        flushLine(sb.toString());
    }
    
    @Override
    public void flushLine(String[] values, boolean[] validity) throws SException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            if (validity[i]  ||  isShowApproximations()) {
                sb.append(values[i]);
            }
        }
        sb.append('\n');
        
        flushLine(sb.toString());
    }

	protected void flushLine(String line) {
		try {
			m_fileWriter.write(line);
		} catch (IOException ioeWrite) {
			String msg = String.format("Export error: Failed to write to file '%s'",
					m_file.getAbsolutePath());
			throw new SException(msg, ioeWrite);
		}
	}

	@Override
	public void endExport() throws SException {
		try {
			if (m_fileWriter != null) {
				m_fileWriter.flush();
				m_fileWriter.close();
			}
		} catch (IOException e) {
			String msg = String.format("Export error: Failed to close file '%s'",
					m_file.getAbsolutePath());
			throw new SException(msg, e);
		}
	}
    
//  public static void main(String[] args) {
//        File f = new File("c:\\_tmp\\test1.txt");
//        
//        ISysCsvExporter e = new ISysCsvExporter(f, new String[] {"012345678901234567890", "abc", "aaabbbcccdddeeefff"}, false);
//        e.flushLine(new double[] {123456789.0, 2, 3}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 0.000002, 0.00000000000000000003}, new boolean[] {true, false, true});
//        e.flushLine(new double[] {1, 2, 3}, new boolean[] {true, false, true});
//    }
}
