package si.isystem.itest.common.ktableutils;

import si.isystem.connect.CTestBase;
import si.isystem.itest.model.AbstractAction;

public interface IKTableModelChangedListener {
    void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded);
}
