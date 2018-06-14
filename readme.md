[![Build Status](https://travis-ci.org/dkandalov/slides-presenter.svg?branch=master)](https://travis-ci.org/dkandalov/slides-presenter)

## Slides presenter plugin
This is a plugin to show slides and code examples directly from IntelliJ IDEs.

## Why?
It can be useful if large part of your presentation is based on code examples, so instead of pasting code into Keynote or PowerPoint slides you can show both slides and code in IDE. 
 
 - Code examples stay up-to-date. 
 - You can run and modify code examples live to show how they work.
 - Smooth transition between slides and code examples.


## How to use?

1. Create `slides.txt` in the project with code examples.
2. Add slides as images to the project folder (e.g. in Keynote in the main menu `File -> Export To -> Images...`).
3. Edit `slides.txt` so that it contains paths to slides image files and code examples (see format description below). Note that you can use `Save All` action to make plugin reload `slides.txt`.
4. (Optional) `View -> Enter Presentation Mode`
5. Use `Next Slide` and `Previous Slide` actions. It's recommended to change default shortcuts to something more convenient, e.g. `F12`, `F11` (the only reason for defaults shortcuts `alt-shift-F12`, `alt-shift-F11` is that they don't conflict with other actions).
 
 
## Format of slides.txt
- Empty lines and lines starting with `--` or `#` are ignored.
- Each line contains a path to a file with unix-style path separator `/` which can be an absolute path or path relative to the project root.  
- Paths can include special syntax `{{next N}}` where `N` is the amount of times the line will be repeated with incremented counter. E.g. `slides/slides.{{next 2}}.png` will be expanded into two lines `slides/slides.000.png` and `slides/slides.001.png`.

For example:
```
# Some comment
/absolute/path/intro.png

-- More slides
slides/slides.{{next 2}}.png
src/code.js
src/more-code.js
slides/slides.{{next 2}}.png
```

For real-world example see [slides.txt](https://github.com/dkandalov/coroutines-explained/blob/b958731f5acd94a8cf3a75e5bcc5578afc7a7547/slides.txt) in `coroutines-explained` repository.