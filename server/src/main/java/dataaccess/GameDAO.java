package dataaccess;

public interface GameDAO {
    // basic crud operations
    int createGame(String authToken, String gameName) throws DataAccessException;
}
