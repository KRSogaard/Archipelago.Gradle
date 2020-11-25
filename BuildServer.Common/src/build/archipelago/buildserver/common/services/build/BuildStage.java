package build.archipelago.buildserver.common.services.build;

public enum BuildStage {
    PREPARE("prepare"),
    PACKAGES("packages"),
    PUBLISHING("publishing");

    private final String stage;

    private BuildStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    public static BuildStage getEnum(String value) {
        if (PREPARE.getStage().equalsIgnoreCase(value)) {
            return PREPARE;
        }
        if (PACKAGES.getStage().equalsIgnoreCase(value)) {
            return PACKAGES;
        }
        if (PUBLISHING.getStage().equalsIgnoreCase(value)) {
            return PUBLISHING;
        }
        throw new IllegalArgumentException(value + " is not a valid enum");
    }
}
