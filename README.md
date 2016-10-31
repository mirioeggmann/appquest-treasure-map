# AppQuest Treasure Map

![HSR](http://appquest.hsr.ch/images/fho.png)

## About

AppQuest Treasure Map is the 3. application for the [App Quest 2016](http://appquest.hsr.ch/) Treasure Hunt. The application must be able to display OpenStreetMaps and save coordinates of some locations. At the Treasure Hunt the goal will be to save coordinates of multiple posts.

### General
|   |  |
|---|---|
| Application Requirements | http://appquest.hsr.ch/2016/schatzkarte |
| Minimum API Level | [API level 23 (Marshmallow)](https://developer.android.com/about/versions/marshmallow/android-6.0.html) |
| Development Environment | [Android Studio](https://developer.android.com/studio/index.html) |

### Example
![AppQuest Treasure Map](http://appquest.hsr.ch/2016/wp-content/uploads/IMG_0420.png)

### Links
- https://github.com/osmdroid/osmdroid
- https://giswiki.hsr.ch/OpenStreetMap
- https://github.com/osmdroid/osmdroid/wiki/How-to-add-the-osmdroid-library-via-Gradle
- https://code.google.com/archive/p/osmdroid/wikis/Prerequisites.wiki
- http://appquest.hsr.ch/hsr.mbtiles
- https://code.google.com/archive/p/osmdroid/wikis/HowToIncludeInYourProject.wiki
- http://www.vogella.com/tutorials/AndroidLocationAPI/article.html
- https://android-coding.blogspot.ch/2012/06/example-of-implementing-openstreetmap.html
- https://github.com/osmdroid/osmdroid/blob/master/OpenStreetMapViewer/src/main/java/org/osmdroid/samples/SampleWithMinimapItemizedoverlay.java

### Given code snippets
[osm.java](https://gist.github.com/misto/7114790#file-osm-java)
```java
map = (MapView) findViewById(R.id.map /*eure ID der Map View */);
map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

map.setMultiTouchControls(true);
map.setBuiltInZoomControls(true);

IMapController controller = map.getController();
controller.setZoom(18);

// Die TileSource beschreibt die Eigenschaften der Kacheln die wir anzeigen
XYTileSource treasureMapTileSource = new XYTileSource("mbtiles", 1, 20, 256, ".png", "http://example.org/");

File file = new File(Environment.getExternalStorageDirectory() /* entspricht /sdcard/ */, "hsr.mbtiles");

/* Das verwenden von mbtiles ist leider ein wenig aufwändig, wir müssen
 * unsere XYTileSource in verschiedene Klassen 'verpacken' um sie dann
 * als TilesOverlay über der Grundkarte anzuzeigen.
 */
MapTileModuleProviderBase treasureMapModuleProvider = new MapTileFileArchiveProvider(new SimpleRegisterReceiver(this), 
		treasureMapTileSource, new IArchiveFile[] { MBTilesFileArchive.getDatabaseFileArchive(file) });

MapTileProviderBase treasureMapProvider = new MapTileProviderArray(treasureMapTileSource, null,
		new MapTileModuleProviderBase[] { treasureMapModuleProvider });

TilesOverlay treasureMapTilesOverlay = new TilesOverlay(treasureMapProvider, getBaseContext());
treasureMapTilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);

// Jetzt können wir den Overlay zu unserer Karte hinzufügen:
map.getOverlays().add(treasureMapTilesOverlay);
```

---

AppQuest Logbuch format
```
{
  "task": "Schatzkarte",
  "points": [
    {"lat": $lat1, "lon": $lon1},
    {"lat": $lat2, "lon": $lon2}
  ]
}
```

## License
[MIT License](https://github.com/mirioeggmann/appquest-treasure-map/blob/master/LICENSE)
