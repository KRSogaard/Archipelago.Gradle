package build.archipelago.maui.commands.packages;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "package", aliases = {"pkg"}, mixinStandardHelpOptions = true, description = "packages",
        subcommands = {PackageCreateCommand.class})
public class PackageCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
