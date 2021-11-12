package si.isystem.itest.preferences.controls;

import si.isystem.connect.CTestBase;

public interface IValidator {
    /**
     * 
     * @return null if OK, error in case of an error.
     */
    String validate(CTestBase testBase, int section);
}
