package si.isystem.commons.export;


public interface ISysExporter {
    public void flushHeader(String[] values);
    public void flushLine(String[] values, boolean[] validity);
	public void endExport();
}
