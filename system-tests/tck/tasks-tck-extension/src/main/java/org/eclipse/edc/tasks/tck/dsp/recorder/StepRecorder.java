/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.tasks.tck.dsp.recorder;

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

    private final Step.ContinueStep<T> proceedStep = new Step.ContinueStep<>();

    public synchronized Step<T> nextStep(String key) {
        var sequence = sequences.get(key);
        if (sequence != null) {
            return sequence.nextStep();
        } else {
            return proceedStep;
        }
    }

    public SequenceBuilder<T> sequence(String key) {
        return new SequenceBuilder<>(this, key);
    }

    private StepRecorder<T> skip(String key) {
        return addStep(key, new Step.SkipStep<>());
    }

    private StepRecorder<T> proceed(String key) {
        return addStep(key, new Step.ContinueStep<>());
    }

    private StepRecorder<T> intercept(String key, Consumer<T> step) {
        return addStep(key, new Step.InterceptStep<>(step));
    }

    private synchronized StepRecorder<T> addStep(String key, Step<T> step) {
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

    public static class SequenceBuilder<T> {
        private final StepRecorder<T> recorder;
        private final String key;

        public SequenceBuilder(StepRecorder<T> recorder, String key) {
            this.recorder = recorder;
            this.key = key;
        }

        public SequenceBuilder<T> skip() {
            recorder.skip(key);
            return this;
        }

        public SequenceBuilder<T> proceed() {
            recorder.proceed(key);
            return this;
        }

        public SequenceBuilder<T> intercept(Consumer<T> step) {
            recorder.intercept(key, step);
            return this;
        }

    }


    private static class Sequence<T> {
        private final List<Step<T>> steps = new ArrayList<>();
        private boolean repeat;
        private int playIndex = 0;

        public void addStep(Step<T> step) {
            steps.add(step);
        }

        public Step<T> nextStep() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("No replay steps");
            }
            if (playIndex >= steps.size()) {
                throw new IllegalStateException("Exceeded replay steps");
            }
            var step = steps.get(playIndex);
            if (repeat && playIndex == steps.size() - 1) {
                playIndex = 0;
            } else {
                playIndex++;
            }
            return step;
        }
    }
}
