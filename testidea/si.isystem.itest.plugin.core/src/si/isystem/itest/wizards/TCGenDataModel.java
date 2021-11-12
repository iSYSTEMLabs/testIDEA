package si.isystem.itest.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;


/**
 * Classes in this file contain model for Test Case Generation Wizard.
 * This model is used by identifiers table and by test case generator.
 * 
 * @author markok
 */

/**
 * Root class, the main container.
 * @author markok
 */
public class TCGenDataModel {

    private TCGenSection m_funcSection;
    
    // all global vars + local vars from base test case + persist vars from base test-case
    // user can also add host vars.
    private TCGenSection m_varsSection;

    private TCGenSection m_stubsSection;
    private TCGenSection m_testPointsSection;
    
    private TCGenSection m_hil;
    private TCGenSection m_options;

    private TCGenSection m_initTargetScript;
    private TCGenSection m_initFunctionScript;
    private TCGenSection m_endFunctionScript;
    private TCGenSection m_restoreTargetScript;

    // this section defines how to combine section vectors from the above sections
    // to get final vector - complete test case.
    private TCGenSection m_testCaseVectorsSection;

    private TCGenAsserts m_asserts;
    private TCGenDryRun m_dryRunConfig;
    
    private TCGenOccur m_occurrence; // how many times each combination of parameters in 
                             // each section should appear in final output
    
    
    public TCGenDataModel() {

        m_funcSection = new TCGenSection();
        m_varsSection = new TCGenSection();
        
        m_stubsSection = new TCGenSection();
        m_testPointsSection = new TCGenSection();
        
        m_hil = new TCGenSection();
        m_options = new TCGenSection();
        
        m_initTargetScript = new TCGenSection();
        m_initFunctionScript = new TCGenSection();
        m_endFunctionScript = new TCGenSection();
        m_restoreTargetScript = new TCGenSection();
        
        m_testCaseVectorsSection = new TCGenSection();
        
        m_asserts = new TCGenAsserts();
        m_dryRunConfig = new TCGenDryRun();
        
        m_occurrence = new TCGenOccur();
    }


    TCGenOccur getOccurrence() {
        return m_occurrence;
    }

    
    TCGenSection getFunctionSection() {
        return m_funcSection;
    }
    
    
    TCGenSection getVarsSection() {
        return m_varsSection;
    }


    TCGenSection getStubsSection() {
        return m_stubsSection;
    }


    TCGenSection getTestPointsSection() {
        return m_testPointsSection;
    }


    TCGenSection getHil() {
        return m_hil;
    }


    TCGenSection getOptions() {
        return m_options;
    }


    TCGenSection getInitTargetScript() {
        return m_initTargetScript;
    }


    TCGenSection getInitFunctionScript() {
        return m_initFunctionScript;
    }


    TCGenSection getEndFunctionScript() {
        return m_endFunctionScript;
    }


    TCGenSection getRestoreTargetScript() {
        return m_restoreTargetScript;
    }


    TCGenSection getTestCaseVectorsSection() {
        return m_testCaseVectorsSection;
    }

    
    public TCGenAsserts getAsserts() {
        return m_asserts;
    }


    TCGenDryRun getDryRunConfig() {
        return m_dryRunConfig;
    }
}


class TCGenSection {
    // Specifies how many times each value should occur in generated test vector.
    // If set to 'max', then all possible combinations of values in all parameters are used.
    // If set to 'Custom' then occurrences are specified for each identifier
    private TCGenOccur m_occurrence = new TCGenOccur();
    private List<TCGenIdentifier> m_identifiers = new ArrayList<>();
    
    private String m_stubbedFuncOrTestPointId = "";
    private String m_stubOrTpStepIndex = "";
    
    private boolean m_isCopyCoverage;
    private boolean m_isAppendModeOnCopy;
    private boolean m_isCopyProfiler;
    private boolean m_isCopyTrace;
    
    
    void addIdentifier(int idx, String identifierName) {
        if (idx == -1) {
            m_identifiers.add(new TCGenIdentifier(identifierName));
        } else {
            m_identifiers.add(idx, new TCGenIdentifier(identifierName));
        }
    }
    

