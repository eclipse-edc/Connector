/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.transform.spi;

import java.util.List;

import static java.lang.String.join;
import static java.util.Objects.requireNonNull;

/**
 * Base functionality for problem builders.
 */
public abstract class AbstractProblemBuilder<B extends AbstractProblemBuilder<?>> {
    protected static final String UNKNOWN = "unknown";

    protected String type;
    protected String property;

    @SuppressWarnings("unchecked")
    public B type(String type) {
        this.type = type;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B type(Class<?> type) {
        this.type = type != null ? type.getName() : null;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B type(Enum<?> type) {
        this.type = type != null ? type.toString() : null;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B property(String property) {
        this.property = property;
        return (B) this;
    }

    /**
     * Concatenates the strings to a comma-separated list with the following form:
     * <pre>
     * ["one"] --> "one"
     * ["one", "two"] --> "one or two"
     * ["one", "two", "three"] --> "one, two, or three"
     * </pre>
     */
    protected String concatList(List<String> elements) {
        requireNonNull(elements);
        if (elements.size() == 0) {
            return "";
        } else if (elements.size() == 1) {
            return elements.get(0);
        } else if (elements.size() == 2) {
            return elements.get(0) + " or " + elements.get(1);
        }
        return join(", ", elements.subList(0, elements.size() - 1)) + ", or " + elements.get(elements.size() - 1);
    }

    public abstract void report();
}
