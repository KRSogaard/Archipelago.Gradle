package build.archipelago.buildserver.models.rest;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogFileRestResponse {
    private String signedUrl;
}
