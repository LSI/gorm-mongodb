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
package org.grails.datastore.mapping.mongo.config;

import java.util.*;

import org.grails.datastore.mapping.document.config.Collection;
import org.grails.datastore.mapping.query.Query;

import com.mongodb.WriteConcern;

/**
 * Provides configuration options for mapping Mongo DBCollection instances
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class MongoCollection extends Collection {

    private String database;
    private WriteConcern writeConcern;
    private List<Map> compoundIndices = new ArrayList<Map>();
    private Query.Order sort;
    private List<Index> indices = new ArrayList<Index>();


    public void index(Map<String, Object> definition) {
        index(definition, Collections.<String,Object>emptyMap());
    }

    public void index(Map<String, Object> definition, Map<String, Object> options) {
        if(definition != null && !definition.isEmpty()) {
            indices.add(new Index(definition, options));
        }
    }

    public List<Index> getIndices() {
        return indices;
    }

    public Query.Order getSort() {
        return sort;
    }

    public void setSort(Object s) {
        if (s instanceof Query.Order) {
            this.sort = (Query.Order) s;
        }
        if (s instanceof Map) {
            Map m = (Map) s;
            if (!m.isEmpty()) {
                Map.Entry entry = (Map.Entry) m.entrySet().iterator().next();
                Object key = entry.getKey();
                if ("desc".equalsIgnoreCase(entry.getValue().toString())) {
                    this.sort = Query.Order.desc(key.toString());
                }
                else {
                    this.sort = Query.Order.asc(key.toString());
                }
            }
        }
        else {
            this.sort = Query.Order.asc(s.toString());
        }
    }

    /**
     * The database to use
     *
     * @return The name of the database
     */
    public String getDatabase() {
        return database;
    }
    /**
     * The name of the database to use
     *
     * @param database The database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return The {@link WriteConcern} for the collection
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * The {@link WriteConcern} for the collection
     */
    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    /**
     * Sets a compound index definition
     *
     * @param compoundIndex The compount index
     */
    public void setCompoundIndex(Map compoundIndex) {
        if (compoundIndex != null) {
            compoundIndices.add(compoundIndex);
        }
    }

    /**
     * Return all defined compound indices
     *
     * @return The compound indices to return
     */
    public List<Map> getCompoundIndices() {
        return compoundIndices;
    }

    /**
     * Definition of an index
     */
    public static class Index {
        Map<String, Object> definition = new HashMap<String, Object>();
        Map<String, Object> options = new HashMap<String, Object>();


        public Index(Map<String, Object> definition) {
            this.definition = definition;
        }

        public Index(Map<String, Object> definition, Map<String, Object> options) {
            this.definition = definition;
            this.options = options;
        }

        public Map<String, Object> getDefinition() {
            return definition;
        }

        public Map<String, Object> getOptions() {
            return options;
        }
    }
}
