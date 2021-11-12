package si.isystem.commons.export;

public abstract class ISysExporterAdapter implements ISysExporter {
    private final boolean m_showApproximations;

    public ISysExporterAdapter(boolean showApproximations) {
        m_showApproximations = showApproximations;
    }
    
    public boolean isShowApproximations() {
        return m_showApproximations;
    }
}
