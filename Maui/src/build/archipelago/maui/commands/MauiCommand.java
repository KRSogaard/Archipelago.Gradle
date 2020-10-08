package build.archipelago.maui.commands;

import build.archipelago.maui.commands.packages.PackageCommand;
import build.archipelago.maui.commands.workspace.WorkspaceCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "Maui", mixinStandardHelpOptions = true, subcommands = {
        WorkspaceCommand.class,
        PackageCommand.class,
        BuildCommand.class,
        PathCommand.class,
        RecursiveCommand.class,
        CleanCommand.class})
public class MauiCommand  {

}
