package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("dagx:datatarget")
public interface DataTarget  {

    String getType();
}
