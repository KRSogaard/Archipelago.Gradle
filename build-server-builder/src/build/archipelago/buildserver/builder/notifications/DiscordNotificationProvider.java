package build.archipelago.buildserver.builder.notifications;

import build.archipelago.buildserver.models.BuildPackageDetails;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import com.amazonaws.services.dynamodbv2.xspec.S;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.List;

@Slf4j
public class DiscordNotificationProvider implements NotificationProvider {

    protected final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private Instant lastMessage = null;
    private long msBettenMessages = 250;

    private final String webhook;
    protected HttpClient client;

    public DiscordNotificationProvider(String webhook) {
        this.webhook = webhook;

        client = HttpClient
                .newBuilder()
                .build();

        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void notify(String message) {
        Instant nextMessage = lastMessage == null ? Instant.MIN : lastMessage.plusMillis(msBettenMessages);
        if (Instant.now().isBefore(nextMessage)) {
            log.warn("Too many discord messages, sleeping a bit to avoid issues");
            try {
                Thread.sleep(nextMessage.toEpochMilli() - Instant.now().toEpochMilli());
            } catch (InterruptedException e) {
            }
        }

        lastMessage = Instant.now();
        WebhookJsonRequest messageJson = WebhookJsonRequest.builder()
                .Content(message)
                .Username("Builder")
                .build();
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(new URI(webhook))
                    .header("content-type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(messageJson)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Got unknown error while sending notification to Discord", e);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200: // Ok
            case 201:
            case 202:
            case 203:
            case 204:
                return;
            case 429:
                log.warn("Too many requests to Discord");
            default:
                log.error("Unknown status code " + httpResponse.statusCode() + " while sending notification to discord");
                throw new RuntimeException("Unknown status code " + httpResponse.statusCode() + " while sending notification to discord");
        }
    }

    private StringBuilder getBase(String buildId, String accountId) {
        StringBuilder builder = new StringBuilder();
        builder.append("**[").append(buildId).append("][").append(accountId).append("]** ");
        return builder;
    }

    @Override
    public void buildStarted(String buildId, String accountId, String versionSet, List<BuildPackageDetails> buildPackages, boolean dryRun) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("Has started");
        builder.append("\n```diff");
        for (BuildPackageDetails pkg : buildPackages) {
            builder.append("\n+ ");
            builder.append(pkg.getPackageName());
            builder.append(" [");
            builder.append(pkg.getCommit());
            builder.append("]");
        }
        builder.append("\n```");
        builder.append("\nhttps://giphy.com/gifs/8vIFoKU8s4m4CBqCao");
        notify(builder.toString());
    }

    @Override
    public void stageStarted(String buildId, String accountId, String stage) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("The ");
        builder.append(stage);
        builder.append(" stage has started");
        notify(builder.toString());
    }

    @Override
    public void stageFinished(String buildId, String accountId, String stage) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("The ");
        builder.append(stage);
        builder.append(" stage has finished");
        notify(builder.toString());
    }

    @Override
    public void stageFailed(String buildId, String accountId, String stage) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("The ");
        builder.append(stage);
        builder.append(" stage has failed");
        notify(builder.toString());
    }

    @Override
    public void buildSuccessful(String buildId, String accountId) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("The build was successfully completed");
        builder.append("\nhttps://giphy.com/gifs/borat-great-success-a0h7sAqON67nO");
        notify(builder.toString());
    }

    @Override
    public void buildFailed(String buildId, String accountId) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("The build failed");
        builder.append("\nhttps://giphy.com/gifs/misfitsandmonsters-trutv-misfits-and-monsters-bmm107-MVgLEacpr9KVK172Ne");
        notify(builder.toString());
    }

    @Override
    public void buildError(String buildId, String accountId, RuntimeException exp) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("Build error");
        builder.append("\n> ");
        builder.append(exp.getMessage());
        if (exp.getCause() != null) {
            builder.append("\nCaused by");
            builder.append("\n> ");
            builder.append(exp.getCause().getMessage());
        }
        notify(builder.toString());
    }

    @Override
    public void buildDone(String buildId, String accountId) {
        StringBuilder builder = getBase(buildId, accountId);
        builder.append("Builder is done with the the build. (May have failed or was successful)");
        notify(builder.toString());
    }
}
