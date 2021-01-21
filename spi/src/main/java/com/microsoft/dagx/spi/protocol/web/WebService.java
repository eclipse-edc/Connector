package com.microsoft.dagx.spi.protocol.web;

/**
 * Manages the runtime web (HTTP) service.
 */
public interface WebService {

    /**
     * Registers a JAX-RS resource instance, or controller. Extensions may contribute bespoke APIs to the runtime.
     */
    void registerController(Object controller);

}
