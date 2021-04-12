package build.archipelago.authservice.client;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.common.rest.models.errors.*;
import com.google.common.base.*;

import java.util.*;

public class AuthExceptionHandler {
    public static final String TYPE_TOKEN_UNAUTHORIZED = "token/unauthorized";
    public static final String TYPE_TOKEN_NOT_VALID = "token/notValid";
    public static final String TYPE_TOKEN_EXPIRED = "token/expired";
    public static final String TYPE_TOKEN_NOT_FOUND = "token/notFound";
    public static final String TYPE_KEY_NOT_FOUND = "key/notFound";
    public static final String TYPE_CLIENT_NOT_FOUND = "client/notFound";
    public static final String TYPE_CLIENT_SECRET_REQUIRED = "client/secretRequired";
    public static final String TYPE_GRANT_INVALID = "grant/invalid";
    public static final String TYPE_DEVICE_CODE_NOT_FOUND = "device/codeNotFound";
    public static final String TYPE_USER_NOT_FOUND = "user/notFound";
    public static final String TYPE_USER_EXISTS = "user/exists";
    public static final String TYPE_ACCESS_KEY_NOT_FOUND = "accessKey/notFound";
    public static final String TYPE_AUTHORIZATION_PENDING = "authorization_pending";



    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(UserNotFoundException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_USER_NOT_FOUND)
                .title("The user was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    put("email", exp.getEmail());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(UnauthorizedAuthTokenException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_TOKEN_UNAUTHORIZED)
                .title("The token is unauthorized")
                .status(403)
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(TokenExpiredException exp) {
        Map<String, Object> data = new HashMap<>();
        if (!Strings.isNullOrEmpty(exp.getDeviceCode())) {
            data.put("device_code", exp.getDeviceCode());
        }
        return ProblemDetailRestResponse.builder()
                .error(TYPE_TOKEN_EXPIRED)
                .title("The token has expired")
                .status(403)
                .detail(exp.getMessage())
                .data(data);
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(TokenNotFoundException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_TOKEN_NOT_FOUND)
                .title("Token not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    put("token", exp.getToken());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(KeyNotFoundException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_KEY_NOT_FOUND)
                .title("The key was not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    put("kid", exp.getKid());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(ClientNotFoundException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_CLIENT_NOT_FOUND)
                .title("The client not found")
                .status(404)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    put("client_id", exp.getClientId());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(DeviceCodeNotFoundException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_DEVICE_CODE_NOT_FOUND)
                .title("The device code not found")
                .status(404)
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(ClientSecretRequiredException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_CLIENT_SECRET_REQUIRED)
                .title("A client secret is required for this client")
                .status(406)
                .detail(exp.getMessage())
                .data(new HashMap<>() {{
                    put("client_id", exp.getClientId());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(InvalidGrantTypeException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_GRANT_INVALID)
                .title("The key was not valid")
                .status(406)
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(TokenNotValidException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_TOKEN_NOT_VALID)
                .title("The key was not valid")
                .status(406)
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(AuthorizationPendingException exp) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_AUTHORIZATION_PENDING)
                .title("The user has yet to approve the device code")
                .status(403)
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(UserExistsException ex) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_USER_EXISTS)
                .title("The user with that email already exits")
                .status(409)
                .detail(ex.getMessage())
                .data(new HashMap<>() {{
                    put("email", ex.getEmail());
                }});
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(AccessKeyNotFound ex) {
        return ProblemDetailRestResponse.builder()
                .error(TYPE_ACCESS_KEY_NOT_FOUND)
                .title("The access key was not found")
                .status(403)
                .detail(ex.getMessage());
    }

    public static Object createException(ProblemDetailRestResponse problem) {
        switch (problem.getError()) {
            case TYPE_USER_NOT_FOUND:
                return new UserNotFoundException((String) problem.getData().get("email"));
            case TYPE_TOKEN_NOT_FOUND:
                return new TokenNotFoundException((String) problem.getData().get("token"));
            case TYPE_TOKEN_EXPIRED:
                String device_code;
                if (problem.getData().containsKey("device_code")) {
                    device_code = (String) problem.getData().get("device_code");
                    return new TokenExpiredException(device_code);
                }
                return new TokenNotFoundException(null);
            case TYPE_USER_EXISTS:
                return new UserExistsException((String) problem.getData().get("email"));
            case TYPE_ACCESS_KEY_NOT_FOUND:
                return new AccessKeyNotFound();
            default:
                throw new RuntimeException(problem.getError() + " was not a known auth error");
        }
    }
}
