package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * A destination where data is sent.
 */
@JsonTypeName("dagx:datatarget")
public interface DataDestination {

    String getType();
}
