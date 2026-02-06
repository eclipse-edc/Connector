/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.guard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Records and plays a sequence of steps. Sequences may be repeated if {@link #repeat()} is enabled.
 */
public class StepRecorder<T> {
    private final Map<String, Sequence<T>> sequences = new HashMap<>();

    public synchronized StepRecorder<T> playNext(String key, T entity) {
        var sequence = sequences.get(key);
        if (sequence != null) {
            sequence.playNext(entity);
        }
        return this;
    }

    public synchronized StepRecorder<T> record(String key, Consumer<T> step) {
        var sequence = sequences.computeIfAbsent(key, k -> new Sequence<>());
        sequence.addStep(step);
        return this;
    }

    public synchronized StepRecorder<T> repeat() {
        for (var sequence : sequences.values()) {
            sequence.repeat = true;
        }
        return this;
    }


    private static class Sequence<T> {
        private final List<Consumer<T>> steps = new ArrayList<>();
        private boolean repeat;
        private int playIndex = 0;

        public void addStep(Consumer<T> step) {
            steps.add(step);
        }

        public void playNext(T entity) {
            if (steps.isEmpty()) {
                throw new IllegalStateException("No replay steps");
            }
            if (playIndex >= steps.size()) {
                throw new IllegalStateException("Exceeded replay steps");
            }
            steps.get(playIndex).accept(entity);
            if (repeat && playIndex == steps.size() - 1) {
                playIndex = 0;
            } else {
                playIndex++;
            }
        }
    }
}
