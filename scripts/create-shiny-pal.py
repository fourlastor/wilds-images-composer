import os
import json
from PIL import Image
import math

# Replace with the path to the shiny.png file. Example: shiny.png (if shiny.png is in the same directory as this script)
path = "pokemon-sprites-staging/shiny.png"

# Load front sprite and infer the palette data
im = Image.open(path)
width, height = im.size
shiny_color1 = None
shiny_color2 = None

for x in range(width):
    for y in range(height):
        r,g,b,a = im.getpixel((x,y))
        if ((r,g,b,a) == (0,0,0,255) or
            (r,g,b,a) == (255,255,255,255) or
            (r,g,b,a) == (0,0,0,0) or
            (r,g,b,a) == (255,255,255,0)):
            continue
        if (r,g,b,a) != shiny_color1 and (r,g,b,a) != shiny_color2:
            print((r,g,b,a))
            if shiny_color1 is None:
                shiny_color1 = (r,g,b,a)
            else:
                shiny_color2 = (r,g,b,a)
                break

if sum(shiny_color1) < sum(shiny_color2):
    temp = shiny_color2
    shiny_color2 = shiny_color1
    shiny_color1 = temp

lines = []
lines.append("")
#	RGB 32, 32, 19
#	RGB 27, 13, 25
lines.append("\tRGB " + '{0:02d}'.format(math.floor((shiny_color1[0])/8)) + ", " + '{0:02d}'.format(math.floor((shiny_color1[1])/8)) + ", " + '{0:02d}'.format(math.floor((shiny_color1[2])/8)))
lines.append("\tRGB " + '{0:02d}'.format(math.floor((shiny_color2[0])/8)) + ", " + '{0:02d}'.format(math.floor((shiny_color2[1])/8)) + ", " + '{0:02d}'.format(math.floor((shiny_color2[2])/8)))
lines.append("")


print("File contents:")
print("\n".join(lines))
with open("pokemon-sprites-staging/shiny.pal", 'w') as f:
    f.write("\n".join(lines))


print("<3 Thank you for helping me out! - SheerSt")






