package build.archipelago.maui.builder.configuration;

import com.google.inject.AbstractModule;

public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ServiceConfiguration());
        install(new CommandConfiguration());
    }


}
