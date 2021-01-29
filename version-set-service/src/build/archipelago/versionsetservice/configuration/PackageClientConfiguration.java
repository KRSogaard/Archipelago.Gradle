package build.archipelago.versionsetservice.configuration;

import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import com.google.common.base.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class PackageClientConfiguration {

    @Bean
    public PackageServiceClient getPackageServiceClient(
            @Value("${endpoints.package-service}") String endpoint,
            @Value("${oauth.client-id}") String clientId,
            @Value("${oauth.client-secret}") String clientSecret
    ) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpoint), "Package Service endpoint can not be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(clientId), "OAuth client id can not be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(clientSecret), "OAuth client secret can not be null or empty");
        return new RestPackageServiceClient(endpoint, clientId, clientSecret);
    }

}
