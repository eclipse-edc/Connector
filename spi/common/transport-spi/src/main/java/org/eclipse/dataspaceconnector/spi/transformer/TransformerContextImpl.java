package org.eclipse.dataspaceconnector.spi.transformer;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TransformerContextImpl implements TransformerContext {
    private final List<String> problems = new ArrayList<>();
    private final TypeTransformerRegistry<?> registry;

    public TransformerContextImpl(TypeTransformerRegistry<?> registry) {
        this.registry = registry;
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public List<String> getProblems() {
        return problems;
    }

    @Override
    public void reportProblem(String problem) {
        problems.add(problem);
    }

    @Override
    public <INPUT, OUTPUT> @Nullable OUTPUT transform(INPUT object, Class<OUTPUT> outputType) {
        return registry.transform(object, outputType).getContent();
    }
}
