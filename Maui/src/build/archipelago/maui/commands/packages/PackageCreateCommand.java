package build.archipelago.maui.commands.packages;

import build.archipelago.maui.core.actions.PackageCreateAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new package")
public class PackageCreateCommand implements Callable<Integer> {

    private PackageCreateAction packageCreateAction;

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;
    @CommandLine.Option(names = { "-d", "--desc"})
    private String description;

    public PackageCreateCommand(PackageCreateAction packageCreateAction) {
        this.packageCreateAction = packageCreateAction;
    }

    @Override
    public Integer call() throws Exception {
        if (packageCreateAction.createPackageName(name, description)) {
            return 0;
        }
        return 1;
    }
}
