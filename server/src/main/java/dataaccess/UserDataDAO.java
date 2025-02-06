package dataaccess;

import model.UserData;

import java.util.HashSet;
import java.util.Collection;

public class UserDataDAO implements UserDAO {
    // local storage
    private final HashSet<UserData> UserStorage = new HashSet<>();

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
}
