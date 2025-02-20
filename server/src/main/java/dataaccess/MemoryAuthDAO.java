package dataaccess;
import model.auth.AuthData;
import java.util.HashSet;

public class MemoryAuthDAO implements AuthDAO {
    public HashSet<AuthData> authStorage = new HashSet<>();

    // CREATE
    public void addAuthData(AuthData authData) throws DataAccessException {
        if (!authStorage.contains(authData)) {
            authStorage.add(authData);
        } else {
            throw new DataAccessException("User already exists");
        }
    }
    // DELETE
    public boolean removeAuthData(String token) throws DataAccessException {
        // this is checking if the token is in the storage, and removing if it is.
        for (AuthData authData : authStorage) {
            if (authData.authToken().equals(token)) {
                authStorage.remove(authData);
                // this means it has successfully found and removed the token.
                return true;
            }
        }
        throw new DataAccessException("Invalid AuthToken.");
    }

    public AuthData getUser(String token) {
        // notes on stream
        // .stream() converts authStorage hashmap into a stream. streams are good for applying functional operations on objects
        // filter method then will process each element in stream.
        // the lambda function describes what the filter should do, and how it should equal the token.
        // finds first match, or returns a null.
        // think of a list comprehension in python.
        return authStorage.stream().filter(authData -> authData.authToken().equals(token)).findFirst().orElse(null);
    }

    public void clear() throws DataAccessException {
        authStorage.clear();
    }

}
