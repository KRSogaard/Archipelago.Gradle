package build.archipelago.buildserver.builder.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WebhookJsonRequest {
    @JsonProperty("content")
    private String Content;
    @JsonProperty("username")
    private String Username;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