    void removeIdentifier(int idx) {
        m_identifiers.remove(idx);
    }
        
        
    void swapIdentifiers(int firstIdx, int secondIdx) {

        if (firstIdx < 0  ||  secondIdx < 0  ||  
                firstIdx >= m_identifiers.size() ||
                secondIdx >= m_identifiers.size()) {
            return;
        }

        TCGenIdentifier first = m_identifiers.get(firstIdx);
        m_identifiers.set(firstIdx, m_identifiers.get(secondIdx));
        m_identifiers.set(secondIdx, first);
    }
        
        
    public TCGenOccur getOccurrence() {
        return m_occurrence;
    }

    
    public List<TCGenIdentifier> getIdentifiers() {
        return m_identifiers;
    }

    
    String[] getIdentifierNames() {
        String[] names = new String[m_identifiers.size()];
        
        int idx = 0;
        for (TCGenIdentifier ident : m_identifiers) {
            names[idx++] = ident.getIdentifierName();        
        }
        
        return names;
    }

    
    public TCGenIdentifier getIdentifier(MutableInt row, EOccurenceType occurrenceType) {
        return getIdentifier(row, occurrenceType, new MutableInt());
    }
    
    
    public TCGenIdentifier getIdentifier(MutableInt row, EOccurenceType occurrenceType, MutableBoolean isEvenIdx) {
        
        MutableInt identIdx = new MutableInt();
        
        TCGenIdentifier identifier = getIdentifier(row, occurrenceType, identIdx);
        isEvenIdx.setValue((identIdx.intValue() & 1) == 0);
        return identifier;
    }
    
    
    public TCGenIdentifier getIdentifier(MutableInt row, EOccurenceType occurrenceType, MutableInt identifierIdx) {

        identifierIdx.setValue(0);
        
        for (TCGenIdentifier ident  : m_identifiers) {
            row.subtract(ident.getNumRows(occurrenceType));
            if (row.intValue() < 0) {
                row.add(ident.getNumRows(occurrenceType)); // row is now index for identifier
                return ident;

            }

            identifierIdx.increment();
        }
        return null;
    }
    
    
    int getNumTableRows() {
        int rows = 0;
        for (TCGenIdentifier ident  : m_identifiers) {
            rows += ident.getNumRows(m_occurrence.getOccurrenceType());
        }
        return rows;
    }

    
    public String getStubbedFuncOrTestPointId() {
        return m_stubbedFuncOrTestPointId;
    }


    public String getStubOrTpStepIndex() {
        return m_stubOrTpStepIndex;
    }


    public void setStubOrTPInfo(String funcOrTpID, String stepIdx) {
        m_stubbedFuncOrTestPointId = funcOrTpID;
        m_stubOrTpStepIndex = stepIdx;
    }
    
    
    public void setAnalyzerInfo(boolean isCopyCoverage,
                                boolean isAppendModeOnCopy,
                                boolean isCopyProfiler,
                                boolean isCopyTrace) {
        
        m_isCopyCoverage = isCopyCoverage;
        m_isAppendModeOnCopy = isAppendModeOnCopy;
        m_isCopyProfiler = isCopyProfiler;
        m_isCopyTrace = isCopyTrace;
    }

    
    public boolean isCopyCoverage() {
        return m_isCopyCoverage;
    }


    public boolean isAppendModeOnCopy() {
        return m_isAppendModeOnCopy;
    }


    public boolean isCopyProfiler() {
        return m_isCopyProfiler;
    }


    public boolean isCopyTrace() {
        return m_isCopyTrace;
    }


    public String verifyData() {
        String msg = m_occurrence.verifyData();
        if (msg != null) {
            return msg + " See text field next to radio buttons.";
        }
        
        for (TCGenIdentifier ident : m_identifiers) {
            msg = ident.verifyData();
            if (msg != null) {
                return msg;
            }
        }
        
        return null;
    }
}


/**
 * This class contains generation data for one identifier, for example function
 * parameter or global variable.
 */
class TCGenIdentifier {
    
    private static final int MAX_VALUES_IN_RANGE_FOR_COMBO = 10;

    private static final int MAX_VALUES_PER_IDENTIFIER = 10000; // limit defined by memory
                                                           // limit in 32-bit testIDEA.
                     // Also this number of values makes no sense in real-world cases. 

    protected static final String OTHER_VALUES_STR = "<other values>";

    private String m_identifierName;
    
    // Final set of values includes both - values from the list and values generated with range. 
    private List<String> m_values = new ArrayList<>(); // list of values to be used in test vector for that identifier
    private String m_rangeStart = ""; // range of values to be used in test vector for that identifier
    private String m_rangeEnd = "";
    private String m_rangeStep = "";
    
