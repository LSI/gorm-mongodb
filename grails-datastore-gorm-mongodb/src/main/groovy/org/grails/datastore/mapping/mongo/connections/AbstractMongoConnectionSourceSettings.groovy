package org.grails.datastore.mapping.mongo.connections

import com.mongodb.ConnectionString
import com.mongodb.ServerAddress
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.config.MongoSettings

/**
 * @author Graeme Rocher
 * @since 6.0
 */
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
@CompileStatic
abstract class AbstractMongoConnectionSourceSettings extends ConnectionSourceSettings implements MongoSettings {

    /**
     * The connection string
     */
    protected ConnectionString connectionString

    /**
     * The default database name
     */
    String databaseName = "test"


    /**
     * The host name to use
     */
    String host = ServerAddress.defaultHost();

    /**
     * The port to use
     */
    Integer port = ServerAddress.defaultPort()

    /**
     * The username to use
     */
    String username
    /**
     * The password to use
     */
    String password
    /**
     * The engine to use by default
     */
    String engine = MongoConstants.CODEC_ENGINE

    /**
     * Whether to use stateless mode by default
     */
    boolean stateless = false

    /**
     * @return Obtain the final URL whether from the connection string or the host/port setting
     */
    ConnectionString getUrl() {
        if(connectionString != null) {
            return connectionString
        }
        else {
            String uAndP = username && password ? "$username:$password@" : ''
            String portStr = port ? ":$port" : ''
            return new ConnectionString("mongodb://${uAndP}${host}${portStr}/$database")
        }
    }

    /**
     * @param connectionString The connection string
     */
    void url(ConnectionString connectionString) {
        this.connectionString = connectionString
    }

    /**
     * @return Obtain the database name
     */
    String getDatabase() {
        if(connectionString != null) {
            return connectionString.database ?: databaseName
        }
        else {
            return databaseName
        }
    }

}
