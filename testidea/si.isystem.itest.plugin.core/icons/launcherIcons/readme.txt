BMP files in this folder are used by Eclipse build process.

Entering BMP files into Eclipse 'Launching' page of product
configuration works better in Eclipse 4 than ICO file.
Fix: Eclipse does not recognize 24- bit BMPs, you must export 32-bit BMPs
in GIMP, and under compatibility options 'Do not write colorspace information'.
See stackoverflow http://stackoverflow.com/questions/11470028/splash-screen-does-not-show-up-when-product-export.

Marko, 4.8.2010