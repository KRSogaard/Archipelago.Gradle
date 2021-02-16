package build.archipelago.authservice.models.rest;

import lombok.*;

@Data
@Builder
public class JWKRestResponse {
    private String use;
    private String kty;
    private String kid;
    private String alg;
    private String n;
    private String e;
}
