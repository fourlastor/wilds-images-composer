import sys, os
from PIL import Image


path = './pokemon-sprites-staging/'
filename = 'front.gif'

gif = Image.open(path + filename)

# Make a new image based on gif dimensions
frames = []
frame_durations = []

for frame_index in range(gif.n_frames):
    gif.seek(frame_index)
    frames.append(gif.convert("RGBA"))

    # Convert from milliseconds to number of in-game frames using
    # the formula n_frames = (duration_millis / 1000) * 60
    frame_durations.append((gif.info['duration'] / 1000.0) * 60)

lines = ['	frame {0}, {1:02d}'.format(i, int(d)) for i, d in enumerate(frame_durations)]
lines.append('	endanim')

with open(path + 'anim.asm', 'w') as f:
    # No carriage-return for safety (thats the way it is in the decompile)
    f.write('\n'.join(lines))

width, height = frames[0].size

# Spritesheet image
output = Image.new('RGBA', (width, height * len(frames)), (255, 255, 255, 255))

for i, frame in enumerate(frames):
    output.paste(frame, (0, i * height))

output.save(path + 'front.png', "png")
