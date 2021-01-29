package build.archipelago.buildserver.builder.output;

import build.archipelago.buildserver.common.services.build.logs.PackageLogsService;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.output.OutputWrapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;

@Slf4j
public class S3OutputWrapper implements OutputWrapper {

    private PackageLogsService service;
    private String accountId;
    private String buildId;
    private ArchipelagoPackage pkg;
    private final StringBuilder logs;

    public S3OutputWrapper(PackageLogsService service, String accountId, String buildId, ArchipelagoPackage pkg) {
        this.service = service;
        this.accountId = accountId;
        this.buildId = buildId;
        this.pkg = pkg;

        logs = new StringBuilder();
    }

    @Override
    public void write(String message) {
        this.write(message, new ArrayList<String>());
    }

    @Override
    public void write(String message, Object... args) {
        log.info(String.format(message, args));
        this.addMessage("OUT", message, args);
    }

    @Override
    public void error(String message) {
        this.error(message, new ArrayList<String>());
    }

    @Override
    public void error(String message, Object... args) {
        log.error(String.format(message, args));
        this.addMessage("ERROR", message, args);
    }

    public void upload() {
        service.uploadLog(accountId, buildId, pkg, logs.toString());
    }

    private void addMessage(String prefix, String message, Object... args) {
        synchronized (logs) {
            logs.append(Instant.now().toEpochMilli());
            logs.append(";");
            logs.append(prefix);
            logs.append(";");
            logs.append(String.format(message, args));
            logs.append("\n");
        }
    }
}
