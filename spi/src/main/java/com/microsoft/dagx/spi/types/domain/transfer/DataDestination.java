package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A destination where data is sent.
 */
@JsonIgnoreProperties(value = "type", allowGetters = true)
public interface DataDestination {

    String getType();

    DestinationSecretToken getSecretToken();

}
