# This script creates intex.html out of XML TOC files, and copies
# all testIDEA help files to web.

# Run this script with Python 3!
#from __future__ import print_function

from __future__ import print_function

import os
import sys
import ftplib
import shutil
import xml.dom.minidom as mdom

sys.path.append('../../IConnectSWIG')
import isysUtils

def printHelp():
    print('Invalid option! Use:')
    print('  -b - generate index.html')
    print('  -d - deploy to web')


def __ftpDirToWeb(ftp, localDir):
    files = os.listdir(localDir)
    index = 1
    for fName in files:
        # do not upload XML and PYthon scripts
        if fName.endswith('.xml') or fName.endswith('.py'):
            continue
        
        src = os.path.join(localDir, fName)
        if os.path.isdir(src): # do not go to subdirs
            # if any of the folders exist, ignore error. 
            try:
                ftp.mkd(fName)
            except:
                pass
            ftp.cwd(fName)
            __ftpDirToWeb(ftp, src)
            ftp.cwd('..')
        else:
            print('Storing(' + str(index) + '/' + str(len(files)) + '): ' + src)

            inf = open(src, 'rb')
            index += 1
            ftp.storbinary('STOR ' + fName, inf)
            inf.close()


def __ftpHelpToWeb():
    ftpServer = 'www247.your-server.de'
    ftpUserForAPIDoc = 'testidv_2'
    ftpPassForAPIDoc = 'kry9wund4o'
    
    ftp = ftplib.FTP(ftpServer, ftpUserForAPIDoc, ftpPassForAPIDoc)

    # if any of the folders exist, ignore error. If they don't exist,
    # cwd command will fail.    
    try:
        ftp.mkd('testIDEA')
    except:
        pass
    
    try:
        ftp.mkd('testIDEA/help')
    except:
        pass
    
    ftp.cwd('testIDEA/help')
    
    __ftpDirToWeb(ftp, '.')
    
    ftp.close()


def copyHelpToDiskM():
    mainHelpDir = 'M:/testIDEAHelp'
    isysUtils.rmTree(mainHelpDir)
    os.mkdir(mainHelpDir)
    shutil.copy('index.html', mainHelpDir)
    tmpDirName = 'gettingstarted'
    shutil.copytree(tmpDirName, os.path.join(mainHelpDir, tmpDirName))
    tmpDirName = 'concepts'
    shutil.copytree(tmpDirName, os.path.join(mainHelpDir, tmpDirName))
    tmpDirName = 'tasks'
    shutil.copytree(tmpDirName, os.path.join(mainHelpDir, tmpDirName))
    print('Files copied to M:\\')
    

def xml2Index():
    
    allFiles = os.listdir('.')

    index = {}
    for fName in allFiles:
        if fName.endswith('.xml'):
            dom = mdom.parse(fName)

            links = []
            for node in dom.childNodes:
                if node.nodeName == 'toc':
                    for topic in node.childNodes:
                        if topic.nodeName == 'topic':
                            attrs = topic.attributes
                            if 'label' in attrs  and  'href' in attrs:
                                link = attrs['href'].nodeValue
                                # remove folder 'help' because index.html will be located there
                                link = link[5:]
                                links.append("<a href='" + link + "'>" + attrs['label'].nodeValue + "</a>")

            index[fName] = links

    return index


def printAndDelLinks(of, index, key):
    links = index[key]
    print('        <ul>', file=of)
    for link in links:
        print('        <li>', link, '</li>', file=of)
        
    print('        </ul>', file=of)
    del(index[key])
    

def index2File(fName, index):
    of = open(fName, 'wt')
    
    winIDEAVersion, verStr_, verMajor, verMinor, verBuild = isysUtils.getWinIDEAVersion()
    
    print('<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">', file = of)
    print('<html>', file = of)
    print('  <body>', file = of)
    print('    <h1>testIDEA Help</h1>', file = of)
    print('    &nbsp;&nbsp;&nbsp;&nbsp;<b>testIDEA version: ' + winIDEAVersion + '</b>', file = of)
    print('    <h2>Table of Contents</h2>', file = of)

    print('    <h3>Getting Started</h3>', file = of)
    printAndDelLinks(of, index, 'tocgettingstarted.xml')
    
    print('    <h3>Concepts</h3>', file = of)
    printAndDelLinks(of, index, 'tocconcepts.xml')
    print('      <h4>TestCase Editor</h4>', file = of)
    printAndDelLinks(of, index, 'tocTestSpec.xml')
    
    print('    <h3>Tasks</h3>', file = of)
    printAndDelLinks(of, index, 'toctasks.xml')

    if len(index) > 0:
        print('WARNING: Additional sections exist in help. They will be written to '
              'the end of TOC. Edit this script to place them to desired place if required!')
        
        for key in index:
            printAndDelLinks(of, index, key)
            
    print('  </body>', file = of)
    print('</html>', file = of)


if len(sys.argv) > 1:
    if sys.argv[1] == '-b':
        index = xml2Index()
        index2File('index.html', index)
        print("\n'index.html' created!\n")
    elif sys.argv[1] == '-d':
        # no longer deploy to FTP server, Simon will do it manually
        # __ftpHelpToWeb()
        copyHelpToDiskM()
    else:
        printHelp()
else:
    printHelp()
