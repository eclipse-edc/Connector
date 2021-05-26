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
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeBoundary implements Serializable {
    public static final String TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final long serialVersionUID = 1L;
    private String startTime;
    private String endTime;
    private String timeZone; // null for local-time; or a valid ID for TimeZone.getTimeZone(id)

    public TimeBoundary() {
        this(null, null, null);
    }

    public TimeBoundary(String startTime) {
        this(startTime, null, null);
    }

    public TimeBoundary(String startTime, String endTime) {
        this(startTime, endTime, null);
    }

    public TimeBoundary(String startTime, String endTime, String timeZone) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeZone = timeZone;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeBoundary that = (TimeBoundary) o;
        return Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(timeZone, that.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, timeZone);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TimeBoundary{");

        sb.append("startTime='").append(startTime)
                .append("; endTime='").append(endTime)
                .append("; timeZone='").append(timeZone)
                .append('}');

        return sb.toString();
    }
}
