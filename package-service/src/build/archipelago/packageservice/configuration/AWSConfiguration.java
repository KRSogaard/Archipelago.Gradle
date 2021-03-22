package build.archipelago.packageservice.configuration;

import com.amazonaws.auth.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.s3.*;
import lombok.extern.slf4j.Slf4j;
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
        log.info("Creating AWSCredentials using access id '{}'", accessId);
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, accessKey));
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AmazonDynamoDB dynamoDB(
            AWSCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String awsRegion) {
        log.info("Creating DynamoDB Client using region '{}'", awsRegion);
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
        log.info("Creating S3 Client using region '{}'", awsRegion);
        return AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(credentialsProvider)
                .build();
    }
}