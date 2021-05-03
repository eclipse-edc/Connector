package com.microsoft.dagx.transfer.nifi;


import java.util.HashMap;
import java.util.Map;

public class EndpointConverterRegistry {
    private final Map<String, NifiTransferEndpointConverter> converters;

    
    public EndpointConverterRegistry(){
        converters= new HashMap<>();
    }
    
    public void register(String type, NifiTransferEndpointConverter converter){
        converters.put(type, converter);
    }

    public NifiTransferEndpointConverter get(String type){
        return converters.get(type);
    }

    public EndpointConverterRegistry with(String type, NifiTransferEndpointConverter converter) {
        register(type, converter);
        return this;
    }
}
