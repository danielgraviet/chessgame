package dataaccess;

import model.users.UserData;
import java.util.Collection;


public interface UserDAO {
    void insertUser(UserData user) throws DataAccessException;
    boolean authenticate(String username, String password) throws DataAccessException;
    boolean newUser(String username) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    Collection<UserData> getAllUsers() throws DataAccessException;
    void updateUser(UserData user) throws DataAccessException;
    void deleteUser(UserData user) throws DataAccessException;
    void clear() throws DataAccessException;
}
