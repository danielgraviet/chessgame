package dataaccess;
import model.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.UUID;

public class MemoryAuthDAO implements AuthDAO {
    private static final Logger log = LoggerFactory.getLogger(MemoryAuthDAO.class);
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
        // notes on stream
        // .stream() converts AuthStorage hashmap into a stream. streams are good for applying functional operations on objects
        // filter method then will process each element in stream.
        // the lambda function describes what the filter should do, and how it should equal the token.
        // finds first match, or returns a null.
        // think of a list comprehension in python.
        return AuthStorage.stream().filter(authData -> authData.authToken().equals(token)).findFirst().orElse(null);
    }

    public void clear() throws DataAccessException {
        AuthStorage.clear();
    };

}
