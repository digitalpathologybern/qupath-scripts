//===================================================================
// import Indica Labs HALO AI GeoJSON objects to QuPath
// convert to detection and calulate measurements (diameter)
// Version 1.0
// University of Bern, Institute of Pathology
// Stefan Reinhard (stefan.reinhard@unibe.ch) 2023-07-05
//===================================================================

import qupath.lib.scripting.QP
import java.nio.charset.StandardCharsets
import java.nio.file.Files 
import java.nio.file.Paths
import org.apache.commons.io.IOUtils
import qupath.lib.objects.PathObjects 
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane


// 1 check for GeoJSON
def server = QP.getCurrentImageData().getServer()
def hierarchy = QP.getCurrentHierarchy()
def path = GeneralTools.toPath(server.getURIs()[0]).toString().replace(".mrxs", ".geojson");
def JSONfile = new File(path)
if (!JSONfile.exists()) {
    println "No GeoJSON file for this image..."
    return
}
// 2 read GeoJSON and write to annotation (line object)
String json = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
stream = IOUtils.toInputStream(json)
var objs = PathIO.readObjectsFromGeoJSON(stream);
for (annotation in objs) {
    hierarchy.addObject(annotation) 
}

// 3 convert line object to polygon object
def lineObjects = getAnnotationObjects().findAll {it.getROI().isLine()}
def polygonObjects = []
for (lineObject in lineObjects) {
    def line = lineObject.getROI()
    def polygon = ROIs.createPolygonROI(line.getAllPoints(), line.getImagePlane())
    def polygonObject = PathObjects.createAnnotationObject(polygon, lineObject.getPathClass())
    polygonObject.setName(lineObject.getName())
    polygonObject.setColor(lineObject.getColor())
    polygonObjects << polygonObject
}
removeObjects(lineObjects, true)
addObjects(polygonObjects)

// 4 convert to detection and add measurements
def annotations = getAnnotationObjects()
def detections = annotations.collect {
    new qupath.lib.objects.PathDetectionObject(it.getROI())
}
removeObjects(annotations, true)
addObjects(detections)
selectDetections()
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")
def roi = ROIs.createRectangleROI(0, 0, getCurrentServer().getWidth(), getCurrentServer().getHeight(), ImagePlane.getPlane(0, 0))
def annotation = PathObjects.createAnnotationObject(roi)
addObject(annotation)
resolveHierarchy()
fireHierarchyUpdate()