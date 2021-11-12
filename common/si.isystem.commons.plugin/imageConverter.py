
from PIL import ImageFile, BmpImagePlugin

_i16, _i32 = BmpImagePlugin.i16, BmpImagePlugin.i32


#
# CLI command used to convert 16x16 images and add a 1 pixel transparent border to them
# for file in src/*; do convert  $file   -bordercolor transparent   -compose Copy   -border 1   dst/`basename $file`; done
#

class BmpAlphaImage(ImageFile.ImageFile):
    format = "32bit-BMP"
    format_description = "BMP with Alpha channel"

    def _open(self):
        s = self.fp.read(14)
        offset = _i32(s[10:])
        self._read_bitmap(offset)

    def _read_bitmap(self, offset):
        s = self.fp.read(4)
        s += ImageFile._safe_read(self.fp, _i32(s) - 4)

        # Allows BMP v3, v4 and v5
        if len(s) not in (40, 108, 124):
            raise IOError("BMP header type unsupported (%d)" % len(s))

        # Only accept BMP with alpha.
        bpp = _i16(s[14:])
        if bpp != 32:
            raise IOError("BMP pixel depth unsupported (%d)" % bpp)

        compress = _i32(s[16:])
        if compress == 3:
            # BI_BITFIELDS compression
            mask = (_i32(self.fp.read(4)), _i32(self.fp.read(4)),
                    _i32(self.fp.read(4)), _i32(self.fp.read(4)))
        elif compress != 0:
            # Only accept uncompressed BMP.
            raise IOError("Unsupported BMP compression (%d)" % compress)

        self.mode, rawmode = 'RGBA', 'BGRA'

        self.size = (_i32(s[4:]), _i32(s[8:]))
        direction = -1
        if s[11] == '\xff':
            # upside-down storage
            self.size = self.size[0], 2**32 - self.size[1]
            direction = 0

        self.info["compress"] = compress

        # data descriptor
        self.tile = [("raw", (0, 0) + self.size, offset, (rawmode, 0, direction))]
        
BMP_EXTENSION = ".bmp"
PNG_EXTENSION = ".png"
        
def convertBmp2Png(inPath):
    # Try to open as 32bit BMP with our converter
    try:
        return BmpAlphaImage(inPath)
    # If we fail just try with the regular image converter
    except:
        return convert2Png(inPath)

def convert2Png(inPath):
    from PIL import Image
    return Image.open(inPath)

# Only resizes images where size is between 17 and 31 and width == height
def resizeTo16x16(img):
    w, h = img.size
    xOff = 0
    yOff = 0
    
    if w != h or w <= 16 or w >= 32 or h <= 16 or h >= 32:
        return img
    
    if w > 16:
        xOff = (w-16)//2
    if h > 16:
        yOff = (h-16)//2
        
    if xOff > 0  or  yOff > 0:
        return img.crop((xOff, yOff, xOff+16, yOff+16))
    else:
        return img

if __name__ == '__main__':
    convertBmp2Png("c:\\_tmp\\analyzer-32.bmp", "c:\\_tmp\\analyzer-32.png")
    

