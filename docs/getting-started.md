# Getting started

## Setup

1. Fork and clone this repository.
2. Download [IntellijIDEA](https://www.jetbrains.com/idea/) (community edition is great), you can also use [Toolbox](https://www.jetbrains.com/toolbox-app/) for it.
3. Open the repository from Intellij: ![Screenshot from 2023-01-17 08-49-15](https://user-images.githubusercontent.com/1263058/212852539-02e4ee80-7422-4c9f-a5ce-9258e7b1e369.png)
4. Suggest changes in [pull requests](https://github.blog/2015-01-21-how-to-write-the-perfect-pull-request/)

##  Image conversion

The java code is in `image-converter/src/main/java`

There are 3 scripts in the `scripts` folder:

### `create-shiny-pal.py`

Takes in input a PNG shiny image, which has always 2 colors, plus possibly black and white.

Extracts the two colors, ordering them from highest to lowest brightness, where "brigthness" is defined as the sum of the color's components.

### `fixup-pokemon-sprites.py`

Takes in input a PNG image

Makes all pixels which are within `TOLERANCE` of black/white, black and white respectively, keeping their alpha.

Example: if black is `(0, 0, 0)` and white is `(255, 255, 255)`, and `TOLERANCE` is 8, and we have two colors:

1. `(2, 5, 6)` => becomes black as all its channels are within the tolerance level
2. `(255, 249, 245)` => stays as is because the blue component (245) is out of the tolerance window for white (247)

Takes all the white pixels at the edges, and for each of these checks its white neighbours (recursively) until all of them are transparent. See also https://en.wikipedia.org/wiki/Breadth-first_search

### `gif-to-anim-asm.py`

Takes in input a GIF image

Saves the timing info

Splits the frames in separate images (the python script then joins them together, this should be skipped)
