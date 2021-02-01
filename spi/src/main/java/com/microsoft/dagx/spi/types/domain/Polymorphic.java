package com.microsoft.dagx.spi.types.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Types implement this interface for polymorphic de/serialization support.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "dagxtype")
public interface Polymorphic {
}