    private TCGenOccur m_occurrence = new TCGenOccur(); // if set to custom, check setting for each value
    
    private List<TCGenValue> m_customValueOcurrences = new ArrayList<>();

    
    TCGenIdentifier(String identifierName) {
        m_identifierName = identifierName;
    }
    
    public String getIdentifierName() {
        return m_identifierName;
    }

    public List<String> getValues() {
        return m_values;
    }

    public String getValuesAsString() {
        
        return StringUtils.join(m_values, ", ");
        /*
        StringBuilder sb = new StringBuilder();
        for (String str : m_values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str);
        }
        
        return sb.toString(); */
    }

    public String getRangeStart() {
        return m_rangeStart;
    }

    public String getRangeEnd() {
        return m_rangeEnd;
    }

    public String getRangeStep() {
        return m_rangeStep;
    }

    public TCGenOccur getOccurrence() {
        return m_occurrence;
    }

    /**
     * Returns the number of rows this identifer needs for display of all its
     * data in the table.
     * 
     * @param sectionOccurenceType section occurrence type overrides setting 
     * in identifier. Display of custom occurrences for values depends on this 
     * setting and identifier occurrence setting.
     */
    int getNumRows(EOccurenceType sectionOccurenceType) {
        int numRows = 1;  // the first row, row with identifier
        
        // || !m_customValueOcurrences.isEmpty() - do not show values even if 
        // present - it will only confuse user if mode != CUSTOM
        if (m_occurrence.getOccurrenceType() == EOccurenceType.CUSTOM  &&  
                sectionOccurenceType == EOccurenceType.CUSTOM) {
            numRows += m_customValueOcurrences.size() + 2; // for value occurrence header and row with + sign
        }
        
        return numRows; 
    }
    
    
    TCGenValue getCustomValueOccurrence(int idx) {
        return m_customValueOcurrences.get(idx);
    }


    List<TCGenValue> getCustomValueOccurrences() {
        return m_customValueOcurrences;
    }

    
    public void setIdentifierName(String identifierName) {
        m_identifierName = identifierName;
    }

    
    public void setValues(String valuesStr) {
        
        if (valuesStr.trim().isEmpty()) {
            // m_values.clear(); - throws UnsupportedOperationException, because
            // Arrays.asList() returns list backed up by array - not true ArrayList
            m_values = new ArrayList<String>();
            return;
        }
        
        // Split on commas and surrounding whitespaces. Does not support
        // qualified variable names, as their usage here is highly unlikely -
        // users should use host variables in such case
        m_values = Arrays.asList(valuesStr.split("\\s*,\\s*"));
    }

    
    public void setRangeStart(String rangeStart) {
        m_rangeStart = rangeStart.trim();
    }

    
    public void setRangeEnd(String rangeEnd) {
        m_rangeEnd = rangeEnd.trim();
    }

    
    public void setRangeStep(String rangeStep) {
        m_rangeStep = rangeStep.trim();
    }

    
    public void setOccurrence(String occurrence) {
        m_occurrence.setValue(occurrence);
    }

    
    public void setCustomValueOcurrences(List<TCGenValue> customValueOcurrences) {
        m_customValueOcurrences = customValueOcurrences;
    }


    public void addValueOccurrence(TCGenValue tcGenValue) {
        m_customValueOcurrences.add(tcGenValue);
    }
    

    /**
     * Adds values in the same order as they are listed in identifier line.
     * @param idx index of value to add
     */
    public void addAutoValueOccurrence(int idx) {
        
        List<String> allValues = getAllValues();
        List<String> customValues = getCustomValues();
        Set<String> customValuesSet = new TreeSet<>(customValues);
        
        for (String value : allValues) {
            if (!customValuesSet.contains(value)) {
                addValueOccurrence(idx, value);
                return;
            }
        }
        
        // if all values are already used
        addValueOccurrence(idx, "");
    }
    
    
    private void addValueOccurrence(int idx, String valueStr) {
        
        TCGenValue tcGenValue = new TCGenValue();
        tcGenValue.setValue(valueStr);
        
        if (idx == -1) {
            m_customValueOcurrences.add(tcGenValue);
        } else {
            m_customValueOcurrences.add(idx, tcGenValue);
        }
    }
    

    void removeValueOccurrence(int idx) {
        m_customValueOcurrences.remove(idx);
    }


