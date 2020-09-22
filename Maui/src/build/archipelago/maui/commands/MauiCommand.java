package build.archipelago.maui.commands;

import build.archipelago.maui.commands.workspace.WorkspaceCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "Maui", mixinStandardHelpOptions = true, subcommands = {WorkspaceCommand.class, BuildCommand.class})
public class MauiCommand  {

}
