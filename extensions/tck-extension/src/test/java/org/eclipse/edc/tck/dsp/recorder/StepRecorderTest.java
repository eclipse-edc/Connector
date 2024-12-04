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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StepRecorderTest {
    private static final String KEY = "test";

    @Test
    void verifyRecordReplay() {
        var recorder = new StepRecorder<Void>();
        var counter = new int[1];
        recorder.record(KEY, i -> assertThat(counter[0]++).isEqualTo(0));
        recorder.record(KEY, i -> assertThat(counter[0]++).isEqualTo(1));

        recorder.playNext(KEY, null);
        recorder.playNext(KEY, null);
        assertThat(counter[0]).isEqualTo(2);
    }

    @Test
    void verifyRepeat() {
        var recorder = new StepRecorder<Void>();
        var counter = new int[1];
        recorder.record(KEY, i -> counter[0]++);
        recorder.repeat();

        for (int i = 0; i < 4; i++) {
            recorder.playNext(KEY, null);
        }
        assertThat(counter[0]).isEqualTo(4);
    }

    @Test
    void verifyNoRepeat() {
        var recorder = new StepRecorder<Void>();
        var counter = new int[1];
        recorder.record(KEY, i -> counter[0]++);

        recorder.playNext(KEY, null);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> recorder.playNext(KEY, null));
        assertThat(counter[0]).isEqualTo(1);
    }
}