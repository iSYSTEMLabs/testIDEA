package si.isystem.itest.wizards;

import si.isystem.connect.CTestSpecification;

public interface IExtScriptPage {

    public String generateScriptMethod(CTestSpecification testSpec);
    public String generateScriptMethodName(CTestSpecification testSpec);        
}
