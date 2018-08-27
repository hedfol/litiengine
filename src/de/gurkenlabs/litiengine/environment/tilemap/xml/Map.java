
package de.gurkenlabs.litiengine.environment.tilemap.xml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import de.gurkenlabs.litiengine.environment.tilemap.IImageLayer;
import de.gurkenlabs.litiengine.environment.tilemap.ILayer;
import de.gurkenlabs.litiengine.environment.tilemap.IMap;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObjectLayer;
import de.gurkenlabs.litiengine.environment.tilemap.ITileLayer;
import de.gurkenlabs.litiengine.environment.tilemap.ITileset;
import de.gurkenlabs.litiengine.environment.tilemap.MapOrientation;
import de.gurkenlabs.litiengine.environment.tilemap.MapUtilities;
import de.gurkenlabs.litiengine.environment.tilemap.StaggerAxis;
import de.gurkenlabs.litiengine.environment.tilemap.StaggerIndex;
import de.gurkenlabs.litiengine.util.ColorHelper;
import de.gurkenlabs.litiengine.util.MathUtilities;
import de.gurkenlabs.litiengine.util.geom.GeometricUtilities;
import de.gurkenlabs.litiengine.util.io.FileUtilities;

@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.FIELD)
public final class Map extends CustomPropertyProvider implements IMap, Serializable, Comparable<Map> {
  public static final String FILE_EXTENSION = "tmx";
  private static final long serialVersionUID = 402776584608365440L;
  private static final int[] MAX_SUPPORTED_VERSION = { 1, 1, 5 }; // 1.1.5

  @XmlAttribute
  private double version;

  @XmlAttribute
  private String tiledversion;

  @XmlAttribute
  private String orientation;

  @XmlTransient
  private MapOrientation mapOrientation = MapOrientation.UNDEFINED;

  @XmlAttribute
  private String renderorder;

  @XmlAttribute
  private int width;

  @XmlAttribute
  private int height;

  @XmlAttribute
  private int tilewidth;

  @XmlAttribute
  private int tileheight;

  @XmlAttribute
  private int hexsidelength;

  @XmlTransient
  private StaggerAxis staggerAxisEnum = StaggerAxis.UNDEFINED;

  @XmlTransient
  private StaggerIndex staggerIndexEnum = StaggerIndex.UNDEFINED;


  @XmlAttribute
  private String staggeraxis;

  @XmlAttribute
  private String staggerindex;

  @XmlAttribute
  private String backgroundcolor;

  @XmlAttribute(name = "nextobjectid")
  private int nextObjectId;

  @XmlAttribute(required = false)
  private String name;

  @XmlElement(name = "tileset")
  private List<Tileset> rawTilesets;

  @XmlElement(name = "imagelayer")
  private List<ImageLayer> rawImageLayers;

  @XmlElement(name = "layer")
  private List<TileLayer> rawTileLayers;

  @XmlElement(name = "objectgroup")
  private List<MapObjectLayer> rawMapObjectLayers;

  @XmlTransient
  private String path;

  private transient List<ITileset> tilesets;
  private transient List<ITileLayer> tileLayers;
  private transient List<IMapObjectLayer> mapObjectLayers;
  private transient List<IImageLayer> imageLayers;
  private transient List<ILayer> allRenderLayers;

  private transient Color decodedBackgroundColor;

  @Override
  public List<IImageLayer> getImageLayers() {
    return this.imageLayers;
  }

  /**
   * Gets the next object id.
   *
   * @return the next object id
   */
  @Override
  public int getNextObjectId() {
    return this.nextObjectId;
  }

  @Override
  public MapOrientation getOrientation() {
    if (this.mapOrientation == MapOrientation.UNDEFINED) {
      this.mapOrientation = MapOrientation.valueOf(this.orientation.toUpperCase());
    }
    return this.mapOrientation;
  }

  @Override
  @XmlTransient
  public String getPath() {
    return this.path;
  }

  @Override
  public String getRenderOrder() {
    return this.renderorder;
  }

  @Override
  public List<IMapObjectLayer> getMapObjectLayers() {
    if (this.mapObjectLayers == null) {
      ArrayList<IMapObjectLayer> tmpMapObjectLayers = new ArrayList<>();
      if (this.rawMapObjectLayers != null) {
        tmpMapObjectLayers.addAll(this.rawMapObjectLayers);
      }

      this.mapObjectLayers = Collections.unmodifiableList(tmpMapObjectLayers);
    }

    return this.mapObjectLayers;
  }

  @Override
  public IMapObjectLayer getMapObjectLayer(IMapObject mapObject) {
    for (IMapObjectLayer layer : this.getMapObjectLayers()) {
      Optional<IMapObject> found = layer.getMapObjects().stream().filter(x -> x.getId() == mapObject.getId()).findFirst();
      if (found.isPresent()) {
        return layer;
      }
    }

    return null;
  }

