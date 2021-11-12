package si.isystem.itest.ui.spec.data;

import si.isystem.itest.model.YamlScalar;

/**
 * This class is used to store comments for tables. One instance stores comments
 * for one row. 
 */
public class TableRowComment {
    private String m_declNlComment;  // comment for left table column, declarations in var section
    private String m_declEolComment; // comment for left table column, declarations in var section
    private String m_initNlComment;  // comment for right table column, init in var section
    private String m_initEolComment; // comment for right table column, init in var section
    private String m_mappingKey; // used in tables with two or more columns, which store
    // mapping information, but also in expressions table, where it is used to store
    // the diff info of test results.
    private Object m_data;

    public TableRowComment()
    {}
    
    
    public TableRowComment(String declNlComment,
                           String declEolComment,
                           String initNlComment,
                           String initEolComment) {
        m_declNlComment = declNlComment;
        m_declEolComment = declEolComment;
        m_initNlComment = initNlComment;
        m_initEolComment = initEolComment;
        m_mappingKey = "";
    }
    
    
    /**
     * Assigns all object attributes - only references are assigned, which is not
     * a problem for Strings, but for data object it may be!
     * @param src
     */
    public void assign(TableRowComment src) {
        if (src == null) {
            System.err.println("src == null");
            new Throwable().printStackTrace();
            return;
        }
        m_declNlComment = src.m_declNlComment; 
        m_declEolComment = src.m_declEolComment;
        m_initNlComment = src.m_initNlComment;
        m_initEolComment = src.m_initEolComment;
        m_mappingKey = src.m_mappingKey;
        m_data = src.m_data;
    }
    
    /** Sets comment for the leftmost column - used for tables with lists and mappings. */ 
    public void setLeftComment(String newLineComment, String endOfLineComment) {
        m_declNlComment = newLineComment;
        m_declEolComment = endOfLineComment;
    }

    /** Sets comment for values column in vars section. */ 
    public void setRightComment(String newLineComment, String endOfLineComment) {
        m_initNlComment = newLineComment;
        m_initEolComment = endOfLineComment;
    }

    public String getLeftNlComment() {
        return m_declNlComment;
    }
    
    public String getLeftEolComment() {
        return m_declEolComment;
    }
    
    public String getRightNlComment() {
        return m_initNlComment;
    }
    
    public String getRightEolComment() {
        return m_initEolComment;
    }
 
    public boolean isLeftCommentSet() {
        return !m_declNlComment.isEmpty()  ||  !m_declEolComment.isEmpty();
    }
    
    public boolean isRightCommentSet() {
        return !m_initNlComment.isEmpty()  ||  !m_initEolComment.isEmpty();
    }

    
    @Override
    public int hashCode() {  // see Eff. Java by JB
        int hash = 17;
        
        hash = 31 * hash + m_declNlComment.hashCode();
        hash = 31 * hash + m_declEolComment.hashCode();
        hash = 31 * hash + m_initNlComment.hashCode();
        hash = 31 * hash + m_initEolComment.hashCode();
        hash = 31 * hash + m_mappingKey.hashCode();
        
        return hash;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
 
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof TableRowComment)) {
            return false;
        }
        TableRowComment other = (TableRowComment)o;
        
        return m_declNlComment.equals(other.m_declNlComment)  &&
               m_declEolComment.equals(other.m_declEolComment)  &&
               m_initNlComment.equals(other.m_initNlComment)  &&
               m_initEolComment.equals(other.m_initEolComment)  &&
               m_mappingKey.equals(other.m_mappingKey);
    }

    /** 
     * Sets value, which may be used when assigning comments to items in 
     * mapping, for example assignments in stubs are stored in a mapping with
     * variable names as keys.
     * @param key
     */
    public void setMappingKey(String key) {
        m_mappingKey = key;
    }
    
    /** 
     * Returns value, which may be used when assigning comments to items in 
     * mapping, for example assignments in stubs are stored in a mapping with
     * variable names as keys.
     */
    public String getMappingKey() {
        return m_mappingKey;
    }

    /** Returns data object set by application. This object is not used for 
     * comparison in equals() method(). */ 
    public Object getData() {
        return m_data;
    }

    /** Returns data object to be used by application. This object is not used for 
     * comparison in equals() method(). */ 
    public void setData(Object data) {
        m_data = data;
    }
    
    /**
     * Returns human readable string representation of this object.
     */
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        String declComment = YamlScalar.getToolTipText(m_declNlComment, m_declEolComment);
        sb.append(declComment);
        
        String initComment = YamlScalar.getToolTipText(m_initNlComment, m_initNlComment);
        if (!initComment.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n===================\n"); 
            }
            sb.append(initComment);
        }
        
        return sb.toString();
    }
}