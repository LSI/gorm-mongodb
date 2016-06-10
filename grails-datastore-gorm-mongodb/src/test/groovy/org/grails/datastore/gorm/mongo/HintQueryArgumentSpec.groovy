package org.grails.datastore.gorm.mongo
import com.mongodb.MongoException
import grails.gorm.CriteriaBuilder
import grails.gorm.DetachedCriteria
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person

class HintQueryArgumentSpec extends GormDatastoreSpec {

    void "Test that hints work on criteria queries"() {
        when:"A criteria query is created with a hint"
            CriteriaBuilder c = Person.createCriteria()
            c.list {
                eq 'firstName', 'Bob'
                arguments hint:["firstName":1]
            }

        then:"The query contains the hint"
            c.query.@queryArguments == [hint:['firstName':1]]

        when:"A dynamic finder uses a hint"
            def results = Person.findAllByFirstName("Bob", [hint:"firstName"])
            for(e in results) {} // just to trigger the query

        then:"The hint is used"
            MongoException exception = thrown()
            exception.message.contains('bad hint')

        when:"A dynamic finder uses a hint"
            results = Person.findAllByFirstName("Bob", [hint:["firstName":1]])

        then:"The hint is used"
            results.size() == 0
     }

    void "Test that hints work on detached criteria queries"() {
        when:"A criteria query is created with a hint"
            DetachedCriteria<Person> detachedCriteria = new DetachedCriteria<>(Person)
            detachedCriteria = detachedCriteria.build {
                eq 'firstName', 'Bob'
            }

            def results = detachedCriteria.list(hint:["firstName":"blah"])
            for(e in results) {} // just to trigger the query
        then:"The hint is used"
            MongoException exception = thrown()
            exception.message.contains('bad hint')
    }
}
