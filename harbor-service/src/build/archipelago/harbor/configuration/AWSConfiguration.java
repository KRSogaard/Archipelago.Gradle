package build.archipelago.harbor.configuration;

import com.amazonaws.auth.*;
import com.amazonaws.services.cognitoidp.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.s3.*;
import lombok.extern.slf4j.*;
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
        log.info("Creating AWSCredentialsProvider with accessId '{}'", accessId);
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, accessKey));
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AmazonDynamoDB dynamoDB(
            AWSCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String awsRegion) {
        log.info("Creating AmazonDynamoDB with region '{}'", awsRegion);
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(credentialsProvider)
                .build();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AmazonS3 amazonS3(
            AWSCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String awsRegion) {
        log.info("Creating AmazonS3 with region '{}'", awsRegion);
        return AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(credentialsProvider)
                .build();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AWSCognitoIdentityProvider getCognitoClient(
            AWSCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String awsRegion) {
        log.info("Creating AWSCognitoIdentityProvider with region '{}'", awsRegion);
        return AWSCognitoIdentityProviderClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(credentialsProvider)
                .build();
    }
}