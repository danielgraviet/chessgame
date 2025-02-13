package server;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import service.GameService;
import model.users.EmptyResponse;
import model.users.UserData;
import spark.Request;
import spark.Response;
import service.UserService;
import java.util.HashMap;
import java.util.Map;

public class GameHandler {

    private final GameService gameService;

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Object createGame(Request req, Response res) {
        // this is where I set all my error codes.
        try {
            String authData = req.headers("Authorization");

            if (authData == null || authData.isEmpty()) {
                res.status(400);
                return createErrorResponse("Error: bad request");
            }

            // need a way to check that the authToken is in the database.

            int gameID = gameService.createGame(authData, req.body());
            res.status(200);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("gameID", gameID);
            return new Gson().toJson(responseMap);

        } catch (DataAccessException e) {
            if (e.getMessage().equals("Invalid token.")) {
                res.status(401);
                return createErrorResponse("Error: unauthorized");
            } else {
                res.status(500);
                return createErrorResponse("Error: " + e.getMessage());
            }
        }
    }

    private String createErrorResponse(String error) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", error);
        return new Gson().toJson(jsonObject);
    }
}
