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

package org.eclipse.edc.util.reflection;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent an object path instance, e.g. "properties.'https://w3id.org/edc/v0.0.1/ns/id'" can be parsed into two
 * {@link PathItem}s one representing "properties" and the other "https://w3id.org/edc/v0.0.1/ns/id"
 */
public class PathItem {

    public static List<PathItem> parse(String propertyName) {
        var result = new ArrayList<PathItem>();
        result.add(new PathItem());
        for (var i = 0; i < propertyName.length(); i++) {
            var character = propertyName.charAt(i);

            var lastEntry = result.get(result.size() - 1);

            switch (character) {
                case '\'' -> lastEntry.toggle();
                case '.' -> {
                    if (lastEntry.opened) {
                        lastEntry.append(character);
                    } else {
                        result.add(new PathItem());
                    }
                }
                default -> lastEntry.append(character);
            }

        }
        return result.stream().toList();
    }

    private boolean opened;
    private final StringBuilder builder = new StringBuilder();

    public void open() {
        this.opened = true;
    }

    public void close() {
        this.opened = false;
    }

    public void append(char character) {
        builder.append(character);
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public void toggle() {
        if (opened) {
            close();
        } else {
            open();
        }
    }
}
