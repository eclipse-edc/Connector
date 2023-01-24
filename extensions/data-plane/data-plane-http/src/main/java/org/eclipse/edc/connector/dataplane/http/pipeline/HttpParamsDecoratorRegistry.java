package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.ArrayList;
import java.util.List;

// TODO extract interface
// TODO document
public class HttpParamsDecoratorRegistry {

    private final List<HttpRequestParamsBuilderDecorator> sourceDecorators = new ArrayList<>();
    private final List<HttpRequestParamsBuilderDecorator> sinkDecorators = new ArrayList<>();

    public void registerSourceDecorator(HttpRequestParamsBuilderDecorator decorator) {
        sourceDecorators.add(decorator);
    }

    public void registerSinkDecorator(HttpRequestParamsBuilderDecorator decorator) {
        sinkDecorators.add(decorator);
    }

    public void decorateSink(DataFlowRequest request, HttpRequestParams.Builder builder) {
        sinkDecorators.forEach(decorator -> decorator.decorate(request, builder));
    }

    public void decorateSource(DataFlowRequest request, HttpRequestParams.Builder builder) {
        sourceDecorators.forEach(decorator -> decorator.decorate(request, builder));
    }
}
