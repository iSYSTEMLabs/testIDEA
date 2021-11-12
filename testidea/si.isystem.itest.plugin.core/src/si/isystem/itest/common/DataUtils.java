package si.isystem.itest.common;

import java.util.ArrayList;
import java.util.List;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.connect.IntVector;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalArgumentException;

public class DataUtils {

    /**
     * Returns string vector of the specified sequence section.
     */
    static public StrVector getVector(CTestBase testBase, int sequenceSection) {
        StrVector paramsVec = new StrVector();
        CSequenceAdapter params = new CSequenceAdapter(testBase, sequenceSection, true);
        params.getStrVector(paramsVec);
        return paramsVec;
    }

    
    static public String[] getArray(CTestBase testBase, int sequenceSection) {
        CSequenceAdapter params = new CSequenceAdapter(testBase, sequenceSection, true);
        int numItems = (int) params.size();
        String[] paramsArray = new String[numItems];
        for (int i = 0; i < numItems; i++) {
            paramsArray[i] = params.getValue(i);
        }
        return paramsArray;
    }

    
    static public List<String> getSeqAsList(CTestBase testBase, int sequenceSection) {
        CSequenceAdapter params = new CSequenceAdapter(testBase, sequenceSection, true);
        int numItems = (int) params.size();
        List<String> list = new ArrayList<>(numItems);
        for (int i = 0; i < numItems; i++) {
            list.add(params.getValue(i));
        }
        return list;
    }

    
    static public List<String> strVectorToList(StrVector vector) {
        int numItems = (int) vector.size();
        List<String> list = new ArrayList<>(numItems);
        for (int i = 0; i < numItems; i++) {
            list.add(vector.get(i));
        }
        return list;
    }

    
    static public StrVector listToStrVector(List<String> list) {
        StrVector vector = new StrVector();
        for (String item : list) {
            vector.add(item);
        }
        return vector;
    }

    
    public static StrStrMap getMap(CTestBase testBase, int section) {
        StrStrMap map = new StrStrMap();
        CMapAdapter adapter = new CMapAdapter(testBase, section, true);
        adapter.getStrStrMap(map);
        return map;
    }
    
    
    static public StrVector getKeys(CTestBase testBase, int section) {
        StrVector keys = new StrVector();
        CMapAdapter adapter = new CMapAdapter(testBase, section, true);
        adapter.getKeys(keys);
        return keys;
    }


    static public StrVector splitToVectorWithISysQualifiedNames(String expr) {
        StrVector dest = new StrVector();
        List<String> list = splitToListWithISysQualifiedNames(expr);
        for (String item : list) {
            dest.add(item);
        }
        
        return dest;
    }
    
    
    /**
     * Splits the given string on commas, and also takes single and double quotes 
     * into account - see listToString(), seqToString() for inverse operation. For empty string 
     * empty list is returned. Two commas without
     * anything between produce empty string in the list. See also unit test.
     *  
     * IMPORTANT: This method does not allow iSYSTEM qualified names with
     * double comma as a separator. See splitToListWithISysQualifiedNames()
     * below for this purpose.
     * 
     * 
     * @param expr
     * @return
     */
    static public List<String> splitToList(String expr) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        List<String> tokens = new ArrayList<>();
        int beginIndex = 0;
        
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            
            if (c == '\\') {
                if (i == expr.length() - 1) {
                    throw new SIllegalArgumentException("Unterminated escape " +
                    		"sequence at the end of string: " + expr);
                }
                i += 1;
                continue; // skip escaped character
            }
            
            if (c == '\'') {
                inSingleQuotes = !inSingleQuotes;
            }
            
            if (c == '"') {
                inDoubleQuotes = !inDoubleQuotes;
            }
            
