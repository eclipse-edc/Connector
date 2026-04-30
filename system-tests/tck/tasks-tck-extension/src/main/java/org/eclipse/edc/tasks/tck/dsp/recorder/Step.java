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

import java.util.function.Consumer;

public sealed interface Step<T> {

    final class ContinueStep<T> implements Step<T> {

    }

    final class SkipStep<T> implements Step<T> {

    }

    final class InterceptStep<T> implements Step<T>, Consumer<T> {

        private final Consumer<T> consumer;

        public InterceptStep(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(T t) {
            consumer.accept(t);
        }
    }
}
