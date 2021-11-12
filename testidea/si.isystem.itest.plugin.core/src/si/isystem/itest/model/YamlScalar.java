package si.isystem.itest.model;

import java.util.List;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.DataUtils;

/**
 * This is immutable class containing information about one YAML scalar. 
 * 
 * @author markok
 */
public class YamlScalar {

    public static final String NL_EOL_COMMENT_SEPARATOR = "\n------------------------------\n";
    
    private final int m_sectionId;  // this is enum value from CTest... classes. It is int
    // to fit all enum types - it is users responsibility to provide the right
    // value for the given class
    private String m_key;  // used in sections like locals, init and stub step 
                           // assignments, where mapping keys are user defined (var names)
    private int m_index;   // used in sequence sections like 'expected'
    private String m_value;
    private String m_newLineComment;
    private String m_endOfLineComment;
    private TestSpecAccessor m_testSpecAccessor;

    // public final static int SECTION_NOT_SET = -1;
    
    private volatile int m_hashCode;

    public enum EQuotingType {
        QUOTE_IF_REQUIRED, QUOTE_ALWAYS, QUOTE_YAML_LIST, QUOTE_NEVER
    }

    /** Static factory methods should be used to get instance of this class. */ 
    protected YamlScalar(int sectionId) {
    
        m_sectionId = sectionId;
        m_value = "";
        m_newLineComment = "";
        m_endOfLineComment = "";
    }

    
    /** Ctor for user mappings. */
    protected YamlScalar(int sectionId, String key) {
        
        m_sectionId = sectionId;
        m_key = key;
        m_value = "";
        m_newLineComment = "";
        m_endOfLineComment = "";
    }

    
    /** Ctor for sequences. */
    protected YamlScalar(int sectionId, int index) {
        
        m_sectionId = sectionId;
        m_index = index;
        m_value = "";
        m_newLineComment = "";
        m_endOfLineComment = "";
    }

    
    /** @see KeyEditor  */
    public static YamlScalar newKey(int sectionId) {
        YamlScalar scalar = new YamlScalar(sectionId);
        scalar.m_testSpecAccessor = scalar.new KeyEditor();
        return scalar;
    }


    /** @see ListEditor */
    public static YamlScalar newList(int sectionId) {
        YamlScalar scalar = new YamlScalar(sectionId);
        scalar.m_testSpecAccessor = scalar.new ListEditor();
        return scalar;
    }


    /** @see ValueEditor  */
    public static YamlScalar newValue(int sectionId) {
        YamlScalar scalar = new YamlScalar(sectionId);
        scalar.m_testSpecAccessor = scalar.new ValueEditor();
        return scalar;
    }

    /** @see MixedEditor  */
    public static YamlScalar newMixed(int sectionId) {
        YamlScalar scalar = new YamlScalar(sectionId);
        scalar.m_testSpecAccessor = scalar.new MixedEditor();
        return scalar;
    }
    
    /** @see UserMappingKeyValueEditor */
    public static YamlScalar newUserMapping(int sectionId, String key) {
        YamlScalar scalar = new YamlScalar(sectionId, key);
        scalar.m_testSpecAccessor = scalar.new UserMappingKeyValueEditor();
        return scalar;
    }
    
    /** @see UserMappingIndexValueEditor */
    public static YamlScalar newUserIndexMapping(int sectionId, int idx) {
        YamlScalar scalar = new YamlScalar(sectionId, idx);
        scalar.m_testSpecAccessor = scalar.new UserMappingIndexValueEditor();
        return scalar;
    }
    
    /** @see ListElementEditor */
    public static YamlScalar newListElement(int sectionId, int index) {
        YamlScalar scalar = new YamlScalar(sectionId, index);
        scalar.m_testSpecAccessor = scalar.new ListElementEditor();
        return scalar;
    }

    
    public static YamlScalar newListElement(int sectionId, int index, boolean isAllowIndexOutOfRangeOnRead) {
        YamlScalar scalar = new YamlScalar(sectionId, index);
        scalar.m_testSpecAccessor = scalar.new ListElementEditor(isAllowIndexOutOfRangeOnRead);
        return scalar;
    }
    
    
    