            if (c == ','  &&  !inSingleQuotes  &&  !inDoubleQuotes) {
                tokens.add(expr.substring(beginIndex, i).trim());
                beginIndex = i + 1;
            }
        }
        
        String last = expr.substring(beginIndex).trim();
        if (!tokens.isEmpty()  ||  !last.isEmpty()) {
            tokens.add(last); // add string after the last comma
        }
        
        return tokens;
    }
    

    /**
     * Parses comma separated list of strings (var names, numbers), quoted C strings,
     * and qualified iSYSTEM variable and function names (double comma is 
     * partition separator), for example:
     *   23, "in, string", second, "main.c"#func,,sample.elf, "one,,two, \\\"three, four\"\\", " \\", "a \"b", ',', '\'', '.',
     * result:
     * 23
     * "in, string"
     * second
     * "main.c"#func,,sample.elf
     * "one,,two, \\\"three, four\"\\"
     * " \\"
     * "a \"b"
     * ','
     * '\''
     * '.'
     * 
     * @param input
     * @return
     */
    static public List<String> splitToListWithISysQualifiedNames(String input) {
    
        List<String> tokens = new ArrayList<>();
        int lastPos = 0;
        boolean isInString = false;
        boolean isInChar = false;
        
        for (int idx = 0; idx < input.length(); idx++) {
            switch (input.charAt(idx)) {
            case '\\':
                if (idx == input.length() - 1) {
                    throw new SIllegalArgumentException("Unterminated escape " +
                            "sequence at the end of string: " + input);
                }
                idx++; // skip escaped character
                break;
            case '"':
                isInString = !isInString;
                break;
            case '\'':
                isInChar = !isInChar;
                break;
            case ',':
                if (!isInString  &&  !isInChar) {
                    // handle double comma in isystem qualified names: func,,sample.elf
                    // handle case, when index is at the first comma:  f,,s
                    if (idx > 0  &&  input.length() > idx + 2  &&  
                            input.charAt(idx - 1) != ','  &&
                            input.charAt(idx + 1) == ','  &&
                            input.charAt(idx + 2) != ',') {
                        idx++; // skip next comma
                        break;
                    }

                    // handle case, when index is at the second comma:  f,,s
//                    if (idx > 1  &&  input.length() > idx + 1  &&  
//                            input.charAt(idx - 2) != ','  &&
//                            input.charAt(idx - 1) == ','  &&
//                            input.charAt(idx + 1) != ',') {
//                        break; 
//                    }
                
                    tokens.add(input.substring(lastPos, idx).trim());
                    lastPos = idx + 1;
                }
                break;
            default:
                break;
            }
        }
        
        String lastItem = input.substring(lastPos).trim();
        if (!tokens.isEmpty()  ||  !lastItem.isEmpty()) {
            tokens.add(lastItem);  // add item after the last comma
        }
        
        return tokens; 
    }

    
    /**
     * The same functionality as listToString(), but for different type of list.
     * @param seq
     * @return
     */
    static public String seqToString(CSequenceAdapter seq) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        int numItems = (int)seq.size();
        for (int idx = 0; idx < numItems; idx++) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(seq.getValue(idx));
            isFirst = false;
        }
        
        return sb.toString();
    }
    
    
    /**
     * The same functionality as listToString(), but strings are quoted. Useful
     * when list should be passed as list of literal strings to Python.
     */
    static public String listToQuotedStrings(List<String> items) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for (String item : items) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append('"').append(item).append('"');
            isFirst = false;
        }

        return sb.toString();
    }
    
    
    /**
     * Builds string out of list - inverse of splitToList(). This string is intended to be shown in 
     * UI controls, for example Text. It is not parseable by YAML parser, as
     * quotes are NOT adapted. Empty elements are added as empty string - two commas
     * with nothing between.
     * 
     * @param items
     * @see #seqToString(CSequenceAdapter)
     * @return
     */
    static public String listToString(List<String> items) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for (String item : items) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(item);
            isFirst = false;
        }

        return sb.toString();
    }
    

    /**
     * Convertls IntVector to array of integers.
     */
    static public int[] intVectorToArray(IntVector intVec) {
        int numItems = (int)intVec.size();
        int[] out = new int[numItems];
        
        for (int idx = 0; idx < numItems; idx++) {
            out[idx] = intVec.get(idx);
        }
        
        return out;
    }
    

    /**
     * Parses the given string and returns list of String[3].
     *  
     * @param data string containing data as Python list of tuples
     * @return
     */
    static public List<String[]> string2Tuples(String data) {
        
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        List<String[]> tokens = new ArrayList<>();
        int beginIndex = 0;
        String []tuple = null;
        int tupleIdx = 0;
        boolean isInTuple = false;
        
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            
            if (c == '\\') {
                if (i == data.length() - 1) {
                    throw new SIllegalArgumentException("Unterminated escape " +
                            "sequence at the end of string: " + data);
                }
                i += 1;
                continue; // skip escaped character
            }
            
            if (c == '\'') {
                inSingleQuotes = !inSingleQuotes;
            }
            
            if (c == '"') {
                inDoubleQuotes = !inDoubleQuotes;
            }
            
            if (inSingleQuotes  ||  inDoubleQuotes) {
                continue;
            }
            
            if (c == '(') {
                tuple = new String[3];
                beginIndex = i + 1;
                tupleIdx = 0;
                isInTuple = true;
                continue;
            }
            if (c == ')') {
                String valueStr = data.substring(beginIndex, i).trim();
                if (valueStr.length() > 1) {
                    if (valueStr.charAt(0) == '\''  &&  valueStr.charAt(0) == '\''  ||
                            valueStr.charAt(0) == '"'  &&  valueStr.charAt(0) == '"') {
                        // if it is Python string, remove quotes
                        valueStr = valueStr.substring(1, valueStr.length() - 1);
                    }
                }
                
                if (tuple == null) {
                    throw new NullPointerException("Invalid tuple format (missing '('): " + data); 
                }
                
                tuple[tupleIdx++] = valueStr;
                tokens.add(tuple);
                isInTuple = false;
                continue;
            }
            
            if (c == ','  && isInTuple) {
                
                if (tuple == null) {
                    throw new NullPointerException("Invalid tuple format (missing '('): " + data); 
                }
                
                tuple[tupleIdx++] = data.substring(beginIndex, i).trim();
                beginIndex = i + 1;
            }
        }
        
        return tokens;
    }
    
    
    /**
     * Use this method, when type is known, but there is no var of this type,
     * and you want to get members of the complex type.
     * 
     * @param varType name of complex type, struct or union.
     */
    static public String createDummyVarFromType(String varType) {
        return "*(" + varType + "*)0";
    }
    
    
    static public void copyComment(CTestBase srcTreeNode, int sectionId, CTestBase destTreeNode) {
        String nlComment = srcTreeNode.getComment(sectionId, SpecDataType.KEY, 
                                                 CommentType.NEW_LINE_COMMENT);
        String eolComment = srcTreeNode.getComment(sectionId, SpecDataType.KEY, 
                                                  CommentType.END_OF_LINE_COMMENT);
        destTreeNode.setComment(sectionId, SpecDataType.KEY, 
                                CommentType.NEW_LINE_COMMENT, nlComment);
        destTreeNode.setComment(sectionId, SpecDataType.KEY, 
                                CommentType.END_OF_LINE_COMMENT, eolComment);
    }


    public static StrStrMap listsToStrStrMap(List<String> keys, List<String> values) {
        if (keys.size() != values.size()) {
            throw new SIllegalArgumentException("Sizes of key and value lists differ!").
                add("keysSize", keys.size()).add("valuesSize", values.size());
        }
        
        StrStrMap map = new StrStrMap();
        for (int idx = 0; idx < keys.size(); idx++) {
            map.put(keys.get(idx), values.get(idx));
        }
        
        return map;
    }


    // TODO: move to DataUtils
    public static String[] strVector2StringArray(StrVector src) {
        int numItems = (int)src.size();
        String[] dest = new String[numItems];
        for (int i = 0; i < numItems; i++) {
            dest[i] = src.get(i);
        }
        return dest;
    }


    /**
     * Replaces invalid chars (not letters or digits or in the given list
     * of allowed symbols) with underscores.
     * @param scalar
     * @return
     */
    public static String fixRestrictedTextScalar(String scalar, String allowedSymbols) {
        StringBuilder sb = new StringBuilder();
        
        for (int idx = 0; idx < scalar.length(); idx++) {
            char chr = scalar.charAt(idx);
            if (Character.isLetterOrDigit(chr)  ||  allowedSymbols.indexOf(chr) >= 0) {
                sb.append(chr);
            } else {
                sb.append('_');
            }
        }
        
        return sb.toString();
    }
    
    
    public static int countLines(StringBuilder sb) {
        int nl = 0;
        for (int idx = 0; idx < sb.length(); idx++) {
            if (sb.charAt(idx) == '\n') {
                nl++;
            }
        }
        return nl;
    }
    
    
    public static int countLines(String str) {
        int nl = 0;
        for (int idx = 0; idx < str.length(); idx++) {
            if (str.charAt(idx) == '\n') {
                nl++;
            }
        }
        return nl;
    }
    
    
    /**
     * Returned array contains all items from 'left' plus items from 'right' not found in left.
     * @param left not modified
     * @param right not modified
     * @return new array or null, if no ints were added to 'left'
     */
    public static int[] appendNewItems(final int[] left, final int[] right) {
        int itemCounter = 0;
        int[] indices = new int[right.length];
        
        outer:
        for (int aIdx = 0; aIdx < right.length; aIdx++) {
            for (int vIdx = 0; vIdx < left.length; vIdx++) {
                if (left[vIdx] == right[aIdx]) {
                    continue outer;
                } 
            }

            indices[itemCounter++] = aIdx;
        }
        
        if (itemCounter > 0) {
            int[] unionOfVisibleSections = new int[left.length + itemCounter];
            System.arraycopy(left, 0, unionOfVisibleSections, 0, left.length);
            int fIdx = left.length;
            for (int i = 0; i < itemCounter; i++) {
                    unionOfVisibleSections[fIdx++] = right[indices[i]];
            }

            return unionOfVisibleSections;
        }
        
        return null;
    }
    
    
    /**
     * Returned array contains all items from 'left' minus items from 'right'.
     * @param left
     * @param right
     * @return new array or null, if no ints were removed from 'left'
     */
    public static int[] removeNewItems(final int[] left, final int[] right) {

        int itemCounter = 0;
        int[] indices = new int[left.length]; 
        
        outer:
        for (int vIdx = 0; vIdx < left.length; vIdx++) {
            for (int rIdx = 0; rIdx < right.length; rIdx++) {
                if (left[vIdx] == right[rIdx]) {
                    continue outer;
                }
            }
            indices[itemCounter++] = vIdx;
        }
        
        if (itemCounter < left.length) {
            int[] filteredVisibleSections = new int[itemCounter];
            for (int i = 0; i < itemCounter; i++) {
                filteredVisibleSections[i] = left[indices[i]];
            }

            return filteredVisibleSections;
        }
        
        return null;
    }
}
