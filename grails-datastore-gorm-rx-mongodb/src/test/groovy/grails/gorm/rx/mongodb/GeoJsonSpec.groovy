package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Loc
import grails.gorm.rx.mongodb.domains.Place
import grails.mongodb.geo.Box
import grails.mongodb.geo.Circle
import grails.mongodb.geo.GeometryCollection
import grails.mongodb.geo.LineString
import grails.mongodb.geo.MultiLineString
import grails.mongodb.geo.MultiPoint
import grails.mongodb.geo.MultiPolygon
import grails.mongodb.geo.Point
import grails.mongodb.geo.Polygon
import grails.mongodb.geo.Sphere

class GeoJsonSpec extends RxGormSpec {

    void "Test persist GeometryCollection GeoJSON type"() {
        when:"A GeometryCollection is persisted and queried"
        def col = new GeometryCollection()
        col << Point.valueOf(5,10)
        col << LineString.valueOf([[40, 5 ], [41, 6 ] ])
        def p = new Place(geometryCollection: col)
        p.save(flush:true).toBlocking().first()
        p = Place.get(p.id).toBlocking().first()

        then:"The collection is correct"
        p.geometryCollection == col
    }

    void "Test persist MultiPoint GeoJSON type"() {
        when:"A domain with a multipoint is persisted"
        def mp = MultiPoint.valueOf([
                [ -73.9580, 40.8003 ],
                [ -73.9498, 40.7968 ],
                [ -73.9737, 40.7648 ],
                [ -73.9814, 40.7681 ]
        ])
        def p = new Place(multiPoint: mp)
        p.save(flush:true).toBlocking().first()
        p = Place.findByMultiPoint(mp).toBlocking().first()

        then:"The MultiPoint is persisted correctly"
        p != null
        p.multiPoint == mp
    }

    void "Test persist MultiLineString GeoJSON type"() {
        when:"A domain with a multipoint is persisted"
        def mls = MultiLineString.valueOf([
                [ [ -73.96943, 40.78519 ], [ -73.96082, 40.78095 ] ],
                [ [ -73.96415, 40.79229 ], [ -73.95544, 40.78854 ] ],
                [ [ -73.97162, 40.78205 ], [ -73.96374, 40.77715 ] ],
                [ [ -73.97880, 40.77247 ], [ -73.97036, 40.76811 ] ]
        ])
        def p = new Place(multiLineString: mls)
        p.save(flush:true).toBlocking().first()
        p = Place.findByMultiLineString(mls).toBlocking().first()

        then:"The MultiPoint is persisted correctly"
        p != null
        p.multiLineString== mls
    }

    void "Test persist MultiPolygon GeoJSON type"() {
        when:"A domain with a multipoint is persisted"
        def mp = MultiPolygon.valueOf([
                [ [ [  -73.958, 40.8003 ], [ -73.9498, 40.7968 ], [ -73.9737, 40.7648 ], [ -73.9814, 40.7681 ], [  -73.958, 40.8003 ] ] ],
                [ [ [  -73.958, 40.8003 ], [ -73.9498, 40.7968 ], [ -73.9737, 40.7648 ], [  -73.958, 40.8003 ] ] ]
        ])
        def p = new Place(multiPolygon: mp)
        p.save(flush:true).toBlocking().first()
        p = Place.findByMultiPolygon(mp).toBlocking().first()

        then:"The MultiPoint is persisted correctly"
        p != null
        p.multiPolygon == mp
    }