    public YamlScalar copy() {
        YamlScalar scalar = new YamlScalar(m_sectionId);
        scalar.m_value = m_value;
        scalar.m_key = m_key;
        scalar.m_index = m_index;
        scalar.m_newLineComment = m_newLineComment;
        scalar.m_endOfLineComment = m_endOfLineComment;
        
        if (m_testSpecAccessor instanceof KeyEditor) {
            scalar.m_testSpecAccessor = scalar.new KeyEditor();
            
        } else if (m_testSpecAccessor instanceof ListEditor) {
            scalar.m_testSpecAccessor = scalar.new ListEditor();
            
        } else if (m_testSpecAccessor instanceof ListElementEditor) {
            scalar.m_testSpecAccessor = scalar.new ListElementEditor();
            
        } else if (m_testSpecAccessor instanceof ValueEditor) {
            scalar.m_testSpecAccessor = scalar.new ValueEditor();
            
        } else if (m_testSpecAccessor instanceof MixedEditor) {
            scalar.m_testSpecAccessor = scalar.new MixedEditor();
            
        } else if (m_testSpecAccessor instanceof UserMappingKeyValueEditor) {
            scalar.m_testSpecAccessor = scalar.new UserMappingKeyValueEditor();
            
        } else if (m_testSpecAccessor instanceof UserMappingIndexValueEditor) {
            scalar.m_testSpecAccessor = scalar.new UserMappingIndexValueEditor();
            
        } else {
            throw new SIllegalArgumentException("Invalid type of accessor!")
                      .add("acessorType", m_testSpecAccessor.getClass().getName());
        }
        
        return scalar;
    }


    /* public void dataFromTestSpec(CTestBase testBase, EQuotingType quotingType) {
        m_testSpecAccessor.dataFromTestSpec(testBase);
        m_value = m_testSpecAccessor.quote(m_value, quotingType);
    } */
    
    
    public void dataFromTestSpec(CTestBase testBase) {
        m_testSpecAccessor.dataFromTestSpec(testBase);
    }
    
    
    public void dataToTestSpec(CTestBase testBase) {
        if (testBase != null) {
            m_testSpecAccessor.dataToTestSpec(testBase);
        }
    }


    public void commentToTestSpec(CTestBase testBase) {
        if (testBase != null) {
            m_testSpecAccessor.commentToTestSpec(testBase);
        }
    }


    public String getValue() {
        return m_value;
    }

    
    public String getTagValueFromTestSpec(CTestBase testBase) {
        return testBase.getTagValue(m_sectionId);
    }

    
    public String getNewLineComment() {
        return m_newLineComment;
    }


    public String getEndOfLineComment() {
        return m_endOfLineComment;
    }


    public int getSectionId() {
        return m_sectionId;
    }

    
    /** Returns index for sequence elements. */
    public int getIndex() {
        return m_index;
    }


    /** Returns key for mapping elements. */
    public String getKey() {
        return m_key;
    }
    
    
/*    private static String quoteValue(String value, EQuotingType quotingType) {
        
        if (value == null) {
            return null;
        }
        
        switch (quotingType) {
        case QUOTE_ALWAYS:
            value = StringValidator.scalar2YAML(value);
            break;
        case QUOTE_IF_REQUIRED:
            value = StringValidator.quoteIfRequired(value);
            break;
        case QUOTE_YAML_LIST:
            value = StringValidator.quoteYAMLList(value);
            break;
        case QUOTE_NEVER:
            break;
        default:
            throw new SIllegalArgumentException("Invalid quoting type!")
                                           .add("quotingType", quotingType)
                                           .add("value", value);
        }
        
        return value;
    }
*/
    
    public void setValue(String value) {
        m_value = value;
    }

    
    /** Sets index for scalars in list. */
    public void setIndex(int index) {
        m_index = index;
    }


    /** Sets key for scalar values in mapping. */
    public void setKey(String key) {
        m_key = key;
    }


    /**
     * The string must end with '\n', because the last char is cut away on write,
     * see emitter.c, yaml_emitter_comment_write().
     * 
     * @param newLineComment
     */
    public void setNewLineComment(String newLineComment) {
        m_newLineComment = newLineComment;
    }

    
    /**
     * The string must end with '\n', because the last char is cut away on write,
     * see emitter.c, yaml_emitter_comment_write().
     * 
     * @param newLineComment
     */
    public void setEndOfLineComment(String endOfLineComment) {
        m_endOfLineComment = endOfLineComment;
    }


