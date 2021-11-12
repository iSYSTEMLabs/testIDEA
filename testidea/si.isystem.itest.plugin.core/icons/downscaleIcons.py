#!python

# This script creates images of program icon of sizes needed

# You need to:
#
# - open 'itest_window.xcf' in GIMP and do any required modifications.
#
# - save the image as 'itest_window_455.gif' and run this script to
#   generate all other images.

import os

print '128'
os.system("convert itest_window_455.gif -resize 128x128 itest_window_128.gif")
print '64'
os.system("convert itest_window_455.gif -resize 64x64 itest_window_64.gif")
print '48'
os.system("convert itest_window_455.gif -resize 48x48 itest_window_48.gif")
print '32'
os.system("convert itest_window_455.gif -resize 32x32 itest_window_32.gif")
print '16'
os.system("convert itest_window_455.gif -resize 16x16 itest_window_16.gif")

print 'test'

os.system("convert itest_window_455.gif -resize 128x128 -fill \"#004c80\" -draw \"line 0,0 0,127\" " + 
          "itest_window_128_w_border.gif")

print '128 w border'
os.system("convert itest_window_455.gif -resize 128x128 -fill \"#004c80\" -draw \"line 0,0 0,127\" " +
          "-draw \"line 0,127 127,127\" -draw \"line 127,127 127,0\" -draw \"line 127,0 0,0\" " +
          "itest_window_128_w_border.gif")

print '32 w border'
os.system("convert itest_window_455.gif -resize 32x32 -fill \"#004c80\" -draw \"line 0,0 0,31\" " +
          "-draw \"line 0,31 31,31\" -draw \"line 31,31 31,0\" -draw \"line 31,0 0,0\" " +
          "itest_window-w-border_32.gif")
