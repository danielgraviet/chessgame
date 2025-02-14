package dataaccess;
import model.auth.AuthData;

import javax.xml.crypto.Data;

public interface AuthDAO {
    AuthData getAuthData(String username, String password) throws DataAccessException;
    AuthData getUser(String token) throws DataAccessException;
    void addAuthData(AuthData authData) throws DataAccessException;
    boolean removeAuthData(String token) throws DataAccessException;
    void clear() throws DataAccessException;
}
