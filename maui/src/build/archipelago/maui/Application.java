package build.archipelago.maui;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotBuiltException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.exceptions.VersionSetNotSyncedException;
import build.archipelago.maui.commands.MauiCommand;
import build.archipelago.maui.configuration.ApplicationModule;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.google.inject.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import picocli.CommandLine;

import java.util.stream.Collectors;

@Slf4j
public class Application {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new ApplicationModule());
        CommandLine commandLine = new CommandLine(MauiCommand.class, new GuiceFactory(injector));
        commandLine.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
                if (ex instanceof UnauthorizedException) {
                    System.err.println("You are currently not authorized, please use the \"maui auth\" command");
                } else if (ex instanceof PackageNotBuiltException) {
                    String pkgName = ((PackageNotBuiltException) ex).getPkg().getName();
                    System.err.println(String.format("The package %s has not been built", pkgName));
                } else if (ex instanceof VersionSetNotSyncedException) {
                    System.err.println("The version set has not been synced, please run \"maui ws sync\"");
                } else if (ex instanceof PackageNotFoundException) {
                    PackageNotFoundException e = (PackageNotFoundException) ex;
                    if (e.getPackageName() != null) {
                        System.err.println(String.format("The package \"%s\" dose not exists", e.getPackageName()));
                    } else if (((PackageNotFoundException) ex).getPackages().size() == 1) {
                        System.err.println(String.format("The package \"%s\" dose not exists", e.getPackages().get(0).getName()));
                    } else {
                        String packageNames = e.getPackages().stream()
                                .map(ArchipelagoPackage::getName)
                                .collect(Collectors.joining(", "));
                        System.err.println(String.format("The package [%s] dose not exists", packageNames));
                    }
                } else {
                    log.error("Got an uncaught error", ex);
                    System.err.println(ex.getMessage());
                }
                return -1;
            }
        });
        System.exit(commandLine.execute(args));
    }
}
