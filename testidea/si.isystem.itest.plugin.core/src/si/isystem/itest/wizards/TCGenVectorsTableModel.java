package si.isystem.itest.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import si.isystem.itest.main.Activator;
import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;
import si.isystem.ui.utils.FontProvider;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;

/**
 * This class contains model for KTable, which displays generated vectors.
 * It also generates the vectors.
 * 
 * @author markok
 *
 */
public class TCGenVectorsTableModel extends KTableModelAdapter {

    // greater values may cause memory problems on 32-bit testIDEA, and
    // this value should be more than enough for single function.
    private static final long MAX_ALLOWED_VECTORS = 10000;

    // source data from which vectors are generated.
    private TCGenSection m_sectionModel;
    
    // The following 3 members contain data structures used by generator algorithm.
    // All arrays have the size set to the number of identifiers.
    private int[] m_counter; // Each element is index to identifier's value.
                             // Since each ident. has [0..N] values, element at
                             // identifier's index is in range [0..N]. This array
                             // is used as counter, where each digit may have different base.
    private int[] m_maxCounterVal; // caches num of values ('N') for each identifier
    private IdentifierData[] m_identData;

    // contains generated vectors
    private List<String []> m_vectors = new ArrayList<>();
    // used only to avoid duplicates
    private Set<int[]> m_vectorsSet = new TreeSet<>();
    
    TCGenVectorsTableModel() {

        m_textCellRenderer.setAlignment(SWTX.ALIGN_HORIZONTAL_RIGHT |
                                        SWTX.ALIGN_VERTICAL_CENTER);
        m_headerCellRenderer.setAlignment(SWTX.ALIGN_HORIZONTAL_RIGHT |
                                          SWTX.ALIGN_VERTICAL_CENTER);
    }
    

    public void setData(TCGenSection tcGenSection) {
        m_sectionModel = tcGenSection;
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        if (column == 0) {
            return 50;
        }
        return 100;
    }

    
    @Override
    public int getInitialRowHeight(int row) {
        return FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {
        
        // identifier names in column header
        if (row == 0) {
            if (col > 0) {
                return m_sectionModel.getIdentifiers().get(col - 1).getIdentifierName();
            }
            return "";
        }
        
        // indices in row header
        if (col == 0) {
            return String.valueOf(row);
        }
        
        int identIdx = col - 1;
        int vectorIdx = row - 1;
        return m_vectors.get(vectorIdx)[identIdx];
    }

    
    @Override
    public int doGetRowCount() {
        return m_vectors.size() + 1;  // +1 for header
    }

    
    @Override
    public int doGetColumnCount() {
        
        return m_sectionModel.getIdentifiers().size() + 1; // 1 for index column
    }
    

    public void clearVectors() {
        m_vectors.clear();
        m_vectorsSet.clear();
    }

    
    /**
     * Entry method for vector generator.
     * @return 
     */
    public List<String[]> generateVectors() {

        // create internal data structures first.
        int numIdents = m_sectionModel.getIdentifiers().size();
        m_counter = new int[numIdents];
        m_maxCounterVal = new int[numIdents];
        m_identData = new IdentifierData[numIdents];
        long numAllVectors = 1;
        
        for (int i = 0; i < numIdents; i++) {
            TCGenIdentifier identifier = m_sectionModel.getIdentifiers().get(i);
            m_identData[i] = new IdentifierData(identifier); 
            m_counter[i] = 0;
            m_maxCounterVal[i] = m_identData[i].getValuesSize();
            numAllVectors *= m_maxCounterVal[i];
        }

        for (int i = 0; i < numIdents; i++) {
            m_identData[i].initialize(m_sectionModel.getOccurrence(), numAllVectors);
        }
        
        generate();
        
        return m_vectors;
    }
    
    
    private void generate() {
        
        CounterComparator comparator = new CounterComparator();
        m_vectors = new ArrayList<>();
        m_vectorsSet = new TreeSet<int[]>(comparator);

        
        for (int primaryIdentifierIdx = 0; primaryIdentifierIdx < m_identData.length; primaryIdentifierIdx++) {
        
            IdentifierData primaryIdentData = m_identData[primaryIdentifierIdx];
            
            for (m_counter[primaryIdentifierIdx] = 0;
                    m_counter[primaryIdentifierIdx] < primaryIdentData.m_values.length;
                    m_counter[primaryIdentifierIdx]++) {

                // initialize counter so that all indices are the same as index
                // of current value of the primary identifier. This way we get less
                // vectors since different values are used in each generated vector.
                // If counter would always start at for example 
                //   0 0 <primaryValueIdx] 0 0 0
                // then the first values in list would be used more often than the others.
                for (int cntIdx = 0; cntIdx < m_counter.length; cntIdx++) {
                    m_counter[cntIdx] = m_counter[primaryIdentifierIdx] % m_maxCounterVal[cntIdx];
                }
                
                int [] startCounter = Arrays.copyOf(m_counter, m_counter.length);

                ValueData primaryValue = primaryIdentData.getValueData(m_counter[primaryIdentifierIdx]);

                do {

                    if (!primaryValue.isOccursOK()  &&  !m_vectorsSet.contains(m_counter)) {
                        addToSetAndIncOccurs();
                    }

                    incCounter(primaryIdentifierIdx);

                } while (!primaryValue.isOccursOK()  &&  !Arrays.equals(startCounter, m_counter));

                if (!primaryValue.isOccursOK()) {
                    TCGenIdentifier tcGenIdent = primaryIdentData.getIdentifier();
                    throw new IllegalStateException("All possible vectors were generated, "
                            + "but occurrence requirement could not be met!\n"
                            + "Identifier: '" 
                            + tcGenIdent.getIdentifierName() 
                            + "',  Value index: " + m_counter[primaryIdentifierIdx] 
                            + ",  Req. occurrence: " + primaryValue.m_requiredOccurs
                            + ",  Curr. occurrence: " + primaryValue.m_currentOccurs);
                }
            }
        }
    }


    // Adds vector to set and list, and increments counters of occurrence for each 
    // value used in the vector.
    private void addToSetAndIncOccurs() {
        ValueData value;
        m_vectorsSet.add(Arrays.copyOf(m_counter, m_counter.length));
        
        String [] vector = new String[m_identData.length];
        for (int identIdx = 0; identIdx < m_identData.length; identIdx++) {
            vector[identIdx] = m_identData[identIdx].getCachedValue(m_counter[identIdx]);
        }
        m_vectors.add(vector);
        
        if (m_vectorsSet.size() > MAX_ALLOWED_VECTORS) {
            throw new IllegalArgumentException("Too many vectors generated! Limit: " + 
                                               MAX_ALLOWED_VECTORS);
        }
        
        // increment occurrences in ValueData
        for (int incIdx = 0; incIdx < m_counter.length; incIdx++) {
            value = m_identData[incIdx].getValueData(m_counter[incIdx]);
            value.m_currentOccurs++;
        }
    }
    
    
//    private boolean incCounter() {
//        int idx = 0;
//        boolean isOverflow = false;
//        
//        do {
//            isOverflow = false;
//            m_counter[idx]++;
//            
//            if (m_counter[idx] > m_maxCounterVal[idx]) {
//                isOverflow = true;
//                m_counter[idx] = 0;
//                idx++;
//            }
//        } while (idx < m_counter.length  &&  isOverflow);
//        
//        return Arrays.equals(m_counter, m_startCounterVal);
//    }
    

    /*
     * @param skipIdx which index to skip in counter. If -1, normal counting 
     * is performed.
     */
    private void incCounter(int skipIdx) {
        
        for (int idx = m_counter.length - 1; idx >= 0; idx--) {
            
            // System.out.println(m_counter[idx] + " " + m_maxCounterVal[idx]);
            
            if (idx == skipIdx) {
                continue;
            }
            
            m_counter[idx]++;
            
            if (m_counter[idx] < m_maxCounterVal[idx]) {
                break;
            } else {
                m_counter[idx] = 0;
            }
        };
    }
}


class IdentifierData {

