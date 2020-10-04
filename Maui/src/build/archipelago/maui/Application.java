package build.archipelago.maui;

import build.archipelago.maui.commands.MauiCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
public class Application {

    public static void main(String[] args) {
        System.exit(new CommandLine(MauiCommand.class, new GuiceFactory()).execute(args));
    }


}
