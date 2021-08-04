/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.client.common;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Methods for writing to the terminal.
 */
public class Output {
    private static final AttributedStyle RED_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);

    private Output() {
    }

    public static void error(Throwable throwable, Terminal terminal) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        error(sw.toString(), terminal);
    }

    public static void error(String message, Terminal terminal) {
        terminal.writer().println(new AttributedStringBuilder().style(RED_STYLE).append(message).toAnsi());
    }
}