    void swapValueOccurrence(int firstIdx, int secondIdx) {
        if (firstIdx < 0  ||  secondIdx < 0  ||  
                firstIdx >= m_customValueOcurrences.size() ||
                secondIdx >= m_customValueOcurrences.size()) {
            return;
        }
        
        TCGenValue first = m_customValueOcurrences.get(firstIdx);
        m_customValueOcurrences.set(firstIdx, m_customValueOcurrences.get(secondIdx));
        m_customValueOcurrences.set(secondIdx, first);
    }

    
    long getDefaultOccurrenceForIdent(long maxVectorsForValue) {
        
        EOccurenceType occurrence = m_occurrence.getOccurrenceType();
        if (occurrence != EOccurenceType.CUSTOM) {
            return m_occurrence.getOccurrenceValue(maxVectorsForValue);
        }

        for (TCGenValue tcValue : m_customValueOcurrences) {
            if (tcValue.getValue().equals(OTHER_VALUES_STR)) {
                return tcValue.getOccurrence().getOccurrenceValue();
            }
        }
        
        return 1;  // This is default value of occurrences for all values, which  
                   // do not have custom occurrence value set, and custom
                   // value for OTHER_VALUES_STR is not set.
    }


    public long getOccurenceForValue(String value, 
                                     long defaultOccur, 
                                     long maxOccursForValue) {
        
        if (m_occurrence.getOccurrenceType() == EOccurenceType.CUSTOM) {
            
            for (TCGenValue tcValue : m_customValueOcurrences) {
                if (tcValue.getValue().equals(value)) {
                    return tcValue.getOccurrence().getOccurrenceValue(maxOccursForValue);
                }
            }
        }
         
        return defaultOccur;
    }
    
    
    /**
     * Returns array of string suitable to display in combo box. If there are too 
     * many values produced by range, then these are truncated to 
     * MAX_VALUES_IN_RANGE items.
     *  
     * @return
     */
    List<String> getAllValuesForCombo() {
        
        List<String> rangeValues = new ArrayList<>(m_values);

        MutableBoolean isFloat = new MutableBoolean();
        double[] range = parseRange(isFloat);

        if (range == null) {
            return rangeValues;
        }
        
        double start = range[0];
        double end = range[1];
        double step = range[2];
        
        int numValuesInRange = numValuesInRange(start, end, step);
        
        if (numValuesInRange < MAX_VALUES_IN_RANGE_FOR_COMBO) {
            if (step > 0) {
                for (double d = start; d < end; d += step) {
                    rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
                }
            } else {
                for (double d = start; d > end; d += step) {
                    rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
                }
            }
        } else {
            double d = start;
            for (int i = 0; i < (MAX_VALUES_IN_RANGE_FOR_COMBO / 3); i++) {
                rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
                d += step;
            }

            rangeValues.add("..."); // make clear that values are missing
            d = start + (end - start) / 2 - MAX_VALUES_IN_RANGE_FOR_COMBO / 3 * step;
            for (int i = 0; i < (MAX_VALUES_IN_RANGE_FOR_COMBO / 3); i++) {
                rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
                d += step;
            }
            
            rangeValues.add("..."); // make clear that values are missing
            d = end - MAX_VALUES_IN_RANGE_FOR_COMBO / 3 * step;
            for (int i = 0; i < (MAX_VALUES_IN_RANGE_FOR_COMBO / 3); i++) {
                rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
                d += step;
            }
        }
        
        return rangeValues;
    }

    
    /**
     * @return list of all values for which custom occurrence is set.
     */
    List<String> getCustomValues() {
    
        List<String> values = new ArrayList<>();
        
        for (TCGenValue vo : m_customValueOcurrences) {
            values.add(vo.getValue());
        }
        return values;
    }
    
    
    /**
     * @return list of all values for identifier - list and range values.
     */
    List<String> getAllValues() {
        
        List<String> rangeValues = new ArrayList<>(m_values);
        
        MutableBoolean isFloat = new MutableBoolean();
        double[] range = parseRange(isFloat);
        
        if (range == null) {
            return rangeValues;
        }
        
        double start = range[0];
        double end = range[1];
        double step = range[2];
        
        if (step > 0) {
            for (double d = start; d < end; d += step) {
                rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
            }
        } else {
            for (double d = start; d > end; d += step) {
                rangeValues.add(rangeValueAsStr(d, isFloat.booleanValue()));
            }
        }
        
        return rangeValues;
    }

    
    /**
     * Deprecated, because it returned inaccurate result for floats. Always call
     * getAllValues() and size of the returned array, or IdentifierData.getValuesSize() if
     * available.
     *  
     * @return the number of all values for this identifier - list and range values.
    int getSizeOfValues() {
        
        MutableBoolean isFloat = new MutableBoolean();
        double[] range = parseRange(isFloat);
        
        if (range == null) {
            return m_values.size();
        }
        
        double start = range[0];
        double end = range[1];
        double step = range[2];
        
        return numValuesInRange(start, end, step) + m_values.size();
    }
     */
    

