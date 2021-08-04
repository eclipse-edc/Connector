/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.types.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Types implement this interface for polymorphic de/serialization support.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "edctype")
public interface Polymorphic {
}
