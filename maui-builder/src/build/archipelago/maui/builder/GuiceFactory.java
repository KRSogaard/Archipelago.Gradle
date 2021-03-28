package build.archipelago.maui.builder;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

public class GuiceFactory implements IFactory {
    private final Injector injector;

    public GuiceFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <K> K create(Class<K> aClass) throws Exception {
        try {
            return injector.getInstance(aClass);
        } catch (ConfigurationException ex) { // no implementation found in Guice configuration
            return CommandLine.defaultFactory().create(aClass); // fallback if missing
        }
    }
}