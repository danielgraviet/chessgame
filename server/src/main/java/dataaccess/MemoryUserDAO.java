package dataaccess;

import model.users.UserData;

import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;

public class MemoryUserDAO implements UserDAO {
    // local storage
    private final HashSet<UserData> userStorage = new HashSet<>();

    // CREATE
    public void insertUser(UserData user) throws DataAccessException {
        // check for duplicates
        if (userStorage.contains(user)) {
            throw new DataAccessException("User already exists");
        }
        // adding to local storage
        userStorage.add(user);
    }

    // READ
    public UserData getUser(String username) throws DataAccessException {
        return userStorage.stream()
                .filter(user -> user.username().equals(username))
                .findFirst().orElse(null);
    }

    // CLEAR
    public void clear() {
        userStorage.clear();
    }
}