    private double[] parseRange(MutableBoolean isFloat) {
        
        if (m_rangeStart.isEmpty() ||  m_rangeEnd.isEmpty()) {
            return null;
        }
        
        if (m_rangeStart.contains(".")  ||  m_rangeEnd.contains(".")  ||
                m_rangeStep.contains(".")) {
            isFloat.setValue(true);
        }
        
        double start = 0, end = 0, step = 0;
        try {
            start = Double.parseDouble(m_rangeStart);
            end = Double.parseDouble(m_rangeEnd);
            if (!m_rangeStep.trim().isEmpty()) {
                step = Double.parseDouble(m_rangeStep);
                if (step == 0) {
                    // invalid step for range - return values only
                    return null;
                }
            } else {
                step = 1;
            }
        } catch (NumberFormatException ex) {
            // do not report exception here - wizard should report this in title area,
            return null;
        }

        int numValuesInRange = numValuesInRange(start, end, step);
        if (numValuesInRange > MAX_VALUES_PER_IDENTIFIER) {
            return null;
        }
        
        return new double[] {start, end, step};
    }

    
    // Because floats may give different results when added multiple times in a for loop,
    // the actual number of items may differ from the number returned by this
    // method - this result is useful only for checking of limits of max values
    // allowed.
    private int numValuesInRange(double start, double end, double step) {
        return (int)Math.floor(Math.abs((end - start) / step));
    }
    
    
    private String rangeValueAsStr(double d, boolean isFloat) {
        
        if (isFloat) {
            return String.valueOf(d);
        }
        
        return String.valueOf((long)Math.round(d));
    }
    
    
    /**
     * Verifies consistency of data in this identifier.
     * 
     * @return null if OK, description in case of error.
     */
    String verifyData() {
        double start, end, step = 1;
        
        if (!m_rangeStart.isEmpty()  ||  !m_rangeEnd.isEmpty()  ||  !m_rangeStep.isEmpty()) {

            try {
                start = Double.parseDouble(m_rangeStart);
            } catch (NumberFormatException ex) {
                return "Range start for identifier '" + m_identifierName + 
                        "' should be a number, but it is: " + m_rangeStart;
            }
            try {
                end = Double.parseDouble(m_rangeEnd);
            } catch (NumberFormatException ex) {
                return "Range end for identifier '" + m_identifierName + 
                        "' should be a number, but it is: " + m_rangeEnd;
            }
            try {
                if (!m_rangeStep.isEmpty()) {
                    step = Double.parseDouble(m_rangeStep);
                }
            } catch (NumberFormatException ex) {
                return "Range step for identifier '" + m_identifierName + 
                        "' should be a number, but it is: " + m_rangeStep;
            }

            int numSteps = (int)((end - start) / step); 
            if (numSteps < 0) {
                return "Range is not valid. With step '" + step + "', end '" + end +
                        "' will never be reached for identifier '" + m_identifierName + "'.";
            }

            if (numSteps > MAX_VALUES_PER_IDENTIFIER) {
                return "Range produces to many values for identifier '" + 
                        m_identifierName + "': " + numSteps + 
                        "\nMax allowed: " + MAX_VALUES_PER_IDENTIFIER;
            }
        }
        
        String msg = m_occurrence.verifyData();
        if (msg != null) {
            msg += " See identifier " + m_identifierName;
            return msg;
        }
        
        return verifyCustomValues();
    }

    
    private String verifyCustomValues() {
        
        List<String> allValues = getAllValues();
        List<String> customValues = getCustomValues();
        Set<String> allValuesSet = new TreeSet<>(allValues);

        // check for duplicate values
        if (allValuesSet.size() < allValues.size()) {
            // at least one value is duplicated (set contains each value at most once)
            // Find out which one, to provide user friendly error message.
            Set<String> valuesSet = new TreeSet<>();
            for (String value : allValues) {
                if (valuesSet.contains(value)) {
                    return "Duplicate value found! Identifier: '" + m_identifierName +
                           "',   Value: '" + value + "'.";
                }
                valuesSet.add(value);
            }
        }
        
        Set<String> duplicatesDetector = new TreeSet<String>();
        
        for (String value : customValues) {
            if (!allValuesSet.contains(value)  &&  !value.equals(OTHER_VALUES_STR)) {
                return "Custom value occurrence contains value, which is not in values list or in range!\n"
                        + "Identifer: '" + m_identifierName + "'   Invalid value: " + value;
            }
            
            if (duplicatesDetector.contains(value)) {
                return "Custom value occurrence contains duplicated value!\n"
                        + "Identifer: '" + m_identifierName + "'   Invalid value: " + value;
            }
            duplicatesDetector.add(value);
        }
        
        for (TCGenValue value : m_customValueOcurrences) {
            String msg = value.verifyData();
            if (msg != null) {
                return msg + " Identifier: '" + m_identifierName 
                        + "'.";
            }
        }
        
        return null;
    }
}


