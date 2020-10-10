package build.archipelago.packageservice.client.rest.models;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RestVerificationResponse {
    private List<String> missing;
}
