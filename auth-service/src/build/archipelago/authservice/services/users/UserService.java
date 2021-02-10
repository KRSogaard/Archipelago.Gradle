package build.archipelago.authservice.services.users;

import build.archipelago.authservice.services.users.exceptions.*;

public interface UserService {
    String authenticate(String email, String password) throws UserNotFoundException;
}
