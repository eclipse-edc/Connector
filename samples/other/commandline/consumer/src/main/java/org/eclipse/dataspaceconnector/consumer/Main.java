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

package org.eclipse.dataspaceconnector.consumer;

import org.eclipse.dataspaceconnector.consumer.command.CommandExecutor;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;
import org.eclipse.dataspaceconnector.consumer.command.assembly.RootAssemblyFactory;
import org.eclipse.dataspaceconnector.consumer.runtime.EdcConnectorConsumerRuntime;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.consumer.common.Commands.subCommands;
import static org.eclipse.dataspaceconnector.consumer.common.Output.error;

public class Main {
    private static final String DEFAULT_ENDPOINT = "http://localhost:8181";

    private static final AttributedStyle BLUE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

    public static void main(String... args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        var runtime = new EdcConnectorConsumerRuntime();
        runtime.start();

        TerminalBuilder terminalBuilder = TerminalBuilder.builder().dumb(true);
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();

        Terminal terminal = terminalBuilder.build();

        RootAssemblyFactory.Assembly assembly = RootAssemblyFactory.create();
        Map<String, CommandExecutor> executors = assembly.getExecutors();

        LineReader reader = readerBuilder.terminal(terminal).completer(assembly.getCompleter()).build();

        Main.inputLoop(terminal, reader, executors, runtime);
    }

    private static void inputLoop(Terminal terminal, LineReader reader, Map<String, CommandExecutor> executors, EdcConnectorConsumerRuntime runtime) {
        AttributedStringBuilder prompt = new AttributedStringBuilder().style(Main.BLUE_STYLE).append("dataspaceconnector> ");

        while (true) {
            String line = reader.readLine(prompt.toAnsi());
            if (line.trim().equalsIgnoreCase("q") || line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            ParsedLine parsedLine = reader.getParser().parse(line, 0);
            List<String> cmds = parsedLine.words();
            if (cmds.isEmpty()) {
                continue;
            }
            String cmd = cmds.get(0);
            if (cmd.length() == 0) {
                continue;
            }

            CommandExecutor executor = executors.get(cmd);
            if (executor == null) {
                error("Unrecognized command: " + cmd, terminal);
                continue;
            }

            List<String> subCmds = subCommands(cmds);
            ExecutionContext.Builder contextBuilder = ExecutionContext.Builder.newInstance().runtime(runtime).endpointUrl(Main.DEFAULT_ENDPOINT);
            ExecutionContext context = contextBuilder.terminal(terminal).params(subCmds).build();

            try {
                Result<String> result = executor.execute(context);
                if (result.failed()) {
                    error(result.getFailureMessages().stream().findFirst().orElse(""), terminal);
                }
            } catch (Exception e) {
                error(e, terminal);
            }
        }
    }

}
