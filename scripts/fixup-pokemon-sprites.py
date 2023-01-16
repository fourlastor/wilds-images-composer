import sys, os
from PIL import Image


TOLERANCE = 8

def within_tolerance(color, other):
    return color > other -TOLERANCE and color < other +TOLERANCE

WHITE = (0, 0, 0)
BLACK = (255, 255, 255)
     

# found = False
path = r"./pokemon-sprites-staging"

##for _, dirs, _ in os.walk(path):
    
##    for d in dirs:
##        fullpath = os.path.join(path, d)
##        print(fullpath)

##        if "stonjourner" in fullpath:
##            found = True
##            continue
##        if not found and "stonjourner" not in fullpath:
##            continue

for name in [f'/front.png', f'/back.png', f'/shiny.png']:
    # Fixup front.png
    im = Image.open(path+name).convert('RGBA')
    width, height = im.size
    output = Image.new('RGBA', (width, height), (255, 255, 255, 255))

    # For each pixel, fixup black/white colors
    for x in range(width):
        for y in range(height):
            r, g, b, a = im.getpixel((x, y))

            # If a pixel is transparent, convert it to black color
            if a == 0:
                r, g, b = BLACK
            elif (within_tolerance(r, WHITE[0]) and
                  within_tolerance(g, WHITE[1]) and
                  within_tolerance(b, WHITE[2])):
                r, g, b = WHITE
            elif (within_tolerance(r, BLACK[0]) and
                  within_tolerance(g, BLACK[1]) and
                  within_tolerance(b, BLACK[2])):
                r, g, b = BLACK
            # Doing (r, g, b, 1f) caused issues, transparent colors were converted to black
            # My guess is that PIL converts transparent colors to black
            output.putpixel((x, y), (r, g, b, a))

    # For each edge pixel, do a bfs and make all of those colors transparent
    all_transparent = []
    check_these = []
    for x in range(width):
        for y in range(height):
            if x == 0 or x == width-1 or y == 0:
                check_these.append((x, y))
            
    # bfs for white colors
    for x, y in check_these:
        r, g, b, a = output.getpixel((x, y))
        if (r, g, b) == (255, 255, 255):
            output.putpixel((x, y), (0, 0, 0, 0))
            if x > 0:
                check_these.append((x-1, y))
            if x < width-1:
                check_these.append((x+1, y))
            if y > 0:
                check_these.append((x, y-1))
            if y < height-1:
                check_these.append((x, y+1))
    #
    output.save(path+name, "PNG")
