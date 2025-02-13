package dataaccess;
import model.auth.AuthData;
import java.util.HashMap;
import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.UUID;

public class MemoryAuthDAO implements AuthDAO {
    public HashSet<AuthData> AuthStorage = new HashSet<>();

    // CREATE
    public void addAuthData(AuthData authData) throws DataAccessException {
        if (!AuthStorage.contains(authData)) {
            AuthStorage.add(authData);
        } else {
            throw new DataAccessException("User already exists");
        }
    };

    // READ
    public AuthData getAuthData(String username, String password) throws DataAccessException{
        if (AuthStorage.contains(new AuthData(username, password))){
            return new AuthData(username, password);
        } else {
            throw new DataAccessException("Username or password is incorrect");
        }
    }

    // DELETE
    public boolean removeAuthData(String token) throws DataAccessException {
        // this is checking if the token is in the storage, and removing if it is.
        for (AuthData authData : AuthStorage) {
            if (authData.authToken().equals(token)) {
                AuthStorage.remove(authData);
                // this means it has successfully found and removed the token.
                return true;
            }
        }
        throw new DataAccessException("Invalid AuthToken.");
    }

    public AuthData getUser(String token) throws DataAccessException {
        return AuthStorage.stream().filter(authData -> authData.authToken().equals(token)).findFirst().orElse(null);
    }

    public void clear() throws DataAccessException {
        AuthStorage.clear();
    };

}
