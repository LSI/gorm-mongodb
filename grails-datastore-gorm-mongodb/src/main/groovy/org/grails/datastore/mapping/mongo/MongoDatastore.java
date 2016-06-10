/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.mongo;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.*;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.grails.datastore.gorm.mongo.bean.factory.MongoClientFactoryBean;
import org.grails.datastore.mapping.core.*;
import org.grails.datastore.mapping.document.config.DocumentMappingContext;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.mongo.config.MongoAttribute;
import org.grails.datastore.mapping.mongo.config.MongoCollection;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.mapping.mongo.engine.codecs.AdditionalCodecs;
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.PropertyResolver;

/**
 * A Datastore implementation for the Mongo document store.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener, DisposableBean, StatelessDatastore {

    public static final String SETTING_DATABASE_NAME = "grails.mongodb.databaseName";
    public static final String SETTING_CONNECTION_STRING = "grails.mongodb.connectionString";
    public static final String SETTING_URL = "grails.mongodb.url";
    public static final String SETTING_DEFAULT_MAPPING = "grails.mongodb.default.mapping";
    public static final String SETTING_OPTIONS = "grails.mongodb.options";
    public static final String SETTING_HOST = "grails.mongodb.host";
    public static final String SETTING_PORT = "grails.mongodb.port";
    public static final String SETTING_USERNAME = "grails.mongodb.username";
    public static final String SETTING_PASSWORD = "grails.mongodb.password";
    public static final String SETTING_REPLICA_SET = "grails.mongodb.replicaSet";
    public static final String SETTING_REPLICA_PAIR = "grails.mongodb.replicaPair";
    public static final String SETTING_STATELESS = "grails.mongodb.stateless";
    public static final String SETTING_ENGINE = "grails.mongodb.engine";
    public static final String INDEX_ATTRIBUTES = "indexAttributes";
    public static final String CODEC_ENGINE = "codec";

    protected MongoClient mongo;
    protected MongoClientOptions mongoOptions;
    protected final String defaultDatabase;
    protected Map<PersistentEntity, String> mongoCollections = new ConcurrentHashMap<PersistentEntity, String>();
    protected Map<PersistentEntity, String> mongoDatabases = new ConcurrentHashMap<PersistentEntity, String>();
    protected boolean stateless = false;
    protected boolean codecEngine = true;
    protected CodecRegistry codecRegistry;



    /**
     * Constructs a MongoDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext    The MongoMappingContext
     * @param connectionDetails The connection details containing the {@link #SETTING_HOST} and {@link #SETTING_PORT} settings
     */
    public MongoDatastore(MongoMappingContext mappingContext,
                          Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
        this(mappingContext, mapToPropertyResolver(connectionDetails), ctx);
    }

    /**
     * Constructs a MongoDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext    The MongoMappingContext
     * @param configuration The connection details containing the {@link #SETTING_HOST} and {@link #SETTING_PORT} settings
     */
    public MongoDatastore(MongoMappingContext mappingContext,
                          PropertyResolver configuration, ConfigurableApplicationContext ctx) {
        super(mappingContext, configuration, ctx);
        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }


        this.defaultDatabase = mappingContext.getDefaultDatabaseName();

        initializeConverters(mappingContext);

        final ConverterRegistry converterRegistry = mappingContext.getConverterRegistry();
        converterRegistry.addConverter(new Converter<String, ObjectId>() {
            public ObjectId convert(String source) {
                return new ObjectId(source);
            }
        });

        converterRegistry.addConverter(new Converter<ObjectId, String>() {
            public String convert(ObjectId source) {
                return source.toString();
            }
        });

        converterRegistry.addConverter(new Converter<byte[], Binary>() {
            public Binary convert(byte[] source) {
                return new Binary(source);
            }
        });

        converterRegistry.addConverter(new Converter<Binary, byte[]>() {
            public byte[] convert(Binary source) {
                return source.getData();
            }
        });

        for (Converter converter : AdditionalCodecs.getBsonConverters()) {
            converterRegistry.addConverter(converter);
        }

        codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new AdditionalCodecs(), new PersistentEntityCodeRegistry())
        );
    }

    /**
     * Constructs a MongoDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext    The MongoMappingContext
     */
    public MongoDatastore(MongoMappingContext mappingContext, ConfigurableApplicationContext ctx) {
        this(mappingContext, ctx.getEnvironment(), ctx);
    }

    /**
     * Constructs a MongoDatastore using the default database name of "test" and defaults for the host and port.
     * Typically used during testing.
     */
    public MongoDatastore() {
        this(new MongoMappingContext("test"), Collections.<String, Object>emptyMap(), null);
    }

    /**
     * Constructs a MongoDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext    The MongoMappingContext
     * @param connectionDetails The connection details containing the {@link #SETTING_HOST} and {@link #SETTING_PORT} settings
     */
    public MongoDatastore(MongoMappingContext mappingContext,
                          Map<String, Object> connectionDetails, MongoClientOptions mongoOptions, ConfigurableApplicationContext ctx) {

        this(mappingContext, connectionDetails, ctx);
        if (mongoOptions != null) {
            this.mongoOptions = mongoOptions;
        }
    }



    public MongoDatastore(MongoMappingContext mappingContext) {
        this(mappingContext, Collections.<String, Object>emptyMap(), null);
    }

    /**
     * Constructor for creating a MongoDatastore using an existing Mongo instance
     *
     * @param mappingContext The MappingContext
     * @param mongo          The existing Mongo instance
     */
    public MongoDatastore(MongoMappingContext mappingContext, MongoClient mongo,
                          ConfigurableApplicationContext ctx) {
        this(mappingContext, ctx.getEnvironment(), ctx);
        this.mongo = mongo;
    }

    /**
     * Constructor for creating a MongoDatastore using an existing Mongo instance. In this case
     * the connection details are only used to supply a USERNAME and PASSWORD
     *
     * @param mappingContext The MappingContext
     * @param mongo          The existing Mongo instance
     */
    public MongoDatastore(MongoMappingContext mappingContext, MongoClient mongo,
                          Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
        this(mappingContext, connectionDetails, ctx);
        this.mongo = mongo;
    }

    /**
     * Constructor for creating a MongoDatastore using an existing Mongo instance
     *
     * @param mappingContext The MappingContext
     * @param mongo          The existing Mongo instance
     * @deprecated The {@link Mongo} class is deprecated
     */
    @Deprecated
    public MongoDatastore(MongoMappingContext mappingContext, Mongo mongo,
                          ConfigurableApplicationContext ctx) {
        this(mappingContext, ctx.getEnvironment(), ctx);
        this.mongo = (MongoClient) mongo;
    }

    /**
     * Constructor for creating a MongoDatastore using an existing Mongo instance. In this case
     * the connection details are only used to supply a USERNAME and PASSWORD
     *
     * @param mappingContext The MappingContext
     * @param mongo          The existing Mongo instance
     * @deprecated The {@link Mongo} class is deprecated
     */
    @Deprecated
    public MongoDatastore(MongoMappingContext mappingContext, Mongo mongo,
                          Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
        this(mappingContext, connectionDetails, ctx);
        this.mongo = (MongoClient) mongo;
    }

    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    @Autowired(required = false)
    public void setCodecRegistries(List<CodecRegistry> codecRegistries) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromRegistries(codecRegistries));
    }

    @Autowired(required = false)
    public void setCodecProviders(List<CodecProvider> codecProviders) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromProviders(codecProviders));
    }

    @Autowired(required = false)
    public void setCodecs(List<Codec<?>> codecs) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromCodecs(codecs));
    }

    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    public PersistentEntityCodec getPersistentEntityCodec(PersistentEntity entity) {
        if (entity instanceof EmbeddedPersistentEntity) {
            return new PersistentEntityCodec(codecRegistry, entity);
        } else {
            return getPersistentEntityCodec(entity.getJavaClass());
        }
    }

    public PersistentEntityCodec getPersistentEntityCodec(Class entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Argument [entityClass] cannot be null");
        }

        final PersistentEntity entity = getMappingContext().getPersistentEntity(entityClass.getName());
        if (entity == null) {
            throw new IllegalArgumentException("Argument [" + entityClass + "] is not an entity");
        }

        return (PersistentEntityCodec) getCodecRegistry().get(entity.getJavaClass());
    }

    /**
     * @deprecated Use {@link #getMongoClient()} instead
     */
    @Deprecated
    public Mongo getMongo() {
        return mongo;
    }

    public MongoClient getMongoClient() {
        return mongo;
    }

    public String getCollectionName(PersistentEntity entity) {
        final String collectionName = mongoCollections.get(entity);
        if(collectionName == null) {
            final String decapitalizedName = entity.getDecapitalizedName();
            mongoCollections.put(entity, decapitalizedName);
            return decapitalizedName;
        }
        return collectionName;
    }

    @Override
    protected Session createSession(PropertyResolver connDetails) {
        if (stateless) {
            return createStatelessSession(connDetails);
        } else {
            if (codecEngine) {
                return new MongoCodecSession(this, getMappingContext(), getApplicationEventPublisher(), false);
            } else {
                return new MongoSession(this, getMappingContext(), getApplicationEventPublisher(), false);
            }
        }
    }

    @Override
    protected Session createStatelessSession(PropertyResolver connectionDetails) {
        if (codecEngine) {
            return new MongoCodecSession(this, getMappingContext(), getApplicationEventPublisher(), true);
        } else {
            return new MongoSession(this, getMappingContext(), getApplicationEventPublisher(), true);
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (mongo == null && connectionDetails != null) {
            ServerAddress defaults = new ServerAddress();
            String username = connectionDetails.getProperty(SETTING_USERNAME, (String)null);
            String password = connectionDetails.getProperty(SETTING_PASSWORD, (String)null);
            DocumentMappingContext dc = getMappingContext();
            String databaseName = dc.getDefaultDatabaseName();

            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
            if (username != null && password != null) {
                credentials.add(MongoCredential.createCredential(username, databaseName, password.toCharArray()));
            }

            String host = connectionDetails.getProperty(SETTING_HOST, defaults.getHost());
            int port = connectionDetails.getProperty(SETTING_PORT, Integer.class, defaults.getPort());
            ServerAddress serverAddress = new ServerAddress(host,port);
            if (mongoOptions != null) {
                mongo = new MongoClient(serverAddress, credentials, mongoOptions);
            } else {
                MongoClientOptions.Builder builder = MongoClientOptions.builder();
                builder.codecRegistry(
                        CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), new MongoClientFactoryBean.DefaultGrailsCodecRegistry())
                );
                mongoOptions = builder.build();
                mongo = new MongoClient(serverAddress, credentials, mongoOptions);
            }
        }
        else if(mongo == null) {
            MongoClientOptions.Builder builder = MongoClientOptions.builder();
            builder.codecRegistry(
                    CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), new MongoClientFactoryBean.DefaultGrailsCodecRegistry())
            );
            mongoOptions = builder.build();
            mongo = new MongoClient(new ServerAddress(), mongoOptions);
        }

        if(connectionDetails != null) {

            this.stateless = connectionDetails.getProperty(SETTING_STATELESS, Boolean.class, false);
            this.codecEngine = connectionDetails.getProperty(SETTING_ENGINE, String.class, CODEC_ENGINE).equals(CODEC_ENGINE);
        }
        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            // Only create Mongo templates for entities that are mapped with Mongo
            if (!entity.isExternal()) {
                initializeIndices(entity);
            }
        }
    }

    @Override
    public DocumentMappingContext getMappingContext() {
        return (DocumentMappingContext) super.getMappingContext();
    }

    /**
     * Indexes any properties that are mapped with index:true
     *
     * @param entity The entity
     */
    protected void initializeIndices(final PersistentEntity entity) {
        String collectionName = entity.getDecapitalizedName();
        String databaseName = getMappingContext().getDefaultDatabaseName();

        MongoCollection collectionMapping = (MongoCollection)entity.getMapping().getMappedForm();
        if(collectionMapping.getCollection() != null) {
            collectionName = collectionMapping.getCollection();
        }
        if(collectionMapping.getDatabase() != null) {
            databaseName = collectionMapping.getDatabase();
        }

        mongoCollections.put(entity, collectionName);
        mongoDatabases.put(entity,databaseName);

        final com.mongodb.client.MongoCollection<Document> collection = getMongoClient().getDatabase(databaseName)
                .getCollection(collectionName);


        final ClassMapping<MongoCollection> classMapping = entity.getMapping();
        if (classMapping != null) {
            final MongoCollection mappedForm = classMapping.getMappedForm();
            if (mappedForm != null) {
                List<MongoCollection.Index> indices = mappedForm.getIndices();
                for (MongoCollection.Index index : indices) {
                    final Map<String, Object> options = index.getOptions();
                    final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, options);
                    collection.createIndex(new Document(index.getDefinition()), indexOptions);
                }

                for (Map compoundIndex : mappedForm.getCompoundIndices()) {

                    Map indexAttributes = null;
                    if (compoundIndex.containsKey(INDEX_ATTRIBUTES)) {
                        Object o = compoundIndex.remove(INDEX_ATTRIBUTES);
                        if (o instanceof Map) {
                            indexAttributes = (Map) o;
                        }
                    }
                    Document indexDef = new Document(compoundIndex);
                    if (indexAttributes != null) {
                        final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, indexAttributes);
                        collection.createIndex(indexDef, indexOptions);
                    } else {
                        collection.createIndex(indexDef);
                    }
                }
            }
        }

        for (PersistentProperty<MongoAttribute> property : entity.getPersistentProperties()) {
            final boolean indexed = isIndexed(property);

            if (indexed) {
                final MongoAttribute mongoAttributeMapping = property.getMapping().getMappedForm();
                Document dbObject = new Document();
                final String fieldName = getMongoFieldNameForProperty(property);
                dbObject.put(fieldName, 1);
                Document options = new Document();
                if (mongoAttributeMapping != null) {
                    Map attributes = mongoAttributeMapping.getIndexAttributes();
                    if (attributes != null) {
                        attributes = new HashMap(attributes);
                        if (attributes.containsKey(MongoAttribute.INDEX_TYPE)) {
                            dbObject.put(fieldName, attributes.remove(MongoAttribute.INDEX_TYPE));
                        }
                        options.putAll(attributes);
                    }
                }
                // continue using deprecated method to support older versions of MongoDB
                if (options.isEmpty()) {
                    collection.createIndex(dbObject);
                } else {
                    final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, options);
                    collection.createIndex(dbObject, indexOptions);
                }
            }
        }


    }

    String getMongoFieldNameForProperty(PersistentProperty<MongoAttribute> property) {
        PropertyMapping<MongoAttribute> pm = property.getMapping();
        String propKey = null;
        if (pm.getMappedForm() != null) {
            propKey = pm.getMappedForm().getField();
        }
        if (propKey == null) {
            propKey = property.getName();
        }
        return propKey;
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        initializeIndices(entity);
    }

    public void destroy() throws Exception {
        super.destroy();
        if (mongo != null) {
            mongo.close();
        }
    }

    @Override
    public boolean isSchemaless() {
        return true;
    }


    public String getDatabaseName(PersistentEntity entity) {
        final String databaseName = mongoDatabases.get(entity);
        if(databaseName == null) {
            mongoDatabases.put(entity, defaultDatabase);
            return defaultDatabase;
        }
        return databaseName;
    }

    class PersistentEntityCodeRegistry implements CodecProvider {

        Map<String, PersistentEntityCodec> codecs = new HashMap<String, PersistentEntityCodec>();

        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            final String entityName = clazz.getName();
            PersistentEntityCodec codec = codecs.get(entityName);
            if (codec == null) {
                final PersistentEntity entity = getMappingContext().getPersistentEntity(entityName);
                if (entity != null) {
                    codec = new PersistentEntityCodec(codecRegistry, entity);
                    codecs.put(entityName, codec);
                }
            }
            return codec;
        }
    }
}
