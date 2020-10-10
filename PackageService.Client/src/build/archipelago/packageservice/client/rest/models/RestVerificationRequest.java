package build.archipelago.packageservice.client.rest.models;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RestVerificationRequest {
    private List<String> packages;
}
