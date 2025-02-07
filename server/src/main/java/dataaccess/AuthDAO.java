package dataaccess;
import model.auth.AuthData;

import javax.xml.crypto.Data;

public interface AuthDAO {
    AuthData getAuthData(String username, String password) throws DataAccessException;
    void addAuthData(AuthData authData) throws DataAccessException;
    void deleteAuthData(String authToken) throws DataAccessException;
    void clear() throws DataAccessException;
}
