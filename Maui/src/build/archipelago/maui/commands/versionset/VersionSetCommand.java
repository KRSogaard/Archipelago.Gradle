package build.archipelago.maui.commands.versionset;

import picocli.CommandLine;

@CommandLine.Command(name = "versionset", aliases = {"vs"}, mixinStandardHelpOptions = true, description = "Manipulation of Version-Sets",
        subcommands = {VersionSetBuildCommand.class})
public class VersionSetCommand {
}
