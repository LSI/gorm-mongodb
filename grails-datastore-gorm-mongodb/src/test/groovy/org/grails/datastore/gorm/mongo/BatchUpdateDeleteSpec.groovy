package org.grails.datastore.gorm.mongo

import grails.gorm.annotation.Entity
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
import spock.lang.IgnoreIf

/**
 * Created by graemerocher on 20/03/14.
 */
class BatchUpdateDeleteSpec extends GormDatastoreSpec {


    void "Test that batch delete works"() {
        when:"Some test data"
            createTestData()

        then:"The correct amount of data exists"
            Plant.count() == 6

        when:"a batch delete is executed"
            Plant.where {
                name == ~/Ca+/
            }.deleteAll()
            session.flush()

        then:"The right amount of data is deleted"
            Plant.count() == 4
    }

    // Ignore on Travis infrastructure which doesn't support MongoDB 26
    @IgnoreIf({ System.getenv("TRAVIS")})
    void "Test that batch update works"() {
        when:"Some test data"
            createTestData()

        then:"The correct amount of data exists"
            Plant.count() == 6

        when:"a batch delete is executed"
            Plant.where {
                name == ~/Ca+/
            }.updateAll(goesInPatch:true)
            session.flush()

        then:"The right amount of data is deleted"
            Plant.countByGoesInPatch(true) == 2
    }

    void "Test that batch update works with domain properties"() {
        given:
        BatchAddress addressA = new BatchAddress(name: "a").save()
        BatchAddress addressB = new BatchAddress(name: "b").save(flush: true, failOnError: true)
        new BatchUser(address: addressA).save()
        new BatchUser(address: addressA).save()
        new BatchUser(address: addressB).save(flush: true, failOnError: true)
        session.flush()

        when:
        int aCount = BatchUser.where { address == addressA }.count()
        int bCount = BatchUser.where { address == addressB }.count()

        then:
        aCount == 2
        bCount == 1

        when:
        BatchUser.where { address == addressA }.updateAll(address: addressB)
        session.flush()

        then:
        BatchUser.count() == 3
        BatchUser.where { address == addressB }.count() == 3
        BatchUser.where { address == addressA }.count() == 0
    }

    @Override
    List getDomainClasses() {
        [BatchUser,BatchAddress,Plant]
    }

    void createTestData() {
        new Plant(name: "Cabbage").save()
        new Plant(name: "Carrot").save()
        new Plant(name: "Lettuce").save()
        new Plant(name: "Pumpkin").save()
        new Plant(name: "Bamboo").save()
        new Plant(name: "Palm Tree").save(flush:true)
    }
}


@Entity
class BatchAddress {
    Long id
    String name
    static mapping = {
        version false
    }
}

@Entity
class BatchUser {
    Long id
    BatchAddress address
    static mapping = {
        version false
    }
}