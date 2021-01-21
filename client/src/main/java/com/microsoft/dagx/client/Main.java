package com.microsoft.dagx.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import com.microsoft.dagx.client.command.assembly.RootAssemblyFactory;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;
import java.util.Map;

import static com.microsoft.dagx.client.common.Commands.subCommands;
import static com.microsoft.dagx.client.common.Output.error;

public class Main {
    private static final String DEFAULT_ENDPOINT = "http://localhost:8080";

    private static final AttributedStyle BLUE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

    public static void main(String... args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        TerminalBuilder terminalBuilder = TerminalBuilder.builder().dumb(true);
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();

        Terminal terminal = terminalBuilder.build();

        RootAssemblyFactory.Assembly assembly = RootAssemblyFactory.create();
        Map<String, CommandExecutor> executors = assembly.getExecutors();

        LineReader reader = readerBuilder.terminal(terminal).completer(assembly.getCompleter()).build();

        AttributedStringBuilder prompt = new AttributedStringBuilder().style(BLUE_STYLE).append("dagx> ");

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
            ExecutionContext.Builder contextBuilder = ExecutionContext.Builder.newInstance(new ObjectMapper()).endpointUrl(DEFAULT_ENDPOINT);
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
