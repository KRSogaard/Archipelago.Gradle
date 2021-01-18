package build.archipelago.harbor.models.build;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class NewBuildRestResponse {
    private String buildId;
}
