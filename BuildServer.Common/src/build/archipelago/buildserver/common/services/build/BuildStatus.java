package build.archipelago.buildserver.common.services.build;

public enum BuildStatus {
    WAITING("waiting"),
    PREPARING("preparing"),
    BUILDING("building"),
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
