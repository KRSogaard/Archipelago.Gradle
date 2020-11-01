package build.archipelago.maui.core.output;

public interface OutputWrapper {
    void write(String message);
    void write(String message, Object... args);
    void error(String message);
    void error(String message, Object... args);
}
