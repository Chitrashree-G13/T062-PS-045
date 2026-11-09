package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommandReader {

    public List<String> readCommands(String fileName)
            throws IOException {

        List<String> commands = new ArrayList<>();

        List<String> lines = Files.readAllLines(Path.of(fileName));

        for (String line : lines) {

            String command = line.trim();

            if (command.isEmpty()) {
                continue;
            }

            if (command.startsWith("#")) {
                continue;
            }

            commands.add(command);
        }

        return commands;
    }
}