/**
 * Specifies value with custom occurrence.
 * 
 * @author markok
 */
class TCGenValue {
    String m_value;
    TCGenOccur m_occurs = new TCGenOccur();

    public TCGenValue() {}

    public TCGenValue(TCGenValue src) {
        m_value = src.m_value;
        m_occurs = src.m_occurs;
    }

    public String getValue() {
        return m_value;
    }
    
    public TCGenOccur getOccurrence() {
        return m_occurs;
    }

    public void setValue(String value) {
        m_value = value;
    }

    public String verifyData() {
        
        String msg = m_occurs.verifyData();
        if (msg != null) {
            return msg + " Custom value: '" + m_value + "'";
        }
        return null;
    }
}


/**
 * This class contains data for occurrence setting. 
 * 
 * @author markok
 *
 */
class TCGenOccur {
    enum EOccurenceType {
        ONE("1"), 
        TWO("2"), 
        THREE("3"), 
        N_TIMES(""), // user specifies the number
        MAX("Max"),  // all possible combinations with values of other identifiers
        CUSTOM("Custom"); // each value has its own occurrence specified. 
        
        private String m_uiString;

        EOccurenceType(String uiString) {
            m_uiString = uiString;
        }
        
        String getUIString() {
            return m_uiString;
        }
    };
    
    public final static int MAX_OR_CUSTOM_OCCURS = -1; 
    
    // testIDEA can handle 30000-50000 test case in 32-bit version. However,
    // 10000 test case for single function make no sense, so even this limit is
    // to high. Test generator is not replacement for thinking.
    final static int MAX_OCCURRS = 10000;
    
    // If set to 'max', then this value is used with all possible combinations 
    // of other values.
    private EOccurenceType m_occurs = EOccurenceType.ONE;
    private String m_nTimes = "5";
    
    
    void assign(TCGenOccur src) {
        m_occurs = src.m_occurs;
        m_nTimes = src.m_nTimes;
    }
    
    
    EOccurenceType getOccurrenceType() {
        return m_occurs;
    }
    

