
import os
import re
    
# Draws a big pretty looking sign
def printAnouncement(msg, char='=', lineCount=1, width=80):
    separatorStr = char * width
    
    print ('')
    for i in range(lineCount):
        print (separatorStr)

    minBorder = width//10
    if minBorder < 3:
        minBorder = 3
    
    # left and right border + space on each side
    lineSpace = width - 2*minBorder
    
    #msg = " ".join(msg.split(' '))
    msg = re.sub('\s{2,}', ' ', msg.strip())
    words = msg.split(' ');

    line = ' '
    for i in range(len(words)):
        line += words[i] + ' '
        
        if (i+1 < len(words))  and  (len(line + words[i+1] + ' ') <= lineSpace):
            #print("adding '%s'"%(line))
            None
        else:
            #print("printing '%s'"%(line))
            space = lineSpace-len(line)
            leftSpace = space//2
            rightSpace = space-leftSpace
            
            print ("%s%s%s%s%s"%(char*minBorder, ' '*leftSpace, line, ' '*rightSpace, char*minBorder))
            line = ' '

    for i in range(lineCount):
        print (separatorStr)
    print ('')

def getEnvironmentVariable(varName, defaultValue):
    if varName in os.environ:
        appsPath = os.environ[varName]
    else:
        appsPath = defaultValue
        print("Warining: enviroment variable ", APPS_ENV_VAR_NAME, " not set! Using default value '", appsPath, "'")
    return appsPath

def getISystemAppsPath():
    return getEnvironmentVariable('ISYSTEM_APPS', None)

def getFolderSuffix(isArch64, isDebug):
    if isArch64:
        if isDebug:
            return 'x86_64-debug'
        else:
            return 'x86_64-release'
    else:
        if isDebug:
            return 'x86_32-debug'
        else:
            return 'x86_32-release'

# Returns true if all input paths are older than all output paths
# This can be used to determine if we need to rebuild something        
def isUpToDate(inputPaths, outputPaths):
    if inputPaths == None  or  len(inputPaths) == 0:
        return False;
    if outputPaths == None  or  len(outputPaths) == 0:
        return False;
    
    maxInputTime = None
    maxInPath = None
    print("Input paths (", len(inputPaths), ")")
    for p in inputPaths:
        if p == None:
            print("WARNING: 'None' input path - build required")
            return False
            
        if not os.path.exists(p):
            print("WARNING: Input path missing '%s' - build required"%(p))
            return False
            
                    
        if os.path.isfile(p):
            time = os.path.getmtime(p)
            if maxInputTime == None  or  time > maxInputTime:
                maxInputTime = time
                maxInPath = p
        elif os.path.isdir(p):
            fIdx = 0
            fileList = []
            for root, subFolders, files in os.walk(p):
                for f in files:
                    path = os.path.join(root, f)
                    time = os.path.getmtime(path)
                    if maxInputTime == None  or  time > maxInputTime:
                        maxInputTime = time
                        maxInPath = path
        else:
            print("WARNING: input path '%s' is not a file nor a folder - build required"%(p))
            return False
        
    minOutputTime = None
    minOutPath = None
    print("Output paths (", len(outputPaths), ")")
    for p in outputPaths:
        if p == None:
            print("WARNING: 'None' output path - build required")
            return False
            
        if not os.path.exists(p):
            print("WARNING: Output path missing '%s' - build required"%(p))
            return False
            
                    
        if os.path.isfile(p):
            time = os.path.getmtime(p)
            if minOutputTime == None  or  time > minOutputTime:
                minOutputTime = time
                minOutPath = p
        elif os.path.isdir(p):
            fIdx = 0
            fileList = []
            for root, subFolders, files in os.walk(p):
                for f in files:
                    path = os.path.join(root, f)
                    time = os.path.getmtime(path)
                    if minOutputTime == None  or  time < minOutputTime:
                        minOutputTime = time
                        minOutPath = path
        else:
            print("WARNING: input path '%s' is not a file nor a folder - build required"%(p))
            return False
        
    print("Max input time:  ", maxInputTime, " @ ", maxInPath)
    print("Min output time: ", minOutputTime, " @ ", minOutPath)

    if maxInputTime == None  or  minOutputTime == None:
        return False

    return maxInputTime < minOutputTime
