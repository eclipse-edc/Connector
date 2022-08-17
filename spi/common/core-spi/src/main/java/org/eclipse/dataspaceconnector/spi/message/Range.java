/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.message;

/**
 * Represents a particular section of a collection of items.
 */
public class Range {
    public static final String FROM = "from";
    public static final String TO = "to";
    private final int from;
    private final int to;

    public Range(int from, int to) {
        this.from = from;
        this.to = to;
    }

    /**
     * The index of the last item to be included in the range. Note that the actual number may be lower if the range
     * overshoots the bounds of the collection
     */
    public int getTo() {
        return to;
    }

    /**
     * The index of the first item to be included in the range.
     */
    public int getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return "Range{" +
                "from " + from +
                ", to " + to +
                '}';
    }
}
