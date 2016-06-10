package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Individual implements RxMongoEntity<Individual> {
    ObjectId id
    String name
    Address address
    static embedded = ['address']

    static mapping = {
        name index:true
    }
}