    /**
     * If occurrence value is defined as a number, it is returned. It it set to
     * Max or Custom, MAX_OR_CUSTOM_OCCURS is returned.
     */
    public int getOccurrenceValue() {
        switch (m_occurs) {
        case ONE:
        case TWO:
        case THREE:
            return m_occurs.ordinal() + 1;
        case N_TIMES:
            try {
                int n = Integer.parseInt(m_nTimes);
                
                if (n > 0  &&  n < MAX_OCCURRS) {
                    return n;
                }
                
                throw new IllegalArgumentException("Number in section 'occurence' is out of range: " + n + "\n"
                            + "Should be in interval [1.." + MAX_OCCURRS + "]");
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Section 'occurrence' should contain integer value!\n" + 
                        ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        case CUSTOM:
        case MAX:
            return MAX_OR_CUSTOM_OCCURS;
        default:
        }
        throw new IllegalArgumentException("Unknown enum value: " + m_occurs);
    }

    
    /**
     * If occurrence value is defined as a number, it is returned. It it set to
     * MAX, then maxOccurrence is returned. If set to Custom, MAX_OR_CUSTOM_OCCURS is returned.
     */
    public int getOccurrenceValue(long maxOccurrence) {
        int occurs = getOccurrenceValue();
        
        if (occurs == MAX_OR_CUSTOM_OCCURS) {
            if (m_occurs == EOccurenceType.MAX) {
                return (int)maxOccurrence;
            }
            return MAX_OR_CUSTOM_OCCURS;
        }
        
        return occurs;
    }

    
    String getUIString() {
        if (m_occurs == EOccurenceType.N_TIMES) {
            return String.valueOf(m_nTimes);
        }
        return m_occurs.getUIString();
    }

    
    public void setOccurrenceType(EOccurenceType occurs) {
        m_occurs = occurs;
    }


   /* public void setnTimes(String nTimes) {
        m_nTimes = nTimes;
    } */
  
    
    public void setValue(String occurrence) {
        occurrence = occurrence.trim();
        for (EOccurenceType occurrenceType : EOccurenceType.values()) {
            if (occurrenceType.getUIString().equals(occurrence)) {
                m_occurs = occurrenceType;
                return;
            }
        }
        
        m_occurs = EOccurenceType.N_TIMES;
        m_nTimes = occurrence;
    }
    

    public void setNTimesValue(String occurrence) {
        occurrence = occurrence.trim();
        
        m_occurs = EOccurenceType.N_TIMES;
        m_nTimes = occurrence;
    }
    

    public String getNTimesValue() {
        return m_nTimes;
    }

    
    public String verifyData() {
        
        if (m_occurs != EOccurenceType.N_TIMES) {
            return null;
        }
        
        try {
            int n = Integer.parseInt(m_nTimes);
            
            return n > 0  &&  n < MAX_OCCURRS ? null : 
                "Number in section 'occurence' is out of range: " + n + "\n"
                        + "Should be in interval [1.." + MAX_OCCURRS + "]";
        } catch (NumberFormatException ex) {
            return "Section 'occurrence' should contain integer value!\n" + 
                    ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
    }
}


class TCGenAsserts {
    // The following are lists of String[1], because this suits 
    // KTabelForStringsModel - each line is array of strings, even if table has
    // single column.
    List<String[]> m_expressions;
    List<String[]> m_preConditionExpressions;
    List<String[]> m_stubExpressions;
    List<String[]> m_testPointExpressions;
    
    public TCGenAsserts() {
        m_expressions = new ArrayList<>();
        m_preConditionExpressions = new ArrayList<>();
        m_stubExpressions = new ArrayList<>();
        m_testPointExpressions = new ArrayList<>();
    }


    public List<String[]> getExpressions() {
        return m_expressions;
    }


    public List<String[]> getPreConditionExpressions() {
        return m_preConditionExpressions;
    }


    public List<String[]> getStubExpressions() {
        return m_stubExpressions;
    }


    public List<String[]> getTestPointExpressions() {
        return m_testPointExpressions;
    }
}


class TCGenDryRun {
    private List<String[]> m_varAssignments;
    private boolean m_isUpdateCoverage;
    private boolean m_isUpdateProfiler;
    private String m_profilerStatsMultiplier = "";
    private String m_profilerStatsOffset = "";
    
    
    public TCGenDryRun() {
        m_varAssignments = new ArrayList<>();
    }


    public List<String[]> getVarAssignments() {
        return m_varAssignments;
    }


    public boolean isUpdateCoverage() {
        return m_isUpdateCoverage;
    }


    public boolean isUpdateProfiler() {
        return m_isUpdateProfiler;
    }

    
    public String getProfilerStatsMultiplier() {
        return m_profilerStatsMultiplier;
    }


    public String getProfilerStatsOffset() {
        return m_profilerStatsOffset;
    }


    public void setAnalyzerInfo(boolean isUpdateCoverage, 
                                boolean isUpdateProfiler,
                                String multiplier,
                                String offset) {
        m_isUpdateCoverage = isUpdateCoverage;
        m_isUpdateProfiler = isUpdateProfiler;
        m_profilerStatsMultiplier = multiplier;
        m_profilerStatsOffset = offset;
    }


    public boolean isEmpty() {
        
        return !m_isUpdateCoverage  &&  
               !m_isUpdateProfiler  &&  
                m_varAssignments.isEmpty();
    }
}


/* Removed, because the wizard would be to complex if steps and multiple
 * stubs/test points would be allowed. 
 * 
class TCGenStubTPointStep {
    List<TCGenIdentifier> m_assignIdentifiers;
    List<TCGenIdentifier> m_scriptParams;
    int m_gotoStepIdx;
}
*/

