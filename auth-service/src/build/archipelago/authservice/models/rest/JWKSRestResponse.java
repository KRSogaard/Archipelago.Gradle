package build.archipelago.authservice.models.rest;

import lombok.*;

import java.util.List;

@Data
@Builder
public class JWKSRestResponse {
    private List<JWKRestResponse> keys;
}
