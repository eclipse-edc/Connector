/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.Polymorphic;

import java.util.Map;

/**
 * A temporary token with write privileges to the data destination.
 */
@JsonTypeName("dagx:secrettoken")
public interface SecretToken extends Polymorphic {

    long getExpiration();

    Map<String, ?> flatten();
}
