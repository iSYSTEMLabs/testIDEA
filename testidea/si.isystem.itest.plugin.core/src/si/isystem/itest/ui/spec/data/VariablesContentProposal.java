package si.isystem.itest.ui.spec.data;

import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.jface.fieldassist.IContentProposal;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.StrStrMap;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.ui.utils.AsystContentProposalProvider;

/**
 * This class adds proposal functionality for variables - if '.' or '->' is 
 * encountered, then members of struct or union are given as proposals.
 * 
 * @author markok
 *
 */
public class VariablesContentProposal extends AsystContentProposalProvider {

    private String m_coreId;
    private CTestSpecification m_testSpec;

    public VariablesContentProposal() {
        super();
    }
    
    
    /**
     * 
     * @param proposals
     * @param descriptions
     * @param testSpec used to get proposals for members of structs declared
     *                 as test local vars. May be null. 
     */
    public VariablesContentProposal(String[] proposals, 
                                    String[] descriptions) {
        super(proposals, descriptions);
    }

    
    public void setCoreId(String coreId) {
        m_coreId = coreId;
    }


    public void setTestSpec(CTestSpecification testSpec) {
        m_testSpec = testSpec;
    }


    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        
        MutableObject<String> parentPrefix = new MutableObject<>();
        String identifier = getIdentifier(contents, position, parentPrefix);

        if (identifier == null) {
            return super.getProposals(contents, position);
        }
        
        int dotIdx = identifier.lastIndexOf('.');
        int derefIdx = identifier.lastIndexOf("->");
        
        String parent;
        String filterString;
        if (dotIdx < derefIdx) {  // '->' operator found
            // if user has typed 'var->' we need to get children of '*var' 
            parent = parentPrefix.getValue() + identifier.substring(0, derefIdx).trim();
            filterString = identifier.substring(derefIdx + 2); // +2 to skip '->'
        } else {
            parent = identifier.substring(0, dotIdx).trim();
            filterString = identifier.substring(dotIdx + 1); // +1 to skip '.'
        }
        
        if (parent.isEmpty()) {
            // strings like '   .m_item' are processed in normal way
            return super.getProposals(contents, position);
        }
        
        String [] children = new String[0];
        if (m_coreId != null) {
            try {
                children = GlobalsConfiguration.instance().getGlobalContainer().
                           getVarsGlobalsProvider(m_coreId).getChildren(parent);
            } catch (Exception ex) {
                // connection to winIDEA is established, but there is no 
                // type info for the given expression. There will be no proposals.
            }
        }

        return proposalsToArray(children, filterString);
    }


    /**
     * Returns identifier from the given text and cursor position. If identifier 
     * is test local variable, then it is replaced with "*(&lt;type&gt; *)0"
     * 
     * @param contents usually contents of UI control
     * @param position cursor position in UI control
     * @param parentPrefix output value, '*' or empty string is local variable
     *                     is used
     * @return
     */
    protected String getIdentifier(String contents, int position, MutableObject<String> parentPrefix) {
        
        String strBeforePos = contents.substring(0, position);
        int lastIdentifierStartIdx = getStartOfLastWord(strBeforePos);
        
        if (lastIdentifierStartIdx >= strBeforePos.length()) { 
            // no identifer was found
            return null;
        }
        
        
        String identifier = strBeforePos.substring(lastIdentifierStartIdx);
        
        // handle struct members
        int firstDotIdx = identifier.indexOf('.');
        int firstDerefIdx = identifier.indexOf("->");
        
        if (firstDotIdx == -1  &&  firstDerefIdx == -1) {
            // no dots means it is not a composed identifier (i.e. member of a struct, ...)
            // so handle it in normal way
            return null;
        } 
        
        if (firstDerefIdx == -1) {
            firstDerefIdx = Integer.MAX_VALUE;
        }
        if (firstDotIdx == -1) {
            firstDotIdx = Integer.MAX_VALUE;
        }

        String varName = identifier.substring(0, Math.min(firstDotIdx, firstDerefIdx));
        
        StrStrMap localVars = new StrStrMap();
        if (m_testSpec != null) {
            m_testSpec.getLocalVariables(localVars);
        }        
        
        
        parentPrefix.setValue("*");

        if (localVars.containsKey(varName)) {
            String varType = localVars.get(varName);
            varType = varType.replace('*', ' ').trim(); // get rid of pointer - we need pure type for this hack
            String dummyVarName = DataUtils.createDummyVarFromType(varType);
            identifier = identifier.replace(varName, dummyVarName);
            parentPrefix.setValue(""); // dummyVar '*(<type> *)0' must not get additional '*'
        }
        
        return identifier;
    }


    protected int getStartOfLastWord(String contents) {
        int size = contents.length();
        for (int i = size - 1; i >= 0; i--) {
            char chr = contents.charAt(i);
            char prevChr = 0;
            char nextChr = 0;
            
            if (i > 0) {
                prevChr = contents.charAt(i - 1);
            }
            
            if (i < size - 1) {
                nextChr = contents.charAt(i + 1);
            }

            boolean isPointer = prevChr == '-'  &&  chr == '>'  ||
                                chr == '-'  &&  nextChr == '>';

            // identifier may contain [, ], for example: a[0]->d.f
            // '*' is allowed as part of the expression. This behavior will give
            // invalid results in case of 'a*b' - users will have to write with spaces: a * b.
            // Spaces are not allowed inside expression to be taken for code completion!
            
            // Parentheses '(' and ')' are not taken as part of the name, as they are not convenient
            // for 'decltype(' proposals - new proposals with function names should start.
            
            // Parentheses '(' and ')' are important, for example: (*g_pcomplexStruct).
            // so they were returned to the condition. decltype was added as an exception below.
            
            if (!Character.isJavaIdentifierPart(chr)  &&  chr != '.' &&  !isPointer  
                    &&  chr != '['  &&  chr != ']'  &&  chr != '('  &&  chr != ')'
                    &&  chr != '*') {
                
                if (contents.substring(i + 1).startsWith("decltype")) {
                    return contents.length();
                }

                return i + 1;
            }
            
        }
        return 0;
    }
}