    void "Test persist GeoJSON types"() {
        given:"A domain with GeoJSON types"

        def point = new Point(5, 10)
        def poly = Polygon.valueOf([[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ])
        def line = LineString.valueOf([ [100.0, 0.0], [101.0, 1.0] ])
        def box = Box.valueOf([[0, 0], [10, 10]])
        def circle = Circle.valueOf([[5, 5], 3])

        def p = new Place(point: point,
                polygon: poly,
                lineString: line,
                box: box,
                circle: circle)

        when:"the entity is persisted and retrieved"
        p.save(flush:true).toBlocking().first()
        p = Place.get(p.id).toBlocking().first()

        then:"The GeoJSON types are correctly loaded"
        p.point == point
        p.polygon == poly
        p.lineString == line
        p.box == box
        p.circle == circle
    }

    void "Test geoWithin dynamic finder"() {
        when:"A domain with GeoJSON types"
        def point = new Point(2, 1)
        // Order: bottom left, top left, top right, bottom right, bottom left
        def poly1 = Polygon.valueOf([ [0.0, 0.0], [3.0, 0.0], [3.0, 3.0], [0.0, 3.0], [0.0, 0.0] ])
        def poly2 = Polygon.valueOf([ [5.0, 5.0], [7.0, 5.0], [7.0, 7.0], [5.0, 7.0], [5.0, 5.0] ])
        def p = new Place(point: point)
        p.save(flush:true).toBlocking().first()

        then:"A geoWithin query is executed to find a point within"
        Place.findByPointGeoWithin(poly1).toBlocking().first()
        Place.findByPointGeoWithin( Box.valueOf( [[0.0d, 0.0d], [5.0d, 5.0d]] ) ).toBlocking().first()
        Place.findByPointGeoWithin( Circle.valueOf( [[1.0d, 1.0d], 3.0d] ) ).toBlocking().first()
        Place.findByPointGeoWithin( Sphere.valueOf( [[1.0d, 1.0d], 0.06]) ).toBlocking().first()
        Place.findByPoint(point).toBlocking().first()
        Place.findByPointGeoWithin([ '$polygon': [ [0.0d, 0.0d], [3.0d, 0.0d], [3.0d, 3.0d], [0.0d, 3.0d], [0.0d, 0.0d] ] ]).toBlocking().first()

        when:
        !Place.findByPointGeoWithin(poly2).toBlocking().first()
        then:
        thrown(NoSuchElementException)

        when:
        !Place.findByPointGeoWithin( Box.valueOf( [[5.0d, 5.0d], [10.0d, 10.0d]] ) ).toBlocking().first()
        then:
        thrown(NoSuchElementException)

        when:
        !Place.findByPointGeoWithin( Circle.valueOf( [[10.0d, 10.0d], 3.0d] ) ).toBlocking().first()
        then:
        thrown(NoSuchElementException)

        when:
        !Place.findByPointGeoWithin( Sphere.valueOf( [[10.0d, 10.0d], 0.06] ) ).toBlocking().first()
        then:
        thrown(NoSuchElementException)


    }

    void "Test persist abstract shape type"() {
        when:"A data model based on https://blog.codecentric.de/en/2013/03/mongodb-geospatial-indexing-search-geojson-point-linestring-polygon/"
        createGeoDataModel()


        then:
        Loc.count().toBlocking().first() == 4
        Loc.findByName("P1").toBlocking().first().shape instanceof Point
        Loc.findByName("P2").toBlocking().first().shape instanceof Point
        Loc.findByName("Poly1").toBlocking().first().shape instanceof Polygon
        Loc.findByName("LS1").toBlocking().first().shape instanceof LineString
        Loc.findByShape(Point.valueOf(2, 2)).toBlocking().first()

        when:
        !Loc.findByShape(Point.valueOf(20, 10)).toBlocking().first()

        then:
        thrown(NoSuchElementException)

    }


    void "Test geoIntersects dynamic finder"() {
        when:"A geo data model"
        createGeoDataModel()

        then:
        Loc.findByShapeGeoIntersects( Polygon.valueOf( [[ [0,0], [3,0], [3,3], [0,3], [0,0] ]] ) ).toBlocking().first()
        Loc.findByShapeGeoIntersects( LineString.valueOf( [[1,4], [8,4]] ) ).toBlocking().first()
        Loc.findByShapeGeoIntersects( ['$geometry':[type:'Polygon', coordinates: [[ [0,0], [3,0], [3,3], [0,3], [0,0] ]]]] ).toBlocking().first()

        when:
        !Loc.findByShapeGeoIntersects( LineString.valueOf( [[1,7], [8,7]] ) ).toBlocking().first()
        then:
        thrown(NoSuchElementException)

        when:
        !Loc.findByShapeGeoIntersects( ['$geometry':[type:'LineString', coordinates: [[1,7], [8,7]] ]] ).toBlocking().first()
        then:
        thrown(NoSuchElementException)

    }

    void "Test near queries with GeoJSON types"() {
        given:"A geo data model"
        createGeoDataModel()

        when:"We find points near a given point"
        def results = Loc.findAllByShapeNear( Point.valueOf(1,7) ).toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 4
        results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
        results = Loc.findAllByShapeNear( [$geometry: [type:'Point', coordinates: [1,7]]] ).toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 4
        results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
        results = Loc.withCriteria {
            near 'shape', Point.valueOf(1,7), 300000
        }.toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 1
        results*.name == ['P2']

        when:"We look for points near a given point that are too far away"
        results = Loc.withCriteria {
            near 'shape', Point.valueOf(1,7), 1
        }.toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 0
    }


    void "Test nearSphere queries with GeoJSON types"() {
        given:"A geo data model"
        createGeoDataModel()

        when:"We find points near a given point"
        def results = Loc.findAllByShapeNearSphere( Point.valueOf(1,7) ).toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 4
        results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
        results = Loc.findAllByShapeNearSphere( [$geometry: [type:'Point', coordinates: [1,7]]] ).toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 4
        results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
        results = Loc.withCriteria {
            nearSphere 'shape', Point.valueOf(1,7), 300000
        }.toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 1
        results*.name == ['P2']
    }


    void "TestPolygonsPersist"(){
        when:
        Polygon p = Polygon.valueOf([ Point.valueOf(0,0), Point.valueOf(3,6), Point.valueOf(6,1), Point.valueOf(0,0)  ]) // points
        Loc l = new Loc(shape:p).save(flush:true).toBlocking().first()

        then:
        p.asList() == [ [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ] ]
        l.id != null


        when:
        p = Polygon.valueOf([ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ]) // number arrays as points
        l = new Loc(shape:p).save(flush:true).toBlocking().first()

        then:
        p.asList() == [ [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ] ]
        l.id != null


        when:
        p = Polygon.valueOf([ [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ] ]) // single ring with number arrays as points
        l = new Loc(shape:p).save(flush:true).toBlocking().first()

        then:

        p.asList() == [ [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ] ]
        l.id != null

        when:
        p = Polygon.valueOf( // multi rings with number array as points
                [
                        [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ], // exterior ring
                        [ [ 2 , 2 ] , [ 3 , 3 ] , [ 4 , 2 ] , [ 2 , 2 ] ]  // interior ring
                ]
        )
        l = new Loc(shape:p).save(flush:true).toBlocking().first()

        then:

        p.asList() == [
                [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ], // exterior ring
                [ [ 2 , 2 ] , [ 3 , 3 ] , [ 4 , 2 ] , [ 2 , 2 ] ]  // interior ring
        ]
        l.id != null


        when:
        p = Polygon.valueOf( // multi rings with number array as points
                [
                        [ Point.valueOf(0,0), Point.valueOf(3,6), Point.valueOf(6,1), Point.valueOf(0,0) ], // exterior ring
                        [ Point.valueOf(2,2), Point.valueOf(3,3), Point.valueOf(4,2), Point.valueOf(2,2) ]  // interior ring
                ]
        )
        l = new Loc(shape:p).save(flush:true).toBlocking().first()

        then:

        p.asList() == [
                [ [ 0 , 0 ] , [ 3 , 6 ] , [ 6 , 1 ] , [ 0 , 0 ] ], // exterior ring
                [ [ 2 , 2 ] , [ 3 , 3 ] , [ 4 , 2 ] , [ 2 , 2 ] ]  // interior ring
        ]
        l.id != null


    }



    void "Test multi-ring Polygons"() {
        when:"Define and Save a multi-ring polygon"
        Polygon multiringPolygon = Polygon.valueOf([
                [ [ 0 , 0 ] , [ 20 , 0 ] , [ 20 , 20 ] , [ 0 , 20 ], [ 0 , 0 ] ], // exterior ring
                [ [ 5 , 5 ] , [ 15 , 5 ] , [ 15 , 15 ] , [ 5 , 15 ], [ 5 , 5 ] ]  // interior ring
        ])

        Loc exampleMultiRingedPoly = new Loc(name:"MultiRingPoly1", shape:multiringPolygon).save(flush:true).toBlocking().first()

        then:
        Loc.findByShapeGeoIntersects( LineString.valueOf( [[-10,10], [30,10]] ) ).toBlocking().first().id == exampleMultiRingedPoly.id // test accross the whole poly
        Loc.findByShapeGeoIntersects( LineString.valueOf( [[-10,10], [10,10]] ) ).toBlocking().first().id == exampleMultiRingedPoly.id // test over the side

        when:
        Loc.findByShapeGeoIntersects( LineString.valueOf( [[-10,10], [-1,10]] ) ).toBlocking().first()  // test outside the poly
        then:
        thrown(NoSuchElementException)

        when:
        Loc.findByShapeGeoIntersects( LineString.valueOf( [[6,6], [9,9]] ) ).toBlocking().first()  // test within the exterior cut-out.
        then:
        thrown(NoSuchElementException)

    }


    /**
     * Creates a data model based on A data model based on
     * https://blog.codecentric.de/en/2013/03/mongodb-geospatial-indexing-search-geojson-point-linestring-polygon
     */
    protected void createGeoDataModel() {
        def p1 = Point.valueOf(2, 2)
        def p2 = Point.valueOf(3, 6)
        def poly1 = Polygon.valueOf([[[3, 1], [1, 2], [5, 6], [9, 2], [4, 3], [3, 1]]])
        def line1 = LineString.valueOf([[5, 2], [7, 3], [7, 5], [9, 4]])

        Loc.saveAll(
                new Loc(name: 'P1', shape: p1),
                new Loc(name: 'P2', shape: p2),
                new Loc(name: 'Poly1', shape: poly1),
                new Loc(name: 'LS1', shape: line1),
        ).toBlocking().first()
    }

    @Override
    List getDomainClasses() {
        [Place, Loc]
    }
}
