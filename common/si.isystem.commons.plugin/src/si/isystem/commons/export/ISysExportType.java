package si.isystem.commons.export;

import java.util.HashMap;
import java.util.Map;

public enum ISysExportType {
	Txt ("*.txt",  "Text file"),
	Csv ("*.csv",  "Comma separated values"),
	Xlsx("*.xlsx", "Microsoft Excel");
	
	private static final Map<String, ISysExportType> s_byExtension = new HashMap<>();
	private static final String[] s_allExtensions;
	private static final String[] s_allNames;
	
	static {
		ISysExportType[] vs = values();
		s_allExtensions = new String[vs.length];
		s_allNames = new String[vs.length];
		for (int ti = 0; ti < vs.length; ti++) {
			ISysExportType t = vs[ti];
			s_byExtension.put(t.getExtension(), t);
			s_allExtensions[ti] = t.getExtension();
			s_allNames[ti] = String.format("%s (%s)", t.getDescription(), t.getExtension());
		}
	}
	
	private final String m_extension;
	private final String m_description;

	private ISysExportType(String extension, String description) {
		m_extension = extension;
		m_description = description;
	}

	public String getExtension() {
		return m_extension;
	}

	public String getDescription() {
		return m_description;
	}
	
	public static String[] getExtensions() {
		return s_allExtensions;
	}
	
	public static String[] getFullNames() {
		return s_allNames;
	}
	
	public static ISysExportType getByExtension(String extension) {
		return s_byExtension.get(extension);
	}
	
	public static ISysExportType getByFileName(String fileName) {
		for (ISysExportType t : values()) {
			String ext = t.m_extension.substring(1);
			if (fileName.endsWith(ext)) {
				return t;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
	    return String.format("(%s) %s", m_extension, m_description);
	}
}
