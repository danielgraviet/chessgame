package dataaccess;
import model.auth.AuthData;

public interface AuthDAO {
    AuthData getUser(String token) throws DataAccessException;
    void addAuthData(AuthData authData) throws DataAccessException;
    boolean removeAuthData(String token) throws DataAccessException;
    void clear() throws DataAccessException;
}
