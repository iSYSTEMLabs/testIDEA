# This script is intended for testing of winIDEA and testIDEA installation -
# they should at least start. To use it, create a clean virtual machine
# and install Python. Then copy this script to the virtual machine and
# create two shortcuts - one with parameter '2010' and another one with '2011'.
# Tester should execute both shortcuts and then select option 'Test | Launch testIDEA'
# in winIDEA.

import os
import sys
import shutil

if len(sys.argv) != 2:
    print 'Usage: setupTest.py <version>'
    print 'Where: version is 2010 or 2011'
    raw_input('Press ENTER to exit')
    sys.exit(-1)

setupFile = 'setup.exe'
version = sys.argv[1]
setupSource = 'Z:\\Install\\VENUS9_' + version[2:] + '\\Setup\\' + setupFile
installDir = 'C:\\winIDEA\\' + version
setupConfigFile = 'setup.config'

print 'Copying ' + setupSource + ' from winidea server...',
shutil.copyfile(setupSource, setupFile)
print('Done!')

# Explanation of variables used in setup.txt:
# MAINDIR - Location where you want to install winIDEA.
# BACKUP - Location where installation script stores previous versions of winIDEA files.
# GROUP - Name of the program group
# COMPONENTS - Which components will be installed. Possible values:
#   A - if COMPONENTS contains A then main winIDEA files will be installed (exe, dll,...)
#   B - if COMPONENTS contains B then GCC compiler for PPC will be installed
#   C - if COMPONENTS contains C then GCC compiler for ARM will be installed
#   D - if COMPONENTS contains D then GCC compiler for 68k will be installed
#   E - if COMPONENTS contains E then examples will be installed
#   F - if COMPONENTS contains F then python and isystem connect for python will be installed
#   G - if COMPONENTS contains G then SDKs will be installed
# ADDICONSTODESKTOP - if ADDICONSTODESKTOP contains A then program
# icons will be shown on the desktop
# PYTHONINSTALLDIR - Points to a folder where Python should be
# installed
# ICPYTHONINSTALLPARAMS - There are three command line options for
# iSYSTEM-connect for Python installer. First parameter represents
# major version, second minor version and the last parameter path to
# Python install folder.

cfg = open(setupConfigFile, 'w')
cfg.write('MAINDIR=' + installDir + '\n')
cfg.write('BACKUP=C:\\winIDEA\\' + version + '\\BACKUP' + '\n')
cfg.write(r'GROUP=winIDEA ' + version + '\n')
cfg.write(r'COMPONENTS=ABCDEFG' + '\n')
cfg.write(r'ADDICONSTODESKTOP=A' + '\n')
cfg.write(r'PYTHONINSTALLDIR=C:\Python26' + '\n')
cfg.write(r'ICPYTHONINSTALLPARAMS= 2 6 C:\Python26' + '\n')
cfg.close()

print 'Running silent install...',
os.system(setupFile + ' /s /m=setup.config')
print('Done!')
print 'Starting winIDEA...',
print("Start testIDEA with menu option 'Test | Launch testIDEA', then run all tests.")
os.system(installDir + r'\winIDEA.exe ' + installDir +
          '\SDK\iSYSTEM.Python.SDK\examples\winIDEA\SampleSimulator5554.xjrf')
print('Done!')

raw_input('Press ENTER to exit')

