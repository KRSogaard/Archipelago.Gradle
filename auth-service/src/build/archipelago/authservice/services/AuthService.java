package build.archipelago.authservice.services;

public interface AuthService {
    String authenticate(String email, String password);
}
