package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class VerificationRestResponse {
    private List<String> missing;
}
