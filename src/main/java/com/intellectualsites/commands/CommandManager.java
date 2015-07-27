package com.intellectualsites.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.ConsolePlayer;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;

@SuppressWarnings("unused")
public class CommandManager {

    protected final ConcurrentHashMap<String, Command> commands;
    protected final Character initialCharacter;

    public CommandManager() {
        this('/', new ArrayList<Command>());
    }

    public CommandManager(Character initialCharacter, List<Command> commands) {
        this.commands = new ConcurrentHashMap<>();
        for (Command command : commands) {
            addCommand(command);
        }
        this.initialCharacter = initialCharacter;
    }

    final public void addCommand(final Command command) {
        this.commands.put(command.getCommand().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            this.commands.put(alias.toLowerCase(), command);
        }
    }

    final public boolean createCommand(final Command command) {
        try {
            command.create();
        } catch(final Exception e) {
            e.printStackTrace();
            return false;
        }
        if (command.getCommand() != null) {
            addCommand(command);
            return true;
        }
        return false;
    }

    final public ArrayList<Command> getCommands() {
        ArrayList<Command> result = new ArrayList<>(this.commands.values());
        Collections.sort(result, new Comparator<Command>() {
            @Override
            public int compare(Command a, Command b) {
                if (a == b) {
                    return 0;
                }
                if (a == null) {
                    return -1;
                }
                if (b == null) {
                    return 1;
                }
                return a.getCommand().compareTo(b.getCommand());
            }
        });
        return result;
    }

    public int handle(PlotPlayer plr, String input) {
        if (initialCharacter != null && !input.startsWith(initialCharacter + "")) {
            return CommandHandlingOutput.NOT_COMMAND;
        }
        input = initialCharacter == null ? input : input.substring(1);
        String[] parts = input.split(" ");
        String[] args;
        String command = parts[0].toLowerCase();
        if (parts.length == 1) {
            args = new String[0];
        } else {
            args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
        }
        Command cmd = null;
        cmd = commands.get(command);
        if (cmd == null) {
            return CommandHandlingOutput.NOT_FOUND;
        }
        if (!cmd.getRequiredType().allows(plr)) {
            return CommandHandlingOutput.CALLER_OF_WRONG_TYPE;
        }
        if (!plr.hasPermission(cmd.getPermission())) {
            return CommandHandlingOutput.NOT_PERMITTED;
        }
        Argument[] requiredArguments = cmd.getRequiredArguments();
        if (requiredArguments != null && requiredArguments.length > 0) {
            boolean success = true;
            if (args.length < requiredArguments.length) {
                success = false;
            } else {
                for (int i = 0; i < requiredArguments.length; i++) {
                    if (requiredArguments[i].parse(args[i]) == null) {
                        success = false;
                        break;
                    }
                }
            }
            if (!success) {
                String usage = cmd.getUsage();
                C.COMMAND_SYNTAX.send(plr, cmd.getUsage());
                return CommandHandlingOutput.WRONG_USAGE;
            }
        }
        try {
            boolean a = cmd.onCommand(plr, args);
            if (!a) {
                String usage = cmd.getUsage();
                if (usage != null && !usage.isEmpty()) {
                    MainUtil.sendMessage(plr, usage);
                }
                return CommandHandlingOutput.WRONG_USAGE;
            }
        } catch(final Throwable t) {
            t.printStackTrace();
            return CommandHandlingOutput.ERROR;
        }
        return CommandHandlingOutput.SUCCESS;
    }

    final public char getInitialCharacter() {
        return this.initialCharacter;
    }
}