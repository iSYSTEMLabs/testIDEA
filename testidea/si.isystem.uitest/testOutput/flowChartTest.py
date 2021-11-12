# This script in intended for iSYSTEM internal use.
#
# It creates fake classes to test flow chart drawing algorithm.
# It is executed by testIDEA UI tests.
#
# (c) iSYSTEM AG, 2015

print("tttttttttttttttttttteeeeeeeeeeeeessssssssstt")

import sys
import isystem.flowChart as fc

# Implements only methods used by tested script
class TestInstruction:

    def __init__(self, addr, target, opCode, isSeq, isDirect, isIndirect, isCond, _isCall):
        self.addr = addr
        self.target = target
        self.opCode = opCode
        self.isSeq = isSeq
        self.isDirect = isDirect
        self.isIndirect = isIndirect
        self.isCond = isCond
        self._isCall = _isCall
    
    def getAddress(self):
        return self.addr;
        
    def getJumpTarget(self):
        return self.target

    def getOpCode(self):
        return self.opCode

    def isFlowSequential(self):
        return self.isSeq

    def isFlowDirectJump(self):
        return self.isDirect

    def isFlowIndirectJump(self):
        return self.isIndirect
            
    def isConditional(self):
        return self.isCond

    def isCall(self):
        return self._isCall

    def toString(self):
        return ('TestInstruction:' +
                '  addr: ' + str(self.addr) +
                '\n  opCode: ' + self.opCode +
                '\n  target: ' + str(self.target))
        

class TestInstructionIterator:

    def __init__(self):
        self.instructions = []
        self.idx = 0

    def setIndex(self, idx):
        self.idx = idx;
        
    def addInstr(self, instr : TestInstruction):
        self.instructions.append(instr)

    # immitating methods
    def hasNext(self):
        return self.idx < len(self.instructions)

    def isAddressInRange(self, address):
        return address > 0xf0000000

    def next(self):
        instr = self.instructions[self.idx]
        self.idx += 1
        return instr

    def peek(self):
        return self.instructions[self.idx]

tii = TestInstructionIterator()
addr = 0
#                            addr, target, opCode, isSeq, isDir, isIndir, isCond, isCall):
tii.addInstr(TestInstruction(addr, 0,   'ld a, 0', True,  False, False,  False, False))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld b, 0', True,  False, False,  False, False))
addr += 1
# direct jump
tii.addInstr(TestInstruction(addr,
                             addr + 1,  'jmp addr', False, True,  False,  False, False))
addr += 1
# indirect jump
tii.addInstr(TestInstruction(addr, 0,   'jmp (HL)', False, False, True,  False, False))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld c, 0', True, False, False,  False, False))
addr += 1

# direct call
tii.addInstr(TestInstruction(addr,
                             1000,  'call addr', False, True,  False,  False, True))
addr += 1
# indirect call
tii.addInstr(TestInstruction(addr, 0,   'call (HL)', False, False, True,  False, True))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld d, 0', True, False, False,  False, False))
addr += 1

# CONDITIONAL jumps and calls

# direct jump
tii.addInstr(TestInstruction(addr,
                             addr + 2,  'jnc addr3', False, True,  False,  True, False))
addr += 1
# indirect jump
tii.addInstr(TestInstruction(addr, 0,   'jnz (A)', False, False, True,  True, False))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld e, 0', True, False, False,  False, False))
addr += 1

# direct call
tii.addInstr(TestInstruction(addr,
                             1001,  'call_nz addr4', False, True,  False,  True, True))
addr += 1
# indirect call
tii.addInstr(TestInstruction(addr, 0,   'call_nz (HL)', False, False, True,  True, True))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld ix, 0', True, False, False,  False, False))
addr += 1
tii.addInstr(TestInstruction(addr, 0,   'ld iy, 0', True, False, False,  False, False))
addr += 1

fc.g_testIter = tii

fc.main(sys.argv[1:])

# uncomment for cmd line testing
# fc.main(['-o', '--dot',
#          r'C:\winIDEA\2012\graphwiz\bin',
#          '--function',
#          'funcTestStubsNested',
#          r'd:\bb\trunk\sdk\mpc5554Sample\report\test-flowChart.svg'])
