"""
This file contains code, which makes it easier to write scripts,
which are run from testIDEA, and produce diagrams and charts to be
displayed in testIDEA and added to test reports.

Use this script when you want to greate graphs with graphwiz.
This open source tool is bundled with winIDEA, documentation
for its usage can be found at http://www.graphviz.org.

(c) iSYSTEM AG, 2015
"""

import sys
import os
import subprocess as sp
import _isys_diagutils


def createGraphwizFile(graphFileName):
    """
    Modify this function to generate graph according to your needs.
    See graphwiz documentation at

      http://www.graphviz.org

    for graph file syntax.
    """
    
    of = open(graphFileName, 'w')
    of.write("""
    digraph G {
      main -> initGlobals;
      main -> factorial;
      factorial -> factorial [label="recursive"];
    }
    """)

    of.close()


def main(cmdLineArgs):
    args = _isys_diagutils.parseArgs(cmdLineArgs, None)
    
    graphFileName = args.outFileName + '.dot'
    createGraphwizFile(graphFileName)

    imgFormat = _isys_diagutils.getImageTypeFromExtension(args.outFileName)
    dotExe = os.path.join(args.dotDir, 'dot.exe')
    cmd = dotExe + ' -T' + imgFormat + ' -o' + args.outFileName + ' ' + graphFileName
    try:
        sp.check_call(cmd)
    except Exception as ex:
        print("\nERROR: Can not run graphviz 'dot' utility.", file=sys.stderr)
        print("    Please make sure it is installed.", file=sys.stderr)
        print("    See help (option -h) for instructions.", file=sys.stderr)
        print("    Command: ", cmd, '\n\n', file=sys.stderr)
        raise


if __name__ == '__main__':
    main(sys.argv[1:])
