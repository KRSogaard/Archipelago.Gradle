package build.archipelago.buildserver.models;

import lombok.*;

@Builder
@Value
public class LogFileResponse {
    private String signedUrl;
}
