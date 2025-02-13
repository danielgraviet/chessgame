package dataaccess;

import model.users.UserData;
import org.w3c.dom.CDATASection;

import java.util.Collection;


public interface UserDAO {
    void insertUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    void updateUser(UserData user) throws DataAccessException;
    void deleteUser(UserData user) throws DataAccessException;
    void clear() throws DataAccessException;
}
