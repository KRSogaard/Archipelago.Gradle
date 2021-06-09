package build.archipelago.authservice.configuration;

import com.amazonaws.auth.*;
import com.amazonaws.services.dynamodbv2.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class AWSConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AWSCredentialsProvider credentialsProvider(
            @Value("${aws.access.id}") String accessId,
            @Value("${aws.access.key}") String accessKey) {
        log.info("Using AWS id: {}", accessId)
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, accessKey));
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AmazonDynamoDB dynamoDB(
            AWSCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String awsRegion) {
        log.info("Using AWS region: {}", awsRegion)
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(credentialsProvider)
                .build();
    }
}