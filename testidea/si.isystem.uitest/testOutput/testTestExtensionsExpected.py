# This script was generated with testIDEA script extensions wizard.
# Customize it according to your needs.


import isystem.connect as ic

class TestExtCls:

    def __init__(self, mccMgr = None):
        """
        Normally we'll connect to winIDEA, which is running the test, so that
        target state can be accessed/modified by this script.
        """

        if mccMgr == None:
            # Executed when called from testIDEA.
            # Connection can't be passed between processes.
            self.mccMgr = None
            self.connectionMgr = ic.ConnectionMgr()
            self.connectionMgr.connectMRU()
            self.debug = ic.CDebugFacade(self.connectionMgr)
        else:
            # Executed when called from generated script - connection is reused.
            self.mccMgr = mccMgr
            self.connectionMgr = mccMgr.getConnectionMgr('')
            self.debug = mccMgr.getCDebugFacade('')

        self.testCtrl = None


    def __getTestCaseCtrl(self):
        if self.testCtrl == None  or  self.testCtrl.getTestCaseHandle() != self._isys_testCaseHandle:
            self.testCtrl = ic.CTestCaseController(self.connectionMgr, self._isys_testCaseHandle)
        return self.testCtrl


    def __add_int__initTarget(self, testSpec):

        print('Test case ID: ', testSpec.getTestId())

        # examples for variable evaluation
        varAsString = self.debug.evaluate(ic.IConnectDebug.fMonitor, '<enter your var name here>').getResult()
        varAsInt = self.debug.evaluate(ic.IConnectDebug.fMonitor, '<enter your int var name here>,d').getInt()

        # examples for variable modification
        self.debug.modify(ic.IConnectDebug.fMonitor, '<enter your var name here>', '<enter new value here>')
        self.debug.modify(ic.IConnectDebug.fMonitor, 'iCounter', '42')

        self._isys_initTargetInfo = 'This text will appear in testIDEA Status view AND test report'

        print('This text will appear in testIDEA Status view but NOT in test report')

        return None  # in case of error return error description string


    def __add_int__initTest(self):

        self.__counterFor_stubScript_demoStub = 0
        self.__counterFor_testPointScript_myTestPoint = 0

        self._isys_initFuncInfo = 'This text will appear in testIDEA Status view AND test report'

        print('This text will appear in testIDEA Status view but NOT in test report')

        return None  # in case of error return error description string


    def stubScript_demoStub(self, testSpec):

        print('Test case ID: ', testSpec.getTestId())

        self.testCtrl = self.__getTestCaseCtrl()

        # examples for variable evaluation
        varAsString = self.testCtrl.evaluate('<enter your var name here>')
        varAsInt = int(self.testCtrl.evaluate('<enter your int var name here>,d'))

        # examples for variable modification
        self.testCtrl.modify('<enter your var name here>', '<enter new value here>')
        self.testCtrl.modify('iCounter', '42')

        # increment stub call counter
        self.__counterFor_stubScript_demoStub += 1

        self._isys_stubInfo = 'This text will appear in testIDEA Status view AND test report'

        print('This text will appear in testIDEA Status view but NOT in test report')

        return None  # in case of error return error description string


    def testPointScript_myTestPoint(self, testSpec):

        print('Test case ID: ', testSpec.getTestId())

        self.testCtrl = self.__getTestCaseCtrl()

        # examples for variable evaluation
        varAsString = self.testCtrl.evaluate('<enter your var name here>')
        varAsInt = int(self.testCtrl.evaluate('<enter your int var name here>,d'))

        # examples for variable modification
        self.testCtrl.modify('<enter your var name here>', '<enter new value here>')
        self.testCtrl.modify('iCounter', '42')

        # increment stub call counter
        self.__counterFor_testPointScript_myTestPoint += 1

        self._isys_testPointInfo = 'This text will appear in testIDEA Status view AND test report'

        print('This text will appear in testIDEA Status view but NOT in test report')

        return None  # in case of error return error description string


