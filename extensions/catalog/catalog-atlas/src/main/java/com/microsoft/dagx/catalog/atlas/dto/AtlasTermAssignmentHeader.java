/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasTermAssignmentHeader {
    private String termGuid;
    private String relationGuid;
    private String description;
    private String displayText;
    private String expression;
    private String createdBy;
    private String steward;
    private String source;
    private int confidence;

    private AtlasTermAssignmentStatus status;

    public AtlasTermAssignmentHeader() {
    }

    public String getTermGuid() {
        return termGuid;
    }

    public void setTermGuid(final String termGuid) {
        this.termGuid = termGuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public String getSteward() {
        return steward;
    }

    public void setSteward(final String steward) {
        this.steward = steward;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(final int confidence) {
        this.confidence = confidence;
    }

    public AtlasTermAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(final AtlasTermAssignmentStatus status) {
        this.status = status;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(final String displayText) {
        this.displayText = displayText;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AtlasTermAssignmentHeader)) {
            return false;
        }
        final AtlasTermAssignmentHeader that = (AtlasTermAssignmentHeader) o;
        return confidence == that.confidence &&
                Objects.equals(termGuid, that.termGuid) &&
                Objects.equals(relationGuid, that.relationGuid) &&
                Objects.equals(description, that.description) &&
                Objects.equals(expression, that.expression) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(steward, that.steward) &&
                Objects.equals(source, that.source) &&
                status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(termGuid, relationGuid, description, expression, createdBy, steward, source, confidence, status);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AtlasTermAssignmentId{");
        sb.append("termGuid='").append(termGuid).append('\'');
        sb.append(", relationGuid='").append(relationGuid).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", expression='").append(expression).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", steward='").append(steward).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", confidence=").append(confidence);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }


    public String getRelationGuid() {
        return relationGuid;
    }

    public void setRelationGuid(final String relationGuid) {
        this.relationGuid = relationGuid;
    }
}
