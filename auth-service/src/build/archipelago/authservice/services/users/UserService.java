package build.archipelago.authservice.services.users;

import build.archipelago.authservice.services.users.exceptions.*;
import build.archipelago.authservice.services.users.models.*;

public interface UserService {
    String authenticate(String email, String password) throws UserNotFoundException;
    String createUser(UserModel build) throws UserExistsException;
}
