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

package org.eclipse.edc.tck.dsp;

import org.testcontainers.containers.output.OutputFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TckTestReporter implements Consumer<OutputFrame> {

    private final List<String> failures = new ArrayList<>();
    private final Pattern failedRegex = Pattern.compile("FAILED: (\\w+:.*)");

    public TckTestReporter() {
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        var line = outputFrame.getUtf8String();
        var failed = failedRegex.matcher(line);
        if (failed.find()) {
            failures.add(failed.group(1));
        }
    }

    public List<String> failures() {
        return new ArrayList<>(failures);
    }

}
