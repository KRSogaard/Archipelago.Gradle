package build.archipelago.maui;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.maui.commands.MauiCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
public class Application {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(MauiCommand.class, new GuiceFactory());
        commandLine.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
                if (ex instanceof UnauthorizedException) {
                    System.err.println("You are currently not authorized, please use the \"maui auth\" command");
                } else {
                    throw ex;
                }
                return -1;
            }
        });
        System.exit(commandLine.execute(args));
    }


}