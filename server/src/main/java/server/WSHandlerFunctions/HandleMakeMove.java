package server.WSHandlerFunctions;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.*;
import model.auth.AuthData;
import model.game.GameData;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import server.ConnectionManager;
import websocket.commands.MakeMoveCommand;
import websocket.messages.*;

import java.io.IOException;

public class HandleMakeMove {

    private final GameService gameService;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ConnectionManager connectionManager;
    private final HelperFunctions helperFunctions;
    private final Gson gson = new Gson();

    public HandleMakeMove(GameService gameService, AuthDAO authDAO, GameDAO gameDAO, ConnectionManager connectionManager, HelperFunctions helperFunctions) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
        this.helperFunctions = helperFunctions;
    }

    public void handle(Session session, MakeMoveCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();

        if (this.helperFunctions.invalidTokenAndID(gameID, authToken, session)) {
            return;
        };

        ChessMove move = command.getMove();

        ChessGame game = null;
        GameData gameData = null;
        String username = null;
        AuthData authData = null;


        if (gameID == null || authToken == null || authToken.isBlank() || move == null) {
            this.helperFunctions.sendError(session, "Error: Missing required fields (gameID, authToken, move) for MAKE_MOVE.");
            return;
        }

        try {
            // authenticate the user
            authData = authDAO.getUser(authToken);
            if (authData == null) {
                this.helperFunctions.sendError(session, "Error: Invalid or expired authentication token.");
                return;
            }

            // output the username
            username = authData.username();
            System.out.println("User '" + username + "' attempting to make a move in game " + gameID);

            // get the gameData
            gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                this.helperFunctions.sendError(session, "Error: Game ID " + gameID + " does not exist.");
            }

            game = gameService.getGame(gameID);
            if (game == null) {
                System.err.println("CRITICAL: GameData found but GameService failed to load game " + gameID);
                this.helperFunctions.sendError(session, "Error: Could not load game logic/state for game ID " + gameID + ".");
                return;
            }

            // authorize the move
            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            }

            if (playerColor == null) {
                this.helperFunctions.sendError(session, "Error: Observers cannot make moves.");
                return;
            }

            if (game.getTeamTurn() != playerColor) {
                this.helperFunctions.sendError(session, "Error: It's not your turn (" + game.getTeamTurn() + "'s turn).");
                return; // Stop processing
            }

            if (game.getTeamTurn() == null || game.isGameOver()) {
                this.helperFunctions.sendError(session, "Error: The game is already over.");
                return;
            }

            // where the actual change of the game occurs.
            gameService.makeMove(gameID, move, username, playerColor);
            System.out.println("Move validated and executed successfully for game " + gameID);

            // assume it has been successful, so now we broadcast the updates.

            // get the updated game.
            ChessGame updatedGame = gameService.getGame(gameID);
            if (updatedGame == null) {
                System.err.println("CRITICAL: Game state is null after successful move and save for game " + gameID);
                this.helperFunctions.sendError(session, "Internal server error after making move.");
                return;
            }

            LoadGameMessage loadGameMessage = new LoadGameMessage(gameData);
            String loadGameJson = gson.toJson(loadGameMessage);
            this.helperFunctions.broadcastMessage(loadGameJson, gameID, null);
            System.out.println("Broadcast LOAD_GAME (after move) to all in game " + gameID);

            String moveDescription = String.format("'%s' (%s) moved %s from %s to %s%s.",
                    username,
                    playerColor,
                    updatedGame.getBoard().getPiece(move.getEndPosition()).getPieceType(),
                    move.getStartPosition().toString(),
                    move.getEndPosition().toString(),
                    move.getPromotionPiece() != null ? " promoting to " + move.getPromotionPiece() : "");

            NotificationMessage moveNotification = new NotificationMessage(moveDescription);
            String moveNotificationJson = gson.toJson(moveNotification);
            this.helperFunctions.broadcastMessage(moveNotificationJson, gameID, authToken);
            System.out.println("Broadcast MOVE NOTIFICATION to others in game " + gameID);


            ChessGame.TeamColor opponentColor = (playerColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            String opponentUsername = (playerColor == ChessGame.TeamColor.WHITE) ? gameData.blackUsername() : gameData.whiteUsername();
            opponentUsername = (opponentUsername == null) ? "[Opponent]" : "'" + opponentUsername + "'";

            String stateNotificationText = null;
            if (updatedGame.isInCheckmate(opponentColor)) {
                stateNotificationText = String.format("CHECKMATE! %s (%s) defeated %s (%s).", username, playerColor, opponentUsername, opponentColor);
            } else if (updatedGame.isInStalemate(opponentColor)) {
                stateNotificationText = "STALEMATE! The game is a draw.";
            } else if (updatedGame.isInCheck(opponentColor)) {
                stateNotificationText = String.format("CHECK! %s (%s) is in check.", opponentUsername, opponentColor);
            }

            if (stateNotificationText != null) {
                NotificationMessage stateNotification = new NotificationMessage(stateNotificationText);
                String stateNotificationJson = gson.toJson(stateNotification);
                this.helperFunctions.broadcastMessage(stateNotificationJson, gameID, null);
                System.out.println("Broadcast GAME STATE NOTIFICATION to all in game " + gameID + ": " + stateNotificationText);
            }

        } catch (InvalidMoveException e) {

            System.out.println("Invalid move attempted by " + username + " in game " + gameID + ": " + e.getMessage());
            this.helperFunctions.sendError(session, "Error: Invalid move - " + e.getMessage());


        } catch (DataAccessException e) {

            System.err.println("Data access error processing move for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            this.helperFunctions.sendError(session, "Error processing move: A database error occurred. " + e.getMessage());


        } catch (IOException e) {

            System.err.println("IO error during make move processing/broadcast for game " + gameID + ": " + e.getMessage());


        } catch (Exception e) {

            System.err.println("Unexpected error processing move for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            this.helperFunctions.sendError(session, "An unexpected server error occurred while processing your move.");
        }
    }
}