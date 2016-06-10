
package org.grails.datastore.gorm

import com.mongodb.BasicDBObject
import grails.gorm.tests.GormDatastoreSpec
import org.bson.Document
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.mongo.Birthday
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query.Between
import org.grails.datastore.mapping.query.Query.PropertyCriterion
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * @author graemerocher
 */
class Setup {

    static MongoDatastore mongo
    static AbstractMongoSession session

    static destroy() {
        session.nativeInterface.dropDatabase( session.defaultDatabase )
        session.disconnect()
        mongo.destroy()
    }

    static Session setup(classes) {
        mongo = new MongoDatastore(new MongoMappingContext(System.getProperty(GormDatastoreSpec.CURRENT_TEST_NAME) ?: 'test'), Collections.emptyMap(), null)
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        mongo.applicationContext = ctx
        mongo.afterPropertiesSet()

        mongo.mappingContext.mappingFactory.registerCustomType(new AbstractMappingAwareCustomTypeMarshaller<Birthday, Document, Document>(Birthday) {
            @Override
            protected Object writeInternal(PersistentProperty property, String key, Birthday value, Document nativeTarget) {
                final converted = value.date.time
                nativeTarget.put(key, converted)
                return converted
            }

            @Override
            protected void queryInternal(PersistentProperty property, String key, PropertyCriterion criterion, Document nativeQuery) {
                if (criterion instanceof Between) {
                    def dbo = new BasicDBObject()
                    dbo.put(MongoQuery.MONGO_GTE_OPERATOR, criterion.getFrom().date.time)
                    dbo.put(MongoQuery.MONGO_LTE_OPERATOR, criterion.getTo().date.time)
                    nativeQuery.put(key, dbo)
                }
                else {
                    nativeQuery.put(key, criterion.value.date.time)
                }
            }

            @Override
            protected Birthday readInternal(PersistentProperty property, String key, Document nativeSource) {
                final num = nativeSource.get(key)
                if (num instanceof Long) {
                    return new Birthday(new Date(num))
                }
                return null
            }
        })

        for (cls in classes) {
            mongo.mappingContext.addPersistentEntity(cls)
        }

        PersistentEntity entity = mongo.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        mongo.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if (!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                }
            }
        ] as Validator)

        def txMgr = new DatastoreTransactionManager(datastore: mongo)

        def enhancer = new MongoGormEnhancer(mongo, txMgr)
        mongo.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        mongo.applicationContext.addApplicationListener new DomainEventListener(mongo)
        mongo.applicationContext.addApplicationListener new AutoTimestampEventListener(mongo)

        session = mongo.connect()

        return session
    }
}
