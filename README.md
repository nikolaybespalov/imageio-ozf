# imageio-ozf
ImageIO plugin that allows you to use OziExplorer image files(ozf2/ozf3/ozf4) without using GDAL/OziApi or any other environment dependencies.

It's just like reading any other image file

    BufferedImage ozfImage = ImageIO.read(new File("image.ozf3"));
