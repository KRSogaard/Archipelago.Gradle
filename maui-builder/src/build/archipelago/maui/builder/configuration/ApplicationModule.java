package build.archipelago.maui.builder.configuration;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ServiceConfiguration());
        install(new CommandConfiguration());
    }


}
