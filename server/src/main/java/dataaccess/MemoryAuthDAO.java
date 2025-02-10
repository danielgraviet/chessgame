package dataaccess;
import model.auth.AuthData;

import java.util.HashSet;
import java.util.UUID;

public class MemoryAuthDAO implements AuthDAO {
    private final HashSet<AuthData> AuthStorage = new HashSet<>();

    public AuthData getAuthData(String username, String password) throws DataAccessException{
        if (AuthStorage.contains(new AuthData(username, password))){
            return new AuthData(username, password);
        } else {
            throw new DataAccessException("Username or password is incorrect");
        }
    }

    public void addAuthData(AuthData authData) throws DataAccessException {
        if (!AuthStorage.contains(authData)) {
            AuthStorage.add(authData);
        } else {
            throw new DataAccessException("User already exists");
        }
    };


    public void clear() throws DataAccessException {
        AuthStorage.clear();
    };

}
