package build.archipelago.maui.commands.versionset;

import build.archipelago.maui.commands.workspace.*;
import picocli.CommandLine;

@CommandLine.Command(name = "versionset", aliases = {"vs"}, mixinStandardHelpOptions = true, description = "Manipulation of Version-Sets",
        subcommands = {VersionSetBuildCommand.class})
public class VersionSetCommand {
}
