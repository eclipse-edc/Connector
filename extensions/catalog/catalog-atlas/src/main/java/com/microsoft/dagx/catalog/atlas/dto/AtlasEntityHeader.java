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
public class AtlasEntityHeader extends AtlasStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String guid = null;
    private AtlasEntity.Status status = AtlasEntity.Status.ACTIVE;
    private String displayText = null;
    private List<String> classificationNames = null;
    private List<AtlasClassification> classifications = null;
    private List<String> meaningNames = null;
    private List<AtlasTermAssignmentHeader> meanings = null;
    private Boolean isIncomplete = Boolean.FALSE;
    private Set<String> labels = null;

    public AtlasEntityHeader() {
        this(null, null);
    }

    public AtlasEntityHeader(String typeName) {
        this(typeName, null);
    }

    public AtlasEntityHeader(AtlasEntityDef entityDef) {
        this(entityDef != null ? entityDef.getName() : null, null);
    }

    public AtlasEntityHeader(String typeName, Map<String, Object> attributes) {
        super(typeName, attributes);

        setClassificationNames(null);
        setClassifications(null);
        setLabels(null);
    }


    public AtlasEntityHeader(String typeName, String guid, Map<String, Object> attributes) {
        super(typeName, attributes);
        setGuid(guid);
        setClassificationNames(null);
        setClassifications(null);
        setLabels(null);
    }


    public AtlasEntityHeader(AtlasEntityHeader other) {
        super(other);

        if (other != null) {
            setGuid(other.getGuid());
            setStatus(other.getStatus());
            setDisplayText(other.getDisplayText());
            setClassificationNames(other.getClassificationNames());
            setClassifications(other.getClassifications());
            setIsIncomplete(other.getIsIncomplete());
            setLabels(other.getLabels());
        }
    }

    public AtlasEntityHeader(AtlasEntity entity) {
        super(entity.getTypeName(), entity.getAttributes());
        setGuid(entity.getGuid());
        setStatus(entity.getStatus());
        setClassifications(entity.getClassifications());
        setIsIncomplete(entity.getIsIncomplete());

        if (Functions.isNotEmpty(entity.getClassifications())) {
            classificationNames = new ArrayList<>(entity.getClassifications().size());

            for (AtlasClassification classification : entity.getClassifications()) {
                classificationNames.add(classification.getTypeName());
            }
        }

        if (Functions.isNotEmpty(entity.getLabels())) {
            setLabels(entity.getLabels());
        }
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public AtlasEntity.Status getStatus() {
        return status;
    }

    public void setStatus(AtlasEntity.Status status) {
        this.status = status;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(final String displayText) {
        this.displayText = displayText;
    }

    public List<String> getClassificationNames() {
        return classificationNames;
    }

    public void setClassificationNames(List<String> classificationNames) {
        this.classificationNames = classificationNames;
    }

    public List<AtlasClassification> getClassifications() {
        return classifications;
    }

    public void setClassifications(List<AtlasClassification> classifications) {
        this.classifications = classifications;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }

    public Boolean getIsIncomplete() {
        return isIncomplete;
    }

    public void setIsIncomplete(Boolean isIncomplete) {
        this.isIncomplete = isIncomplete;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasEntityHeader{");
        sb.append("guid='").append(guid).append('\'');
        sb.append(", status=").append(status);
        sb.append(", displayText=").append(displayText);
        sb.append(", classificationNames=[");
        dumpObjects(classificationNames, sb);
        sb.append("], ");
        sb.append("classifications=[");
        AtlasBaseTypeDef.dumpObjects(classifications, sb);
        sb.append("], ");
        sb.append("labels=[");
        dumpObjects(labels, sb);
        sb.append("], ");
        sb.append("isIncomplete=").append(isIncomplete);
        super.toString(sb);
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AtlasEntityHeader that = (AtlasEntityHeader) o;
        return Objects.equals(guid, that.guid) &&
                status == that.status &&
                Objects.equals(displayText, that.displayText) &&
                Objects.equals(classificationNames, that.classificationNames) &&
                Objects.equals(meaningNames, that.classificationNames) &&
                Objects.equals(classifications, that.classifications) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(isIncomplete, that.isIncomplete) &&
                Objects.equals(meanings, that.meanings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), guid, status, displayText, classificationNames, classifications, meaningNames, meanings, isIncomplete, labels);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public List<String> getMeaningNames() {
        return meaningNames;
    }

    public void setMeaningNames(final List<String> meaningNames) {
        this.meaningNames = meaningNames;
    }

    public List<AtlasTermAssignmentHeader> getMeanings() {
        return meanings;
    }

    public void setMeanings(final List<AtlasTermAssignmentHeader> meanings) {
        this.meanings = meanings;
    }

}