    ValueData [] m_values;
    private long m_maxVectorsForValue;
    private TCGenIdentifier m_identifier;
    private List<String> m_cachedValues;
    
    IdentifierData(TCGenIdentifier ident) {

        m_identifier = ident;
        m_cachedValues = ident.getAllValues();
    }
    
    
    void initialize(TCGenOccur sectionOccur, long maxPossibleVectors) {
        
        m_values = new ValueData[m_cachedValues.size()];
        if (m_values.length == 0) {
            throw new IllegalArgumentException("Please specify at least one value "
                    + "for identifier: '" + m_identifier.getIdentifierName() + "' in the table below.");
        }
        
        m_maxVectorsForValue = maxPossibleVectors / m_cachedValues.size();
        
        long defaultOccur = 1;
        defaultOccur = sectionOccur.getOccurrenceValue();
        if (defaultOccur == TCGenOccur.MAX_OR_CUSTOM_OCCURS) { // custom occurrence
            EOccurenceType sectionOccurType = sectionOccur.getOccurrenceType();

            if (sectionOccurType == EOccurenceType.MAX) {
                defaultOccur = m_maxVectorsForValue;
            } else if (sectionOccurType == EOccurenceType.CUSTOM) {
                defaultOccur = m_identifier.getDefaultOccurrenceForIdent(m_maxVectorsForValue);
            } else {
                throw new IllegalArgumentException("Can not generate vectors - invalid "
                        + "occurrence type specified for identifier: '" + m_identifier.getIdentifierName() + 
                        "',   occurrenceType: " + sectionOccurType);
            }
        }
        
        // defaultOccur is defined now
        
        int valIdx = 0;
        for (String value : m_cachedValues) {
            long valueOccur = m_identifier.getOccurenceForValue(value, 
                                                                defaultOccur, 
                                                                m_maxVectorsForValue);
    
            m_values[valIdx] = new ValueData(valIdx++, (int)valueOccur);
        }
    }
    
    
    int getValuesSize() {
        return m_cachedValues.size();
    }

    
    public String getCachedValue(int i) {
        return m_cachedValues.get(i);
    }


    TCGenIdentifier getIdentifier() {
        return m_identifier;
    }
    
    
    ValueData getValueData(int idx) {
        return m_values[idx];
    }
}


class ValueData {

    int m_valIdx; // if no sorting will be introduced, this is redundant
    int m_requiredOccurs;
    int m_currentOccurs = 0;
    
    public ValueData(int valIdx, int requiredOccurs) {
        m_valIdx = valIdx;
        m_requiredOccurs = requiredOccurs;
    }

    public boolean isOccursOK() {
        return m_currentOccurs >= m_requiredOccurs;
    }
}


class CounterComparator implements Comparator<int []>
{

    @Override
    public int compare(int[] o1, int[] o2) {
        
        for (int i = o1.length - 1; i >= 0; i--) {
            if (o1[i] < o2[i]) {
                return -1;
            }
            if (o1[i] > o2[i]) {
                return 1;
            }
        }
        
        return 0; // arrays are equal
    }
}