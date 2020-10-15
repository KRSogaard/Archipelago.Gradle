package build.archipelago.buildserver.common.services.build;

public enum OutputType {
    INFO("info"),
    ERROR("error");

    private final String type;

    private OutputType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
