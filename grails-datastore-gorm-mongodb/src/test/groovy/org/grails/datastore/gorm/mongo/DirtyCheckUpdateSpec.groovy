package org.grails.datastore.gorm.mongo

import grails.gorm.dirty.checking.DirtyCheck
import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import spock.lang.Issue

/**
 * Created by graemerocher on 14/03/14.
 */
class DirtyCheckUpdateSpec extends GormDatastoreSpec{

    @Issue('GPMONGODB-334')
    void "Test that dirty check works for simple lists"() {
        given:"A a domain instance"
            def b = new Bar(foo:"stuff", strings:['a', 'b'])
            b.save(flush:true)
            session.clear()

        when:"The list is updated"
            b = Bar.get(b.id)
            b.strings << 'c'
            b.save(flush:true)
            session.clear()
            b = Bar.get(b.id)

        then:"the update was executed"
            b.strings.size() == 3
            b instanceof DirtyCheckable
    }

    @Override
    List getDomainClasses() {
        [Bar]
    }
}

@Entity
@DirtyCheck
class Bar {
    ObjectId id

    String foo
    List<String> strings = new ArrayList()

}