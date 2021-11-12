#!python

# This script updates version in all plug-in files, which contain it.
# It takes original file, writes it to new file and replaces old version
# with the new one, which is then copied back over the original, for
# example: site.xml -> (replacement done) site.xml.tmp --> site.xml

# This can't be done in ant without extension, because
# it does not support replacing of regular expressions. Replacing a
# token is not useful here, because then we need orignal file with token,
# which is copied during replacement. For example, MANIFEST.MF.tok, which
# contains token to be replaced. However, the token is not handled well
# by Eclipse editor, so we can't edit feature.xml and site.xml with
# Eclipse wizards.

# Author: markok, Jun 2008

from __future__ import print_function

import os
import re
import sys
import shutil
import datetime
import subprocess

sys.path.append("scripts")

from utilClasses import Properties

sys.path.append('../../../sdk')
import isysUtils

# These the versions of the debug and core plug-ins that are committed into svn.
# This ha been done to clear up confusion during svn commits as we always have 
# 5 files with changes because of build testing. During every build these versions 
# are replaced with the real ones and after the builds are finished they are 
# changed back to the dummy versions set here (to prevent svn changes).
fakePluginVersion = '999.999.999.AAA'

#
# Updates the entire trunk or branch - depends on where we currently are
#
def svnUpdate():
    retVal = subprocess.check_call('svn up ../../../', shell=True)
    if retVal != 0:
        raise Exception('Update failed!')

def getSvnRevision():
    
    svnUpdate()
    
    tmpFileName = 'tmp.tmp'
    subprocess.check_call('svn info > ' + tmpFileName, shell = True)
    infoFile = open(tmpFileName, 'r')
    revision = ''
    
    for line in infoFile:
        if line.startswith('Revision:'):
            revision = line.split(' ')[1].strip()
            break;

    infoFile.close()
    os.remove(tmpFileName)
    
    if len(revision) == 0:
        raise Exception('SVN Revison number can not be retrieved!')
    
    return revision

    
def getWinIdeaVersionString():
    winIDEAVersion, verStr_, verMajor, verMinor, verBuild = isysUtils.getWinIDEAVersion()
    version = 'i' + str(verMajor) + '_' + str(verMinor) + '_' + str(verBuild)
    return version

# this expression describes Eclipse plugin version as: major.minor.segment.buildString,
# where buildString must be composed of alphanumeric characters, but the first
# characters must be 'i\d', to distinguish it from other jar
# files (for example log4j-1.0.3.jar')
#versionExpr = re.compile(r'\d+\.\d+\.\d+\.i\d+_\d+_\d+_\d+')
# line identifier is used to change just some lines that contain the substring specified
def setVersionInFile(file, oldVersion, newVersion, lines=None):
    input = open(file)
    tmpFileName = file + '.tmp'
    output = open(tmpFileName, 'w')

    versionExpr = re.compile(oldVersion)
    
    newLines = []
    
    lineNumber = 0;
    for line in input:
        lineNumber += 1
        
        if (lines != None  and  lineNumber in lines)  or  (lines == None  and  versionExpr.search(line)):
            output.write(versionExpr.sub(newVersion, line))
            newLines.append(lineNumber)
        else:
            output.write(line)

    input.close()
    output.close();
    shutil.move(tmpFileName, file)

    return newLines


def getPluginVersionsSuffix(eclipseVersion):
       
    winIdeaVersion = getWinIdeaVersionString()
    svnRevision = getSvnRevision()
    
    if (eclipseVersion == 'Latest'):
        commonSuffix = '.' + winIdeaVersion + '_' + svnRevision
    elif (eclipseVersion == 'Helios'):
        commonSuffix = '.' + winIdeaVersion + '_Indigo_37_' + svnRevision

    return commonSuffix

versionedFiles = ['../si.isystem.debug.dsf/META-INF/MANIFEST.MF',
                  '../si.isystem.debug.feature/feature.xml',
                  '../si.isystem.debug.update/site.xml',
                  '../si.isystem.debug.core/META-INF/MANIFEST.MF',
                  '../si.isystem.debug.core.feature/feature.xml']

# Copied from testIDEA build project
def updateManifestVersion(manifestPath, version):
    """ Updates manifest version in the imported copy of the plug-in. """
    
    fileName = os.path.join(manifestPath, 'MANIFEST.MF')
    tmpFile = os.path.join(manifestPath, 'MANIFEST.TMP')

    print('Updating ', fileName, ' version to ', version)
    src = open(fileName)
    dest = open(tmpFile, 'w')

    for line in src:
        if line.startswith('Bundle-Version:'):
            dest.write('Bundle-Version: ' + version + '\n')
        else:
            dest.write(line)

    src.close()
    dest.close()

    shutil.copy(tmpFile, fileName)
    os.remove(tmpFile)
