package build.archipelago.authservice.models.rest;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OpenIdConfigRestResponse {
    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String jwksUri;
    private List<String> scopesSupported;
    private List<String> responseTypesSupported;
    private List<String> tokenEndpointAuthMethodsSupported;
    private List<String> responseModesSupported;
    private List<String> grantTypesSupported;
    private List<String> subjectTypesSupported;
    private List<String> idTokenSigningAlgValuesSupported;



}
