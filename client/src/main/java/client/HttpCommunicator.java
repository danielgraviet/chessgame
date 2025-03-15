package client;

import chess.ChessGame;
import com.google.gson.Gson;
import model.game.GameData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class HttpCommunicator implements ServerCommunicator {
    private final String baseUrl;
    private final ServerFacade facade;
    private final Gson GSON = new Gson();

    // this class is like a mailman, and takes care of all the tasks delivering and receiving
    public HttpCommunicator(ServerFacade facade,  String serverName) {
        this.baseUrl = "http://" + serverName;
        this.facade = facade;
    }

    public boolean register(String username, String password, String email) {
        // think of this as a python dictionary.
        Map<String, String> body = Map.of("username", username, "password", password, "email", email);
        Map<String, Object> response = sendRequest("POST", "/user", body);
        return handleAuthResponse(response);
    }


    public boolean login(String username, String password) {
        Map<String, String> body = Map.of("username", username, "password", password);
        Map<String, Object> response = sendRequest("POST", "/session", body);
        return handleAuthResponse(response);
    }

    public boolean logout(){
        // this should log out the user, and remove the auth token
        Map<String, Object> response = sendRequest("DELETE", "/session", null);
        return handleAuthResponse(response);
    }

    public void reset() {
        Map<String, Object> response = sendRequest("DELETE", "/db", null);
        if (response.containsKey("error")) {
            throw new RuntimeException("Failed te reset server: " + response);
        }
        System.out.println("System Reset: " + response);
    }

    public int createGame(String gameName) {
        Map<String, Object> response = sendRequest("POST", "/game", Map.of("gameName", gameName));

        // simple error checking
        if (response.containsKey("error")) {
            throw new RuntimeException("Failed te create game: " + response);
        }

        // check for an existing id
        if (!response.containsKey("gameID")) {
            throw new RuntimeException("Missing gameID: " + response);
        }

        // get the gameID and convert it to an int
        Object gameID = response.get("gameID");
        if (gameID == null) {
            throw new RuntimeException("gameID is null: " + response);
        } else if (gameID instanceof Number) {
            return ((Number) gameID).intValue();
        } else {
            throw new RuntimeException("gameID is not an integer: " + response);
        }
    }

    public HashSet<GameData> listGames() {
        //Spark.get("/game", gameServer::listGames);
        Map<String, Object> response = sendRequest("GET", "/game", null);

        // error checking
        if (response.containsKey("error")) {
            String errorMsg = response.containsKey("message") ? (String) response.get("message") : "Unknown error";
            throw new RuntimeException("Failed to list games: " + response.get("error") + " - " + errorMsg);
        }

        // make sure games are present
        Object gamesObject = response.get("games");
        if (gamesObject == null) {
            throw new RuntimeException("Missing games: " + response);
        }

        // storage for rawGames (json) -> games (gameData objects)
        List<?> rawGamesList = (List<?>) gamesObject;
        HashSet<GameData> games = new HashSet<>();

        for (Object game : rawGamesList) {
            if (!(game instanceof Map)) {
                throw new RuntimeException("Invalid game format: " + game);
            }

            Map<String, Object> gameMap = (Map<String, Object>) game;

            // extract the objects and prepare for conversion
            Object gameIDObject = gameMap.get("gameID");
            Object whiteUsernameObject = gameMap.get("whiteUsername");
            Object blackUsernameObject  = gameMap.get("blackUsername");
            Object gameNameObject = gameMap.get("gameName");
            Object chessGameObject = gameMap.get("chessGame");

            // convert from number to int.
            int gameID = ((Number) gameIDObject).intValue();

            // make sure null is also okay.
            String whiteUsername = whiteUsernameObject != null ? whiteUsernameObject.toString() : null;
            String blackUsername = blackUsernameObject != null ? blackUsernameObject.toString() : null;

            String gameName = gameNameObject.toString();
            ChessGame chessGame = GSON.fromJson(GSON.toJson(chessGameObject), ChessGame.class);

            games.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
        }

        return games;
    }

    public boolean joinGame(int gameId, String playerColor) {
        //Spark.put("/game", gameServer::joinGame);
        Map<String, Object> body = Map.of("gameId", gameId, "playerColor", playerColor);
        Map<String, Object> response = sendRequest("PUT", "/game", body);
        // signify if it worked.
        return handleAuthResponse(response);
    }

    // private methods
    private Map<String, Object> sendRequest(String method, String endpoint, Map<String, ?> body) {
        try {
            HttpURLConnection connection = setupConnection(method, endpoint, body);
            int status = connection.getResponseCode();
            System.out.println("HTTP status code: " + status);
            // response code is set as 400
            if (status == 400) {
                return Map.of("error", "HTTP" + status);
            } else if (status == 401) {
                return Map.of("error", " 401: Unauthorized");
            } else if (status == 403) {
                return Map.of("error", " 403: Forbidden");
            } else if (status == 500) {
                return Map.of("error", " 500: Internal Server Error");
            }

            try (var reader = new InputStreamReader(connection.getInputStream())) {
                return GSON.fromJson(reader, Map.class);
            }
        } catch (IOException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private HttpURLConnection setupConnection(String method, String endpoint, Map<String, ?> body) throws IOException {
        URI uri = URI.create(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);

        String authToken = facade.getAuthToken();
        System.out.println("Auth token for " + method + " " + endpoint + " " + (authToken != null ? authToken : "null"));


        if (facade.getAuthToken() != null) {
            conn.setRequestProperty("Authorization", facade.getAuthToken());
        }

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (var output = conn.getOutputStream()) {
                output.write(GSON.toJson(body).getBytes());
            }
        }
        return conn;
    }

    private boolean handleAuthResponse(Map<String, Object> response) {
        if (response.containsKey("error")) {
            return false;
        }

        if (response.containsKey("authToken") && response.get("authToken") != null) {
            facade.setAuthToken((String) response.get("authToken"));
        }
        return true;
    }
}
