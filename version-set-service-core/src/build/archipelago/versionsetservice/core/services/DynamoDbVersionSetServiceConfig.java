package build.archipelago.versionsetservice.core.services;

import lombok.*;

@Builder
@Value
public class DynamoDbVersionSetServiceConfig {
    private String versionSetTable;
    private String versionSetRevisionTable;
    private String versionSetCallbacksTable;
}
