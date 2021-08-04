/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
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

    public static void error(Throwable throwable, Terminal terminal) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        error(sw.toString(), terminal);
    }

    public static void error(String message, Terminal terminal) {
        terminal.writer().println(new AttributedStringBuilder().style(RED_STYLE).append(message).toAnsi());
    }

    private Output() {
    }
}