    public boolean hasComment() {
        return !m_newLineComment.isEmpty()  ||  !m_endOfLineComment.isEmpty();
    }
    
    
    // returns text, which can be used in tooltips
    public String getToolTipText() {
        return getToolTipText(m_newLineComment, m_endOfLineComment);
    }
    
    
    // returns text, which can be used in tooltips
    public static String getToolTipText(String newLineComment, String endOfLineComment) {
        newLineComment = newLineComment.trim();
        endOfLineComment = endOfLineComment.trim();
        
        if (!newLineComment.isEmpty()  ||  !endOfLineComment.isEmpty()) {
            return stripCommentChars(newLineComment) + 
                   NL_EOL_COMMENT_SEPARATOR + 
                   stripCommentChars(endOfLineComment);
        }
        
        return "";
    }
    
    
    public static int getCommentIndent(String comment) {
        
        if (comment.trim().isEmpty()) {
            return 0;
        }
        
        int indent = comment.indexOf('#');
        
        if (indent < 0) {
            throw new SIllegalArgumentException("Invalid comment - missing '#' char!")
                                                .add("comment", comment);
        }
        
        return indent;
    }
    
    
    public static String stripCommentChars(String comment) {
        String [] lines = comment.split("\n");
        StringBuilder sb = new StringBuilder();
        
        for (String line : lines) {
            int indent = getCommentIndent(line);
            if (!line.trim().isEmpty()) {
                line = line.substring(indent + 1).trim();
            }
            sb.append(line).append('\n');
        }
        
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj == this) {
            return true;
        }
        
        if (!(obj instanceof YamlScalar)) {
            return false;
        }
        
        YamlScalar other = (YamlScalar)obj;
        
        boolean valueEq = m_value == null ? other.m_value == null : m_value.equals(other.m_value); 
        boolean nlEq = m_newLineComment == null ? other.m_newLineComment == null : m_newLineComment.equals(other.m_newLineComment); 
        boolean eolEq = m_endOfLineComment == null ? other.m_endOfLineComment == null : m_endOfLineComment.equals(other.m_endOfLineComment); 
        
