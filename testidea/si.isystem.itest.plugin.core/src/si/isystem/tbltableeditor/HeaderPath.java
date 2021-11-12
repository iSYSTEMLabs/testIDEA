package si.isystem.tbltableeditor;

import java.util.Arrays;

public class HeaderPath {

    public final static char LIST_MARK = '@';
    public final static char SEQ_MARK = '#';
    public final static char MAP_MARK = '%';
    public final static char SEPARATOR = '/';
    
    String m_abstractPath; // abstract node path, where indices in lists and 
                           // sequences, and keys in user maps are replaced with 
                           // special MARK characters defined above.
    
    int [] m_listIndices;      // indices in the same order as '#' are found in path
    int m_seqIndex;        
    String m_mapKey;       // keys in the same order as '%' are found in path
    
    
    public HeaderPath(String abstractPath,
                      int[] listIndices,
                      int seqIndex,
                      String mapKey) {
        
        m_abstractPath = abstractPath;
        m_listIndices = listIndices;
        m_seqIndex = seqIndex;
        m_mapKey = mapKey;
    }


    public String getAbstractPath() {
        return m_abstractPath;
    }
    
    
    public int[] getListIndices() {
        return m_listIndices;
    }
    
    
    public int getSeqIndex() {
        return m_seqIndex;
    }
    
    
    public String getKey() {
        return m_mapKey;
    }
    
    
    /**
     * @return path with indices and keys inserted.
     */
    public String getExactPath() {
        String res = m_abstractPath;
        for (int idx : m_listIndices) {
            res = res.replaceFirst("\\\\" + LIST_MARK, Integer.toString(idx));
        }
        
        res = res.replace(String.valueOf(SEQ_MARK), m_mapKey);
        res = res.replace(String.valueOf(MAP_MARK), m_mapKey);
        
        return res;
    }


    @Override
    public String toString() {
        return "HeaderPath:"
               + "\n  m_abstractPath: " + m_abstractPath
               + "\n  m_listIndices: " + Arrays.toString(m_listIndices)
               + "\n  m_seqIndex: " + m_seqIndex 
               + "\n  m_mapKey: " + m_mapKey;
    }
    
    
    
}