  @Override
  public void removeMapObject(int mapId) {
    for (IMapObjectLayer layer : this.getMapObjectLayers()) {
      IMapObject remove = null;
      for (IMapObject obj : layer.getMapObjects()) {
        if (obj.getId() == mapId) {
          remove = obj;
          break;
        }
      }

      if (remove != null) {
        layer.removeMapObject(remove);
        break;
      }
    }
  }

  @Override
  public Dimension getSizeInPixels() {
    Dimension mapSizeInPixels = new Dimension(this.width * this.tilewidth, this.height * this.tileheight);
    switch (this.getOrientation()) {
    case HEXAGONAL:
      int maxX = (int) Math.max(this.getTileGrid()[this.getWidth() - 1][0].getBounds2D().getMaxX(), this.getTileGrid()[this.getWidth() - 1][1].getBounds2D().getMaxX());
      int maxY = (int) Math.max(this.getTileGrid()[0][this.getHeight() - 1].getBounds2D().getMaxY(), this.getTileGrid()[1][this.getHeight() - 1].getBounds2D().getMaxY());
      mapSizeInPixels.setSize(maxX, maxY);
      break;
    case ISOMETRIC:
      break;
    case ORTHOGONAL:
      break;
    case SHIFTED:
      break;
    case STAGGERED:
      break;
    case UNDEFINED:
      break;
    }
    return mapSizeInPixels;
  }

  @XmlTransient
  @Override
  public Rectangle2D getBounds() {
    return new Rectangle(this.getSizeInPixels());
  }

  @Override
  public Dimension getSizeInTiles() {
    return new Dimension(this.width, this.height);
  }

  @Override
  public List<ITileLayer> getTileLayers() {
    return this.tileLayers;
  }

  @Override
  public List<ITileset> getTilesets() {
    return this.tilesets;
  }

  @Override
  public Dimension getTileSize() {
    return new Dimension(this.tilewidth, this.tileheight);
  }

  @Override
  public int getTileWidth() {
    return this.tilewidth;
  }

  @Override
  public int getTileHeight() {
    return this.tileheight;
  }

  @XmlTransient
  public Shape[][] getTileGrid() {
    Shape[][] grid = new Shape[this.getSizeInTiles().width][this.getSizeInTiles().height];
    switch (this.getOrientation()) {
    case HEXAGONAL:
      for (int x = 0; x < this.getSizeInTiles().width; x++) {
        for (int y = 0; y < this.getSizeInTiles().height; y++) {
          final StaggerAxis staggerAxis = this.getStaggerAxis();
          final StaggerIndex staggerIndex = this.getStaggerIndex();
          final int s = this.getHexSideLength();
          final int h = staggerAxis == StaggerAxis.X ? this.getTileHeight() : this.getTileWidth();
          final int r = h / 2;
          final int t = staggerAxis == StaggerAxis.X ? (this.getTileWidth() - s) / 2 : (this.getTileHeight() - s) / 2;
          int widthStaggerFactor = 0;
          int heightStaggerFactor = 0;
          if (staggerAxis == StaggerAxis.X) {
            if ((staggerIndex == StaggerIndex.ODD && MathUtilities.isOddNumber(x)) || (staggerIndex == StaggerIndex.EVEN && !MathUtilities.isOddNumber(x))) {
              heightStaggerFactor = r;
            }
            grid[x][y] = GeometricUtilities.getHex(widthStaggerFactor + x * (t + s), heightStaggerFactor + y * h, staggerAxis, s, r, t);
          }
          if (staggerAxis == StaggerAxis.Y) {
            if ((staggerIndex == StaggerIndex.ODD && MathUtilities.isOddNumber(y)) || (staggerIndex == StaggerIndex.EVEN && !MathUtilities.isOddNumber(y))) {
              widthStaggerFactor = r;
            }
            grid[x][y] = GeometricUtilities.getHex(widthStaggerFactor + x * h, heightStaggerFactor + y * (t + s), staggerAxis, s, r, t);
          }
        }
      }
      break;
    case ISOMETRIC:
      break;
    case ORTHOGONAL:
      for (int x = 0; x < this.getSizeInTiles().width; x++) {
        for (int y = 0; y < this.getSizeInTiles().height; y++) {
          grid[x][y] = new Rectangle(x * this.getTileWidth(), y * this.getTileHeight(), this.getTileWidth(), this.getTileHeight());
        }
      }
      break;
    case SHIFTED:
      break;
    case STAGGERED:
      break;
    case UNDEFINED:
      break;
    default:
      break;
    }
    return grid;
  }

