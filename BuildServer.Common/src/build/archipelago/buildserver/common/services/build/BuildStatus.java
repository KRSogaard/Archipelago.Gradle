package build.archipelago.buildserver.common.services.build;

public enum BuildStatus {
    WAITING("waiting"),
    PREPARING("preparing"),
    IN_PROGRESS("in-progress"),
    FINISHED("finished"),
    FAILED("failed");

    private final String status;

    private BuildStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