        return m_sectionId == other.m_sectionId  &&  valueEq &&  nlEq &&  eolEq;
    }
    
    @Override
    public int hashCode() {
        if (m_hashCode == 0) {  // this is an immutable class
            
            int valueHash = m_value == null ? 0 : m_value.hashCode();
            int nlHash = m_newLineComment == null ? 0 : m_newLineComment.hashCode();
            int eolHash = m_endOfLineComment == null ? 0 : m_endOfLineComment.hashCode();
            
            m_hashCode = 17;
            m_hashCode = 31 * m_hashCode + m_sectionId;
            m_hashCode = 31 * m_hashCode + valueHash;
            m_hashCode = 31 * m_hashCode + nlHash;
            m_hashCode = 31 * m_hashCode + eolHash;
        }
        return m_hashCode;
    }

    
    @Override
    public String toString() {
        
        return "YamlScalar:\n" +
               "  value: " + m_value + "\n" +
               "  nlComment: " + m_newLineComment + '\n' +
               "  eolComment: " + m_endOfLineComment + "\n" +
               "  sectionId: " + m_sectionId + '\n' +
               "  key: " + m_key + "\n" +
               "  index: " + m_index + "\n";
    }

    
    
    
    interface TestSpecAccessor {
        
        void dataFromTestSpec(CTestBase testBase);
        void dataToTestSpec(CTestBase testBase);
        void commentToTestSpec(CTestBase testBase);
        // String quote(String value, EQuotingType quotingType);
    };
    
    
    /** 
     * Accessor for tags, where value is not scalar, for example test spec. func and 
     * stubs. Both comments go to tag, list of mappings continues
     * on the next line. This accessor does not set/get value.
     */
    public class KeyEditor implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {

            m_newLineComment = testBase.getComment(m_sectionId, 
                                                   SpecDataType.KEY,
                                                   CommentType.NEW_LINE_COMMENT);

            m_endOfLineComment = testBase.getComment(m_sectionId, 
                                                     SpecDataType.KEY, 
                                                     CommentType.END_OF_LINE_COMMENT);
        }


        @Override
        public void dataToTestSpec(CTestBase testBase) {
            commentToTestSpec(testBase);
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId,
                                SpecDataType.KEY, CommentType.NEW_LINE_COMMENT, 
                                m_newLineComment);

            testBase.setComment(m_sectionId, 
                                SpecDataType.KEY, CommentType.END_OF_LINE_COMMENT, 
                                m_endOfLineComment);
        }
        
        
        /* @Override
        public String quote(String value, EQuotingType quotingType) {
            return quoteValue(value, quotingType);
        } */
    }
    
    
    /** 
     * Accessor for values in list, for example function name and return value. 
     */
    public class ValueEditor implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {
            m_value = testBase.getTagValue(m_sectionId); 
            
            m_newLineComment = testBase.getComment(m_sectionId, 
                                                          SpecDataType.VALUE,
                                                          CommentType.NEW_LINE_COMMENT);

            m_endOfLineComment = testBase.getComment(m_sectionId, 
                                                     SpecDataType.VALUE, 
                                                     CommentType.END_OF_LINE_COMMENT);
        }
        
        
        @Override
        public void dataToTestSpec(CTestBase testBase) {
            
            testBase.setTagValue(m_sectionId, m_value);

            commentToTestSpec(testBase);
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId,
                                SpecDataType.VALUE, CommentType.NEW_LINE_COMMENT, 
                                m_newLineComment);

            testBase.setComment(m_sectionId, 
                                SpecDataType.VALUE, CommentType.END_OF_LINE_COMMENT, 
                                m_endOfLineComment);
        }
    }

    
    /** 
     * Accessor for tags in form of mapping pair: 
     *     tag: value
     *  where new line comment goes to tag, end of line comment to value.
     *   
     * @author markok
     */
    public class MixedEditor implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {
            m_value = testBase.getTagValue(m_sectionId);
            
            m_newLineComment = testBase.getComment(m_sectionId, 
                                                          SpecDataType.KEY,
                                                          CommentType.NEW_LINE_COMMENT);

            m_endOfLineComment = testBase.getComment(m_sectionId, 
                                                     SpecDataType.VALUE, 
                                                     CommentType.END_OF_LINE_COMMENT);
        }
        
        
        @Override
        public void dataToTestSpec(CTestBase testBase) {
            
            testBase.setTagValue(m_sectionId, m_value);
        
            commentToTestSpec(testBase);
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId,
                                SpecDataType.KEY, CommentType.NEW_LINE_COMMENT, 
                                m_newLineComment);

            testBase.setComment(m_sectionId, 
                                SpecDataType.VALUE, CommentType.END_OF_LINE_COMMENT, 
                                m_endOfLineComment);
        }
        

        /* @Override
        public String quote(String value, EQuotingType quotingType) {
            return quoteValue(value, quotingType);
        } */
    }
    
    
    /** 
     * Editor for complete sequence as YAML string.
     * Accessor for tags, where value is list, for example test spec. tags and 
     * expected expressions. Both comments go to tag, list contents goes as 
     * string to m_value.
     */
    public class ListEditor implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {

            // m_value = testBase.getTagValue(m_sectionId);

            CSequenceAdapter seq = new CSequenceAdapter(testBase, m_sectionId, true);
            m_value = DataUtils.seqToString(seq);
            
            m_newLineComment = testBase.getComment(m_sectionId, 
                                                   SpecDataType.KEY,
                                                   CommentType.NEW_LINE_COMMENT);

            m_endOfLineComment = testBase.getComment(m_sectionId, 
                                                     SpecDataType.KEY, 
                                                     CommentType.END_OF_LINE_COMMENT);
        }


        @Override
        public void dataToTestSpec(CTestBase testBase) {

            // testBase.setTagValue(m_sectionId, '[' + m_value + ']');
            List<String> listItems = DataUtils.splitToList(m_value);
            CSequenceAdapter seq = new CSequenceAdapter(testBase, m_sectionId, false);
            seq.resize(0);
            for (String item : listItems) {
                seq.add(-1, item);
            }
            
            commentToTestSpec(testBase);
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId,
                                SpecDataType.KEY, CommentType.NEW_LINE_COMMENT, 
                                m_newLineComment);

            testBase.setComment(m_sectionId, 
                                SpecDataType.KEY, CommentType.END_OF_LINE_COMMENT, 
                                m_endOfLineComment);
        }
        
        
       /* @Override
        public String quote(String value, EQuotingType quotingType) {
            // list values should not be quoted, because then they are treated 
            // as a single scalar, which is wrong!
            return value;
        } */
    }
    
    
    /** 
     * Editor for single sequence element.
     * Accessor for tags, where value is list, for example test spec. tags and 
     * expected expressions. Both comments go to tag, list contents goes as 
     * string to m_value.
     */
    public class ListElementEditor implements TestSpecAccessor {
        
        private boolean m_isAllowIndexOutOrRangeOnRead = false;

        public ListElementEditor() {}
        
        /** 
         * If sequence has less elements than index is set to, then empty
         * strings are returned instead of exception being thrown. Used for
         * sequences which have fixed number of elements inUI, but not all may 
         * be set in test spec, for example profiler min/max limits.
         * 
         * @param isAllowIndexOutOrRangeOnRead
         */
        public ListElementEditor(boolean isAllowIndexOutOrRangeOnRead) {
            m_isAllowIndexOutOrRangeOnRead = isAllowIndexOutOrRangeOnRead;
        }
        
        @Override
        public void dataFromTestSpec(CTestBase testBase) {

            CSequenceAdapter seq = new CSequenceAdapter(testBase, m_sectionId, true);
            if (m_index >= seq.size()  &&  m_isAllowIndexOutOrRangeOnRead) {
                m_value = "";
                m_newLineComment = "";
                m_endOfLineComment = "";
                
            } else {

                m_value = testBase.getTagValue(m_sectionId, m_index);

                m_newLineComment = testBase.getCommentForSeqElement(m_sectionId, m_index, 
                                                       CommentType.NEW_LINE_COMMENT);

                m_endOfLineComment = testBase.getCommentForSeqElement(m_sectionId, m_index, 
                                                         CommentType.END_OF_LINE_COMMENT);
            }
        }


        @Override
        public void dataToTestSpec(CTestBase testBase) {

            testBase.setTagValue(m_sectionId, m_index, m_value);

            commentToTestSpec(testBase);
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId,
                                m_index,
                                m_newLineComment,
                                m_endOfLineComment);
        }
        
        
        /* @Override
        public String quote(String value, EQuotingType quotingType) {
            // list values should not be quoted, because then they are treated 
            // as a single scalar, which is wrong!
            return value;
        } */
    }
    
    
    /**
     * This editor modifies/gets value of pair in user mapping. The pair
     * to be modified is selected with mapping key.
     * 
     * @author markok
     */
    public class UserMappingKeyValueEditor  implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {
            if (testBase.containsMapEntry(m_sectionId, m_key)) {
                m_value = testBase.getTagValue(m_sectionId, m_key);
            
                m_newLineComment = testBase.getComment(m_sectionId, m_key, 
                                                       CommentType.NEW_LINE_COMMENT);

                m_endOfLineComment = testBase.getComment(m_sectionId, m_key, 
                                                         CommentType.END_OF_LINE_COMMENT);
            } else {
                // means the entry does not exist in mapping
                m_value = m_newLineComment = m_endOfLineComment = "";
            }
        }
        
        
        @Override
        public void dataToTestSpec(CTestBase testBase) {
            
            if (m_value != null) {
                testBase.setTagValue(m_sectionId, m_key, m_value);

                commentToTestSpec(testBase);
            } else {
                testBase.setTagValue(m_sectionId, m_key, "");
                commentToTestSpec(testBase);
                //  removing key when value is null is confusing ... - this
                // should not be used as a workaround for key removal
                // testBase.removeMapEntry(m_sectionId, m_key);
            }
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            testBase.setComment(m_sectionId, m_key, m_newLineComment, m_endOfLineComment);
        }
    }
    
    
    /**
     * This editor modifies/gets value of pair in user mapping. The pair
     * to be modified is selected with mapping index. This is possible, since
     * YAML mappings are ordered.
     * 
     * @author markok
     */
    public class UserMappingIndexValueEditor  implements TestSpecAccessor {
        @Override
        public void dataFromTestSpec(CTestBase testBase) {
            CMapAdapter mapping = new CMapAdapter(testBase, m_sectionId, false);
            if (m_index < mapping.size()) {
                String key = mapping.getKey(m_index);

                m_value = testBase.getTagValue(m_sectionId, key);

                m_newLineComment = testBase.getComment(m_sectionId, key, 
                                                       CommentType.NEW_LINE_COMMENT);

                m_endOfLineComment = testBase.getComment(m_sectionId, key, 
                                                         CommentType.END_OF_LINE_COMMENT);
            } else {
                // when adding values with actions, it may happen that the value 
                // to be added does not exist yet, because action for adding a key
                // has not been executed yet. Remember empty strings for added cells. 
                m_value = m_newLineComment = m_endOfLineComment = "";
            }
        }
        
        
        @Override
        public void dataToTestSpec(CTestBase testBase) {

            CMapAdapter mapping = new CMapAdapter(testBase, m_sectionId, false);
            if (m_index >= mapping.size()) {
                throw new SIllegalArgumentException("Row index out of range!").
                     add("index", m_index).add("rowsInTable", mapping.size());
            }
            String key = mapping.getKey(m_index);

            if (m_value != null) {
                testBase.setTagValue(m_sectionId, key, m_value);

                commentToTestSpec(testBase);
            } else {
                testBase.setTagValue(m_sectionId, key, "");
                commentToTestSpec(testBase);
                //  removing key when value is null is confusing ... - this
                // should not be used as a workaround for key removal
                // testBase.removeMapEntry(m_sectionId, m_key);
            }
        }
        
        
        @Override
        public void commentToTestSpec(CTestBase testBase)
        {
            CMapAdapter mapping = new CMapAdapter(testBase, m_sectionId, false);
            String key = mapping.getKey(m_index);
            
            testBase.setComment(m_sectionId, key, m_newLineComment, m_endOfLineComment);
        }
    }
}
