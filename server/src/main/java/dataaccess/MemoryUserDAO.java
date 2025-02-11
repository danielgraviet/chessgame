package dataaccess;

import model.users.UserData;

import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;

public class MemoryUserDAO implements UserDAO {
    // local storage
    private final HashSet<UserData> UserStorage = new HashSet<>();

    public boolean authenticate(String username, String password, AuthMode mode) throws DataAccessException{
        if (UserStorage.isEmpty()) {
            if (mode == AuthMode.REGISTER) {
                return true;
            } else {
                // this means they are trying to login.
                return false;
            }
        }

        for (UserData user : UserStorage) {
            if (user.username().equals(username)) {
                if (Objects.equals(password, user.password())) {
                    return true;
                } else {
                    throw new DataAccessException("Username and password do not match.");
                }
            }
        }

        if (mode == AuthMode.REGISTER && newUser(username)) {
            return true;
        } else {
            throw new DataAccessException("User not found.");
        }
    }

    // checks if the username is in the db already.
    public boolean newUser(String username) {
        for (UserData user : UserStorage) {
            if (user.username().equals(username)) {
                return false;
            }
        }
        return true;
    }

    // CREATE
    public void insertUser(UserData user) throws DataAccessException {
        // check for duplicates
        if (UserStorage.contains(user)) {
            throw new DataAccessException("User already exists");
        }
        // adding to local storage
        UserStorage.add(user);
    }

    // READ
    public UserData getUser(String username) throws DataAccessException {
        return UserStorage.stream()
                .filter(user -> user.username().equals(username))
                .findFirst().orElseThrow(() -> new DataAccessException("User not found"));
    }

    // READ ALL
    public Collection<UserData> getAllUsers() throws DataAccessException {
        if (UserStorage.isEmpty()) {
            throw new DataAccessException("User not found");
        }
        return UserStorage;
    }

    // UPDATE
    public void updateUser(UserData user) throws DataAccessException {
        if (!UserStorage.contains(user)) {
            throw new DataAccessException("User not found");
        }

        // get new data
        String email = user.email();
        String password = user.password();
        String username = user.username();

        // create new obj and insert it
        UserData updatedUserData = new UserData(username, password, email);
        UserStorage.add(updatedUserData);
        // delete old.
        UserStorage.remove(user);
    }

    // DELETE
    public void deleteUser(UserData user) throws DataAccessException {
        if (!UserStorage.contains(user)) {
            throw new DataAccessException("User not found");
        }

        UserStorage.remove(user);
    }

    // CLEAR
    public void clear() {
        UserStorage.clear();
    }
}
