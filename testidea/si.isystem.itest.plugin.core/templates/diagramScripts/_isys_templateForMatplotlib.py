"""
This file contains code, which makes it easier to write scripts,
which are run from testIDEA, and produce diagrams and charts to be
displayed in testIDEA and added to test reports.

Use this script when you want to greate figures with Python's module
matplotlib. This open source module is bundled with winIDEA Python
distribution, documentation for its usage can be found
at http://matplotlib.org/.

(c) iSYSTEM AG, 2015
"""

import numpy as np
import matplotlib.pyplot as plt
import os
import sys
import _isys_diagutils

def main(cmdLineArgs):
    """
    Modify this function to draw your image.
    """

    args = _isys_diagutils.parseArgs(cmdLineArgs, None)
    imgFormat = _isys_diagutils.getImageTypeFromExtension(args.outFileName)

    # sample code taken from matplotlib examples
    x = np.linspace(0.0, 5.0, 100)
    y = np.cos(2 * np.pi * x) * np.exp(-x)

    plt.plot(x, y, 'k')
    plt.title('Damped exponential decay')
    plt.text(2, 0.65, r'$\cos(2 \pi t) \exp(-t)$')
    plt.xlabel('time (s)')
    plt.ylabel('voltage (mV)')

    # Tweak spacing to prevent clipping of ylabel
    plt.subplots_adjust(left=0.15)

    # Save figure, which will be shown in testIDEA
    plt.savefig(args.outFileName, format=imgFormat)

    # Uncomment the next line to show figure from matplotlib.
    # This script will not return until the figure window is closed.
    # It is recommended to use diagramType 'custom-async' in such case.
    # plt.show()

if __name__ == '__main__':
    main(sys.argv[1:])
