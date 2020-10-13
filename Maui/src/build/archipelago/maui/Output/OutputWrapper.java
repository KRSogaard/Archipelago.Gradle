package build.archipelago.maui.Output;

public interface OutputWrapper {
    void write(String message);
    void write(String message, Object... args);
    void error(String message);
    void error(String message, Object... args);
}
