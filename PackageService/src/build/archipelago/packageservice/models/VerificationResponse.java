package build.archipelago.packageservice.models;

import lombok.*;

import java.util.List;

@Builder
@Value
public class VerificationResponse {
    private List<String> missing;
}
