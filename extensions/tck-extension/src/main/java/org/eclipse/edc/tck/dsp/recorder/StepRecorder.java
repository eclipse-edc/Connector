/*
 *  Copyright (c) 2024 Metaform Systems, Inc
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Metaform Systems, Inc - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.recorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Records and plays a sequence of steps. Sequences may be repeated if {@link #repeat()} is enabled.
 */
public class StepRecorder<T> {
    private boolean repeat;
    private int playIndex = 0;
    private Map<String, List<Consumer<T>>> sequences = new HashMap<>();

    public synchronized StepRecorder<T> playNext(String key, T entity) {
        var sequence = sequences.get(key);
        if (sequence == null) {
            throw new AssertionError("No sequence found for key " + key);
        }
        if (sequence.isEmpty()) {
            throw new IllegalStateException("No replay steps");
        }
        if (playIndex >= sequence.size()) {
            throw new IllegalStateException("Exceeded replay steps");
        }
        sequence.get(playIndex).accept(entity);
        if (repeat && playIndex == sequence.size() - 1) {
            playIndex = 0;
        } else {
            playIndex++;
        }
        return this;
    }

    public synchronized StepRecorder<T> record(String key, Consumer<T> step) {
        var sequence = sequences.computeIfAbsent(key, k -> new ArrayList<>());
        sequence.add(step);
        return this;
    }

    public synchronized StepRecorder<T> repeat() {
        repeat = true;
        return this;
    }
}
