package build.archipelago.maui;
import build.archipelago.maui.commands.MauiCommand;
import build.archipelago.maui.commands.workspace.WorkspaceCommand;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

    private final MauiCommand mauiCommand;

    private final IFactory factory; // auto-configured to inject PicocliSpringFactory

    private int exitCode;

    public MyApplicationRunner(MauiCommand mauiCommand, IFactory factory) {
        this.mauiCommand = mauiCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(mauiCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}