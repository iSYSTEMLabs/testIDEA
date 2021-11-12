package si.isystem.tbltableeditor;


/**
 * This interface is used to provide results information to KTable. It should be 
 * implemented by table client and passed at table construction. 
 * 
 * @author markok
 */
public interface IResultProvider {

    enum EResultStatus {OK, ERROR, NO_RESULT};
    /**
     * 
     * @param col cell column in application model coordinates (without header)
     * @param row cell row in application model coordinates (without header)
     * @param sb on input is contains cell text value (for example expected expression),
     *           on output it should contain result description (for example values of subexpressions) 
     * @return results status
     */
    EResultStatus getCellResult(int dataCol, int dataRow, StringBuilder sb);
    
    /**
     * 
     * @param col cell column in application model coordinates (without header)
     * @param sb on output it should contain result description (for example values of subexpressions)
     * @return results status
     */
    EResultStatus getColumnResult(int dataCol, StringBuilder sb);

    /**
     * 
     * @param row cell row in application model coordinates (without header)
     * @param sb on output it should contain result description (for example values of subexpressions)
     * @return results status
     */
    EResultStatus getRowResult(int dataRow, StringBuilder sb);

    /**
     * 
     * @param sb on output it should contain result description (for example values of subexpressions)
     * @return results status
     */
    EResultStatus getTableResult(StringBuilder sb);
}
