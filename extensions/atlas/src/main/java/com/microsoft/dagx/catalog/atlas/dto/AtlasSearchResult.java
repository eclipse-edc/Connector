/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasSearchResult implements Serializable {
    private AtlasSearchResult.AtlasQueryType queryType;
    private String queryText;
    private String type;
    private String classification;
    private List<AtlasEntityHeader> entities;
    private AtlasSearchResult.AttributeSearchResult attributes;
    private List<AtlasSearchResult.AtlasFullTextResult> fullTextResult;
    private Map<String, AtlasEntityHeader> referredEntities;
    private long approximateCount = -1;

    public AtlasSearchResult() {
    }

    public AtlasSearchResult(AtlasSearchResult.AtlasQueryType queryType) {
        this(null, queryType);
    }

    public AtlasSearchResult(String queryText, AtlasSearchResult.AtlasQueryType queryType) {
        setQueryText(queryText);
        setQueryType(queryType);
        setEntities(null);
        setAttributes(null);
        setFullTextResult(null);
        setReferredEntities(null);
    }


    public AtlasSearchResult.AtlasQueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(AtlasSearchResult.AtlasQueryType queryType) {
        this.queryType = queryType;
    }


    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public List<AtlasEntityHeader> getEntities() {
        return entities;
    }

    public void setEntities(List<AtlasEntityHeader> entities) {
        this.entities = entities;
    }

    public AtlasSearchResult.AttributeSearchResult getAttributes() {
        return attributes;
    }

    public void setAttributes(AtlasSearchResult.AttributeSearchResult attributes) {
        this.attributes = attributes;
    }

    public List<AtlasSearchResult.AtlasFullTextResult> getFullTextResult() {
        return fullTextResult;
    }

    public void setFullTextResult(List<AtlasSearchResult.AtlasFullTextResult> fullTextResult) {
        this.fullTextResult = fullTextResult;
    }

    public Map<String, AtlasEntityHeader> getReferredEntities() {
        return referredEntities;
    }

    public void setReferredEntities(Map<String, AtlasEntityHeader> referredEntities) {
        this.referredEntities = referredEntities;
    }

    public long getApproximateCount() {
        return approximateCount;
    }

    public void setApproximateCount(long approximateCount) {
        this.approximateCount = approximateCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryType, queryText, type, classification, entities, attributes, fullTextResult, referredEntities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AtlasSearchResult that = (AtlasSearchResult) o;
        return Objects.equals(queryType, that.queryType) &&
                Objects.equals(queryText, that.queryText) &&
                Objects.equals(type, that.type) &&
                Objects.equals(classification, that.classification) &&
                Objects.equals(entities, that.entities) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(fullTextResult, that.fullTextResult) &&
                Objects.equals(referredEntities, that.referredEntities);
    }

    public void addEntity(AtlasEntityHeader newEntity) {
        if (entities == null) {
            entities = new ArrayList<>();
        }

        if (entities.isEmpty()) {
            entities.add(newEntity);
        } else {
            removeEntity(newEntity);
            entities.add(newEntity);
        }
    }

    public void removeEntity(AtlasEntityHeader entity) {
        List<AtlasEntityHeader> entities = this.entities;

        if (entities != null && !entities.isEmpty()) {
            Iterator<AtlasEntityHeader> iter = entities.iterator();
            while (iter.hasNext()) {
                AtlasEntityHeader currEntity = iter.next();
                if (currEntity.getGuid().equals(entity.getGuid())) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AtlasSearchResult{" +
                "queryType=" + queryType +
                ", queryText='" + queryText + '\'' +
                ", type=" + type +
                ", classification=" + classification +
                ", entities=" + entities +
                ", attributes=" + attributes +
                ", fullTextResult=" + fullTextResult +
                ", referredEntities=" + referredEntities +
                ", approximateCount=" + approximateCount +
                '}';
    }

    public enum AtlasQueryType {DSL, FULL_TEXT, GREMLIN, BASIC, ATTRIBUTE, RELATIONSHIP}

    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttributeSearchResult {
        private List<String> name;
        private List<List<Object>> values;

        public AttributeSearchResult() {
        }

        public AttributeSearchResult(List<String> name, List<List<Object>> values) {
            this.name = name;
            this.values = values;
        }

        public List<String> getName() {
            return name;
        }

        public void setName(List<String> name) {
            this.name = name;
        }

        public List<List<Object>> getValues() {
            return values;
        }

        public void setValues(List<List<Object>> values) {
            this.values = values;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AtlasSearchResult.AttributeSearchResult that = (AtlasSearchResult.AttributeSearchResult) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, values);
        }

        @Override
        public String toString() {
            return "AttributeSearchResult{" +
                    "name=" + name + ", " +
                    "values=" + values +
                    '}';
        }
    }

    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtlasFullTextResult {
        AtlasEntityHeader entity;
        Double score;

        public AtlasFullTextResult() {
        }

        public AtlasFullTextResult(AtlasEntityHeader entity, Double score) {
            this.entity = entity;
            this.score = score;
        }

        public AtlasEntityHeader getEntity() {
            return entity;
        }

        public void setEntity(AtlasEntityHeader entity) {
            this.entity = entity;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AtlasSearchResult.AtlasFullTextResult that = (AtlasSearchResult.AtlasFullTextResult) o;
            return Objects.equals(entity, that.entity) &&
                    Objects.equals(score, that.score);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity, score);
        }

        @Override
        public String toString() {
            return "AtlasFullTextResult{" +
                    "entity=" + entity +
                    ", score=" + score +
                    '}';
        }
    }
}