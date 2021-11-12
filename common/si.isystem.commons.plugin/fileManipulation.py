
import os
import re
import shutil


def svnLockFiles(files):
    fileStr = ' '.join(files)
    print('Locking files: ', fileStr)
    os.system('svn lock ' + fileStr)

def svnUnlockFiles(files):
    fileStr = ' '.join(files)
    print('Unlocking files: ', fileStr)
    os.system('svn unlock ' + fileStr)

# No special characters are allowed in oldStr except the '.' character which is handled correctly
def replaceStrings(file, oldStr, newStr):
    print("Replacing string '%s' with '%s' in file '%s'."%(oldStr, newStr, file))

    input = open(file)
    tmpFileName = file + '.tmp'
    output = open(tmpFileName, 'w')

    versionExpr = re.compile(oldStr.replace('.', r'\.'))
    
    lineNumber = 0;
    for line in input:
        lineNumber += 1
        
        if (versionExpr.search(line)):
            newLine = versionExpr.sub(newStr, line)
            output.write(newLine)
        else:
            output.write(line)

    input.close()
    output.close();
    shutil.move(tmpFileName, file)

def removeLinesContaining(file, str):
    print("Removing lines with '%s' in file '%s'."%(str, file))

    input = open(file)
    tmpFileName = file + '.tmp'
    output = open(tmpFileName, 'w')

    versionExpr = re.compile(oldStr.replace('.', r'\.'))
    
    lineNumber = 0;
    for line in input:
        lineNumber += 1
        
        if (versionExpr.search(line)):
            None #Skip copying this line
        else:
            output.write(line)

    input.close()
    output.close();
    shutil.move(tmpFileName, file)
