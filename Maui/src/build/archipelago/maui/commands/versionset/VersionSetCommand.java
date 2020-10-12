package build.archipelago.maui.commands.versionset;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "versionset", aliases = {"vs"}, mixinStandardHelpOptions = true, description = "Manipulation of Version-Sets",
        subcommands = {VersionSetBuildCommand.class})
public class VersionSetCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
