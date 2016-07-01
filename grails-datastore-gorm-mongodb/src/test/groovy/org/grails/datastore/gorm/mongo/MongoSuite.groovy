package org.grails.datastore.gorm.mongo

import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.CommonTypesPersistenceSpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.DeleteAllSpec
import grails.gorm.tests.DetachedCriteriaSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.FindWhereSpec
import grails.gorm.tests.GormEnhancerSpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.OneToOneSpec
import grails.gorm.tests.OptimisticLockingSpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.SizeQuerySpec
import grails.gorm.tests.ValidationSpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import grails.gorm.tests.PagedResultSpec
import grails.gorm.tests.EnumSpec
import grails.gorm.tests.OrderBySpec
import grails.gorm.tests.OneToManySpec

/**
 * @author graemerocher
 */
@RunWith(Suite)
@SuiteClasses([
    AttachMethodSpec
])
class MongoSuite {
}
