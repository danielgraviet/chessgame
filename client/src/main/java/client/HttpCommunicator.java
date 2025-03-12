package client;

import com.google.gson.Gson;
import model.game.GameData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;

public class HttpCommunicator implements ServerCommunicator {
    private final String baseUrl;
    private final ServerFacade facade;
    private final Gson gson = new Gson();

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
        return false;
    }

    public boolean logout(){
        return false;
    }

    public int createGame(String gameName) {
        return 0;
    }

    public HashSet<GameData> listGames() {
        return null;
    }

    public boolean joinGame(int gameId, String playerColor) {
        return false;
    }

    // private methods
    private Map<String, Object> sendRequest(String method, String endpoint, Map<String, ?> body) {
        try {
            HttpURLConnection connection = setupConnection(method, endpoint, body);
            int status = connection.getResponseCode();
            if (status >= 400) {
                return Map.of("error", "HTTP" + status);
            }

            try (var reader = new InputStreamReader(connection.getInputStream())) {
                return gson.fromJson(reader, Map.class);
            }
        } catch (IOException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private HttpURLConnection setupConnection(String method, String endpoint, Map<String, ?> body) throws IOException {
        URI uri = URI.create(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);

        if (facade.getAuthToken() != null) {
            conn.setRequestProperty("Authorization", facade.getAuthToken());
        }

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (var output = conn.getOutputStream()) {
                output.write(gson.toJson(body).getBytes());
            }
        }
        return conn;
    }

    private boolean handleAuthResponse(Map<String, Object> response) {
        if (response.containsKey("error")) {
            return false;
        }
        facade.setAuthToken((String) response.get("authToken"));
        return true;
    }
}
