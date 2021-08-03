/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.client;

import org.eclipse.edc.client.command.CommandExecutor;
import org.eclipse.edc.client.command.CommandResult;
import org.eclipse.edc.client.command.ExecutionContext;
import org.eclipse.edc.client.command.assembly.RootAssemblyFactory;
import org.eclipse.edc.client.runtime.EdcConnectorClientRuntime;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.client.common.Commands.subCommands;
import static org.eclipse.edc.client.common.Output.error;

public class Main {
    private static final String DEFAULT_ENDPOINT = "http://localhost:8181";

    private static final AttributedStyle BLUE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

    public static void main(String... args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        EdcConnectorClientRuntime runtime = EdcConnectorClientRuntime.Builder.newInstance().build();
        runtime.start();

        TerminalBuilder terminalBuilder = TerminalBuilder.builder().dumb(true);
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();

        Terminal terminal = terminalBuilder.build();

        RootAssemblyFactory.Assembly assembly = RootAssemblyFactory.create();
        Map<String, CommandExecutor> executors = assembly.getExecutors();

        LineReader reader = readerBuilder.terminal(terminal).completer(assembly.getCompleter()).build();

        Main.inputLoop(terminal, reader, executors, runtime);

        runtime.shutdown();
    }

    private static void inputLoop(Terminal terminal, LineReader reader, Map<String, CommandExecutor> executors, EdcConnectorClientRuntime runtime) {
        AttributedStringBuilder prompt = new AttributedStringBuilder().style(Main.BLUE_STYLE).append("edc> ");

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
                CommandResult result = executor.execute(context);
                if (result.error()) {
                    error(result.getMessage(), terminal);
                }
            } catch (Exception e) {
                error(e, terminal);
            }
        }
    }

}
