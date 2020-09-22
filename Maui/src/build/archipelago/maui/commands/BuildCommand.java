package build.archipelago.maui.commands;

import build.archipelago.maui.commands.workspace.WorkspaceSyncCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "Build a package")
public class BuildCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.printf("Burr Burr i am building.");
        return 0;
    }
}
