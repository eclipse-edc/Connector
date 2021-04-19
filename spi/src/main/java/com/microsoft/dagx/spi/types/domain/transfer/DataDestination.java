package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A destination where data is sent.
 */
public interface DataDestination {

    String getType();

    DestinationSecretToken getSecretToken();

}
