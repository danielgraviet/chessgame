package dataaccess;

import model.users.UserData;
import org.w3c.dom.CDATASection;

import java.util.Collection;


public interface UserDAO {
    enum AuthMode {
        REGISTER,
        LOGIN
    };
    void insertUser(UserData user) throws DataAccessException;
    boolean authenticate(String username, String password, AuthMode mode) throws DataAccessException;
    boolean newUser(String username) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    Collection<UserData> getAllUsers() throws DataAccessException;
    void updateUser(UserData user) throws DataAccessException;
    void deleteUser(UserData user) throws DataAccessException;
    void clear() throws DataAccessException;
}