  @Override
  public double getVersion() {
    return this.version;
  }

  @Override
  public String getTiledVersion() {
    return this.tiledversion;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public Collection<IMapObject> getMapObjects(String... types) {
    List<IMapObject> mapObjects = new ArrayList<>();
    if (this.getMapObjectLayers() == null || this.getMapObjectLayers().isEmpty() || types.length == 0) {
      return mapObjects;
    }

    for (IMapObjectLayer layer : this.getMapObjectLayers()) {
      if (layer == null) {
        continue;
      }

      mapObjects.addAll(layer.getMapObjects(types));
    }

    return mapObjects;
  }

  @Override
  public Collection<IMapObject> getMapObjects() {
    List<IMapObject> mapObjects = new ArrayList<>();
    if (this.getMapObjectLayers() == null || this.getMapObjectLayers().isEmpty()) {
      return mapObjects;
    }

    for (IMapObjectLayer layer : this.getMapObjectLayers()) {
      if (layer == null) {
        continue;
      }

      for (IMapObject mapObject : layer.getMapObjects()) {
        if (mapObject != null) {
          mapObjects.add(mapObject);
        }
      }
    }

    return mapObjects;
  }

  @Override
  public IMapObject getMapObject(int mapId) {
    if (this.getMapObjectLayers() == null || this.getMapObjectLayers().isEmpty()) {
      return null;
    }

    for (IMapObjectLayer layer : this.getMapObjectLayers()) {
      if (layer == null) {
        continue;
      }

      for (IMapObject mapObject : layer.getMapObjects()) {
        if (mapObject != null && mapObject.getId() == mapId) {
          return mapObject;
        }
      }
    }

    return null;
  }

  @Override
  public int getWidth() {
    return this.width;
  }

  @Override
  public int getHeight() {
    return this.height;
  }

  @Override
  public int getHexSideLength() {
    return this.hexsidelength;
  }

  @Override
  public StaggerAxis getStaggerAxis() {
    if (this.staggerAxisEnum == StaggerAxis.UNDEFINED) {
      this.staggerAxisEnum = StaggerAxis.valueOf(this.staggeraxis.toUpperCase());
    }
    return this.staggerAxisEnum;
  }

  @Override
  public StaggerIndex getStaggerIndex() {
    if (this.staggerIndexEnum == StaggerIndex.UNDEFINED) {
      this.staggerIndexEnum = StaggerIndex.valueOf(this.staggerindex.toUpperCase());
    }
    return this.staggerIndexEnum;
  }

  public void setPath(final String path) {
    this.path = path;
    if (this.rawImageLayers != null && !this.rawImageLayers.isEmpty()) {
      for (final ImageLayer imgLayer : this.rawImageLayers) {
        if (imgLayer == null) {
          continue;
        }

        imgLayer.setMapPath(path);
      }
    }

    if (this.rawTilesets != null && !this.rawTilesets.isEmpty()) {
      for (final Tileset tileSet : this.rawTilesets) {
        if (tileSet == null) {
          continue;
        }

        tileSet.setMapPath(FileUtilities.getParentDirPath(path));
      }
    }
  }

  public void updateTileTerrain() {
    for (TileLayer layer : this.rawTileLayers) {
      for (Tile tile : layer.getData()) {
        tile.setTerrains(MapUtilities.getTerrain(this, tile.getGridId()));
      }
    }
  }

  @Override
  public void addMapObjectLayer(IMapObjectLayer layer) {
    this.getRawMapObjectLayers().add((MapObjectLayer) layer);
    this.mapObjectLayers = null;
  }

  @Override
  public void addMapObjectLayer(int index, IMapObjectLayer layer) {
    this.getRawMapObjectLayers().add(index, (MapObjectLayer) layer);
    this.mapObjectLayers = null;
  }

  @Override
  public void removeMapObjectLayer(IMapObjectLayer layer) {
    this.getRawMapObjectLayers().remove(layer);
    this.mapObjectLayers = null;
  }

  @Override
  public void removeMapObjectLayer(int index) {
    this.getRawMapObjectLayers().remove(index);
    this.mapObjectLayers = null;
  }

  @XmlTransient
  public void setHeight(int height) {
    this.height = height;
  }

  @XmlTransient
  public void setOrientation(String orientation) {
    this.orientation = orientation;
  }

  @XmlTransient
  public void setRenderorder(String renderorder) {
    this.renderorder = renderorder;
  }

  @XmlTransient
  public void setTiledVersion(String tiledversion) {
    this.tiledversion = tiledversion;
  }

  @XmlTransient
  public void setTileHeight(int tileheight) {
    this.tileheight = tileheight;
  }

  @XmlTransient
  public void setTileWidth(int tilewidth) {
    this.tilewidth = tilewidth;
  }

  @XmlTransient
  public void setHexSideLength(int hexSideLength) {
    this.hexsidelength = hexSideLength;
  }

  @XmlTransient
  public void setStaggerAxis(String staggerAxis) {
    this.staggeraxis = staggerAxis;
  }

  @XmlTransient
  public void setStaggerIndex(String staggerIndex) {
    this.staggerindex = staggerIndex;
  }

  @XmlTransient
  public void setVersion(double version) {
    this.version = version;
  }

  @XmlTransient
  public void setWidth(int width) {
    this.width = width;
  }

  @Override
  public int compareTo(Map o) {
    if (this.name == null || o.name == null) {
      System.err.println("A map file couldn't be processed due to the name attribute not being present!");
    }
    return this.name.compareTo(o.name);
  }

  @Override
  public List<ILayer> getRenderLayers() {
    return this.allRenderLayers;
  }

  public List<Tileset> getExternalTilesets() {
    List<Tileset> externalTilesets = new ArrayList<>();
    for (Tileset set : this.getRawTilesets()) {
      if (set.sourceTileset != null) {
        externalTilesets.add(set.sourceTileset);
      }
    }

    return externalTilesets;
  }

  public List<Tileset> getRawTilesets() {
    if (this.rawTilesets == null) {
      this.rawTilesets = new ArrayList<>();
    }

    return this.rawTilesets;
  }

  @Override
  public Color getBackgroundColor() {
    if (this.backgroundcolor == null || this.backgroundcolor.isEmpty()) {
      return null;
    }

    if (this.decodedBackgroundColor != null) {
      return this.decodedBackgroundColor;
    }

    this.decodedBackgroundColor = ColorHelper.decode(this.backgroundcolor, true);
    return this.decodedBackgroundColor;
  }

  public List<TileLayer> getRawTileLayers() {
    if (this.rawTileLayers == null) {
      this.rawTileLayers = new ArrayList<>();
    }

    return this.rawTileLayers;
  }

  protected List<ImageLayer> getRawImageLayers() {
    if (this.rawImageLayers == null) {
      this.rawImageLayers = new ArrayList<>();
    }

    return this.rawImageLayers;
  }

  protected List<MapObjectLayer> getRawMapObjectLayers() {
    if (this.rawMapObjectLayers == null) {
      this.rawMapObjectLayers = new ArrayList<>();
    }

    return this.rawMapObjectLayers;
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller u, Object parent) {
    String[] ver = this.tiledversion.split("\\.");
    int[] vNumbers = new int[ver.length];
    try {
      for (int i = 0; i < ver.length; i++) {
        vNumbers[i] = Integer.parseInt(ver[i]);
      }
    } catch (NumberFormatException e) {
      throw new UnsupportedOperationException("unsupported Tiled version: " + tiledversion, e);
    }
    for (int i = 0; i < Math.min(vNumbers.length, MAX_SUPPORTED_VERSION.length); i++) {
      if (vNumbers[i] > MAX_SUPPORTED_VERSION[i]) {
        throw new UnsupportedOperationException("unsupported Tiled version: " + tiledversion);
      } else if (vNumbers[i] < MAX_SUPPORTED_VERSION[i]) {
        break;
      }
    }

    ArrayList<ITileset> tmpSets = new ArrayList<>();
    if (this.rawTilesets != null) {
      tmpSets.addAll(this.rawTilesets);
    }

    ArrayList<ITileLayer> tmpTileLayers = new ArrayList<>();
    if (this.rawTileLayers != null) {
      tmpTileLayers.addAll(this.rawTileLayers);
    }

    ArrayList<IMapObjectLayer> tmpMapObjectLayers = new ArrayList<>();
    if (this.rawMapObjectLayers != null) {
      tmpMapObjectLayers.addAll(this.rawMapObjectLayers);
    }

    ArrayList<IImageLayer> tmpImageLayers = new ArrayList<>();
    if (this.rawImageLayers != null) {
      tmpImageLayers.addAll(this.rawImageLayers);
    }

    ArrayList<ILayer> tmprenderLayers = new ArrayList<>();
    tmprenderLayers.addAll(tmpTileLayers);
    tmprenderLayers.addAll(tmpImageLayers);
    tmprenderLayers.sort(Comparator.comparing(ILayer::getOrder));

    this.tilesets = Collections.unmodifiableList(tmpSets);
    this.tileLayers = Collections.unmodifiableList(tmpTileLayers);
    this.mapObjectLayers = Collections.unmodifiableList(tmpMapObjectLayers);
    this.imageLayers = Collections.unmodifiableList(tmpImageLayers);
    this.allRenderLayers = Collections.unmodifiableList(tmprenderLayers);

  }
}