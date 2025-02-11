package dataaccess;

import model.users.UserData;

import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;

public class MemoryUserDAO implements UserDAO {
    // local storage
    private final HashSet<UserData> UserStorage = new HashSet<>();

    public boolean authenticateRegister(String username, String password) throws DataAccessException {
        // what happens for second user?
        if (UserStorage.isEmpty()) {
            return true;
        }

        if (newUser(username)) {
            return true;
        }
        // i need to adjust this so that if the user is not in the data base, it should return true, to allow new users to register.
        for (UserData user : UserStorage) {
           if (user.username().equals(username)) {
               if (Objects.equals(password, user.password())) {
                   return true;
               } else {
                   throw new DataAccessException("Username and password do not match.");
               }
            }
        }
        throw new DataAccessException("User not found.");
    }

    public boolean authenticateLogin(String username, String password) throws DataAccessException {
        if (UserStorage.isEmpty()) {
            return false;
        }

        for (UserData user : UserStorage){
            if (user.username().equals(username)) {
                if (Objects.equals(password, user.password())) {
                    return true;
                } else {
                    throw new DataAccessException("Username and password do not match.");
                }
            }
        }
        throw new DataAccessException("User not found.");
    }


    // checks if the username is in the db already.
    public boolean newUser(String username) throws DataAccessException {
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
    public void clear() throws DataAccessException {
        UserStorage.clear();
    }
}
