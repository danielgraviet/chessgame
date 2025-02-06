package dataaccess;

import model.UserData;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Collection;


public interface UserDAO {
    void insertUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    Collection<UserData> getAllUsers() throws DataAccessException;
    void updateUser(UserData user) throws DataAccessException;
    void deleteUser(UserData user) throws DataAccessException;
}
