# imageio-ozf
ImageIO plugin that allows you to use OziExplorer image files(ozf2/ozf3/ozf4) without using GDAL/OziApi or any other environment dependencies.

    BufferedImage image = ImageIO.read(new File("world.ozf3"));