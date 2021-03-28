package build.archipelago.buildserver.builder;

import java.time.Instant;

public class StageLog {
    private final StringBuilder logs;
    private boolean hasLogs;

    public StageLog() {
        this.logs = new StringBuilder();
        this.hasLogs = false;
    }

    public void addError(String message, Object... args) {

        addMessage("ERROR", message, args);
    }

    public void addInfo(String message, Object... args) {
        addMessage("INFO", message, args);
    }

    public String getLogs() {
        return logs.toString();
    }

    private void addMessage(String prefix, String message, Object... args) {
        synchronized (logs) {
            hasLogs = true;
            logs.append(Instant.now().toEpochMilli());
            logs.append(";");
            logs.append(prefix);
            logs.append(": ");
            logs.append(String.format(message, args));
            logs.append("\n");
        }
    }

    public boolean hasLogs() {
        return hasLogs;
    }
}
