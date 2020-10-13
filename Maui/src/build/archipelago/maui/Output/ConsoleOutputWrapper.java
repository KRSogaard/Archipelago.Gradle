package build.archipelago.maui.Output;

public class ConsoleOutputWrapper implements OutputWrapper {
    @Override
    public void write(String message) {
        System.out.println(message);
    }

    @Override
    public void write(String message, Object... args) {
        System.out.println(String.format(message, args));
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }

    @Override
    public void error(String message, Object... args) {
        System.err.println(String.format(message, args));
    }
}
