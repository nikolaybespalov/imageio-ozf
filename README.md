[![Build Status](https://travis-ci.org/nikolaybespalov/imageio-ozf.svg?branch=master)](https://travis-ci.org/nikolaybespalov/imageio-ozf)

# imageio-ozf
ImageIO plugin that allows you to use OziExplorer image files(ozf2/ozf3/ozf4) without using GDAL/OziApi or any other environment dependencies.

It's as easy as reading any other image file

    BufferedImage ozfImage = ImageIO.read(new File("image.ozf3"));
    
Just add dependency to your _pom.xml_
```xml
    <dependency>
        <groupId>com.github.nikolaybespalov</groupId>
        <artifactId>imageio-ozf</artifactId>
        <version>${imageio.ozf.version}</version>
        <scope>runtime</scope>
    </dependency>
```
Or to your _build.gradle_
```js
    dependencies {
        runtime("com.github.nikolaybespalov:imageio-ozf:{imageio.ozf.version}")
    }
```
And your project will be able to work with .ozf files!
