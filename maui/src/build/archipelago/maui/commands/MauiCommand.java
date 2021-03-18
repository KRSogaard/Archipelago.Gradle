package build.archipelago.maui.commands;

import build.archipelago.maui.commands.packages.PackageCommand;
import build.archipelago.maui.commands.versionset.VersionSetCommand;
import build.archipelago.maui.commands.workspace.WorkspaceCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "Maui", mixinStandardHelpOptions = true, subcommands = {
        WorkspaceCommand.class,
        VersionSetCommand.class,
        PackageCommand.class,
        BuildCommand.class,
        PathCommand.class,
        RecursiveCommand.class,
        CleanCommand.class,
        VersionCommand.class,
        AuthCommand.class})
public class MauiCommand  implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
