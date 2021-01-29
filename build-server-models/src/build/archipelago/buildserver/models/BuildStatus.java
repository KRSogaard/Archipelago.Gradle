package build.archipelago.buildserver.models;

public enum BuildStatus {
    WAITING("waiting"),
    IN_PROGRESS("in-progress"),
    FINISHED("finished"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String status;

    private BuildStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static BuildStatus getEnum(String value) {
        if (WAITING.getStatus().equalsIgnoreCase(value)) {
            return WAITING;
        }
        if (IN_PROGRESS.getStatus().equalsIgnoreCase(value)) {
            return IN_PROGRESS;
        }
        if (FINISHED.getStatus().equalsIgnoreCase(value)) {
            return FINISHED;
        }
        if (FAILED.getStatus().equalsIgnoreCase(value)) {
            return FAILED;
        }
        if (SKIPPED.getStatus().equalsIgnoreCase(value)) {
            return SKIPPED;
        }
        throw new IllegalArgumentException(value + " is not a valid enum");
    }
}
