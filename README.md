GPX Video Generator
===================

This tool takes GPX files and generates images showing a progress of the single
tracks. The images can be combined into a video using 3rd party tools to generate
results like this:

![Animation](docs/map.gif)


Build
=====

Run `./gradlew fatJar` to generate a jar file including all required dependencies.


Usage
=====

First define your video specs in a [config file](src/test/resources/test-small.yml), then launch the
gpx2video creator to generate the images that can then be converted into a video:

```
java -jar build/libs/gpx2video-0.1.jar  src/test/resources/test-small.yml
```

**Notice:** the sample config files invoke [ffmpeg](https://ffmpeg.org/) to generate a video.


Dependencies
============

* [Wadlbeißer App](https://pfattner.de/wadlbeisser/): code partly re-used from this app
* [MapsForge](https://github.com/mapsforge/mapsforge) used to generate maps. Some Android stub classes are required
  because the Android libs are used
* [kaml](https://github.com/charleskorn/kaml) to parse config files
* [kxml2](https://github.com/kobjects/kxml2) for parsing GPX files
