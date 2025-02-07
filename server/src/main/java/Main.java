import chess.*;
import dataaccess.DataAccessException;
import model.users.UserData;
import server.Server;
import dataaccess.MemoryUserDAO;

public class Main {

    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Server: " + piece);

        // to run the server, click on Main file and run "Main main"
        // then go to the browser and type in localhost:8080
        Server server = new Server();
        server.run(8080);

        try {
            MemoryUserDAO userDataDAO = new MemoryUserDAO();
            UserData user = new UserData("danny", "secret123", "dgravs5@gmail.com");
            userDataDAO.insertUser(user);

            UserData retrievedUser = userDataDAO.getUser("danny");
            System.out.println(retrievedUser);

        } catch (DataAccessException e) {
            System.err.println(e.getMessage());
        }
    }
}