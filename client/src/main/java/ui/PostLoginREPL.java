package ui;

import chess.ChessGame;
import client.ServerFacade;
import model.game.GameData;

import java.util.Collection;
import java.util.Objects;
import java.util.Scanner;

import static ui.EscapeSequences.*;

import static java.lang.System.out;

public class PostLoginREPL {
    // private restricts visibility to only the containing class.
    // final means it can only be assigned once.
    private final ServerFacade facade;
    private final PreLoginREPL loginREPL;
    private final Scanner scanner = new Scanner(System.in);
    private final String serverDomain;

    // establish connection to a WS.
    // prepare for receiving a message.

    public PostLoginREPL(ServerFacade facade, PreLoginREPL loginREPL, String serverDomain) {
        this.facade = facade;
        this.loginREPL = loginREPL;
        this.serverDomain = serverDomain;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
            out.print("[LOGGED IN]: ");
            String[] input = getUserInput();

            // works for empty input
            if (input.length == 0 || input[0].isEmpty()) {
                continue;
            }

            // combine input into full command for two word commands
            String command = String.join(" ", input);
            loggedIn = processCommand(command, input);
        }
    }

    private boolean processCommand(String command, String[] input) {
        return switch (command) {
            // the yield part is able to run function and return boolean val.
            case "list games" -> { listGames(); yield true; }
            case "help" -> { printMenu(); yield true; }
            case "logout" -> handleLogout();
            default -> processMultiWordCommands(input);
        };
    }

    private boolean processMultiWordCommands(String[] input) {
        String commandPrefix = input.length > 0 ? input[0].toLowerCase() : "";
        if (commandPrefix.equals("observe") && input.length == 2) {
            handleObserveGame(input[1]);
        } else if (commandPrefix.equals("join") && input.length == 3) {
            handleJoinGame(input[1], input[2]);
        } else if (commandPrefix.equals("create") && input.length == 2) {
            handleCreateGame(input[1]);
        } else if (commandPrefix.equals("list") && input.length == 2 && input[1].equalsIgnoreCase("games")) {
            listGames();
        }
        else {
                out.println("Unknown command: " + String.join(" ", input));
                out.println("Type 'help' for available commands.");
        }
        return true;
    }

    private void listGames() {
        out.println("Executing list games...");
        Collection<GameData> listOfGames = facade.listGames();
        if (listOfGames == null || listOfGames.isEmpty()) {
            out.println("No games found");
            return;
        }
        printGameList(listOfGames);
    }

    private void printGameList(Collection<GameData> games) {
        out.println("Found " + games.size() + " games");
        out.printf("%-8s %-15s %-15s %-15s%n", "Game ID", "White Player", "Black Player", "Game Name");
        out.println("-------------------------------------------------------");
        for (GameData game : games) {
            String whiteUsername = game.whiteUsername() != null ? game.whiteUsername() : "None";
            String blackUsername = game.blackUsername() != null ? game.blackUsername() : "None";
            out.printf("%-8d %-15s %-15s %-15s%n", game.gameID(), whiteUsername, blackUsername, game.gameName());
        }
    }

    private void handleObserveGame(String gameIdStr) {
        try {
            int gameId = Integer.parseInt(gameIdStr);
            GameData gameData = findGameByID(gameId);

            if (gameData == null || gameData.game() == null) {
                out.println(SET_TEXT_COLOR_RED + "Game " + gameId + " not found or has no game state." + RESET_TEXT_COLOR);
                return;
            }

            String authToken = facade.getAuthToken();
            if (authToken == null) {
                out.println(SET_TEXT_COLOR_RED + "Error: Not properly logged in (no auth token)." + RESET_TEXT_COLOR);
                return;
            }

            out.println("Entering observer mode for game: " + gameData.gameName() + " (ID: " + gameId + ")");

            // --- Create and run GameplayREPL ---
            // Observer has null playerColor
            GameplayREPL gameplayRepl = new GameplayREPL(facade, serverDomain, gameId, null, authToken, this, gameData.game());
            gameplayRepl.run(); // This blocks until the user leaves the game

            // --- Code here runs AFTER GameplayREPL finishes ---
            System.out.print(ERASE_SCREEN); // Clear screen after leaving game
            out.println("Returned to main menu.");


        } catch (NumberFormatException e) {
            out.println(SET_TEXT_COLOR_RED + "Invalid game ID: " + gameIdStr + ". Must be a number." + RESET_TEXT_COLOR);
        } catch (Exception e) {
            out.println(SET_TEXT_COLOR_RED + "Error entering observer mode: " + e.getMessage() + RESET_TEXT_COLOR);
            e.printStackTrace();
        }
    }

    private void handleJoinGame(String gameIdStr, String color) {
        try {
            int gameId = Integer.parseInt(gameIdStr);
            String lowerColor = color.toLowerCase();
            ChessGame.TeamColor playerColor;

            if (lowerColor.equals("white")) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (lowerColor.equals("black")) {
                playerColor = ChessGame.TeamColor.BLACK;
            } else {
                out.println(SET_TEXT_COLOR_RED + "Invalid color: '" + color + "'. Use 'WHITE' or 'BLACK'." + RESET_TEXT_COLOR);
                return;
            }

            // --- Call Facade to Join (HTTP PUT /game) ---
            // This reserves the spot on the server BEFORE connecting via WebSocket
            boolean joinedSuccessfully = facade.joinGame(gameId, lowerColor); // Use lowercase color for facade if needed

            if (!joinedSuccessfully) {
                // Facade should have printed the error from the HTTP response
                out.println(SET_TEXT_COLOR_RED + "Failed to join game via HTTP. See previous error." + RESET_TEXT_COLOR);
                return;
            }

            // --- Successfully joined via HTTP, now get GameData and connect WebSocket ---
            GameData gameData = findGameByID(gameId); // Fetch game data again to get current state
            if (gameData == null || gameData.game() == null) {
                out.println(SET_TEXT_COLOR_RED + "Error: Joined game " + gameId + " but couldn't retrieve its state." + RESET_TEXT_COLOR);
                // Maybe try to leave game via HTTP? Or just fail.
                return;
            }

            String authToken = facade.getAuthToken(); // Get token
            if (authToken == null) {
                out.println(SET_TEXT_COLOR_RED + "Error: Not properly logged in (no auth token)." + RESET_TEXT_COLOR);
                return;
            }

            out.println("Successfully joined game: " + gameData.gameName() + " (ID: " + gameId + ") as " + playerColor);

            // --- Create and run GameplayREPL ---
            GameplayREPL gameplayRepl = new GameplayREPL(facade, serverDomain, gameId, playerColor, authToken, this, gameData.game());
            gameplayRepl.run(); // Blocks until user leaves

            // --- Code here runs AFTER GameplayREPL finishes ---
            System.out.print(ERASE_SCREEN); // Clear screen after leaving game
            out.println("Returned to main menu.");

        } catch (NumberFormatException e) {
            out.println(SET_TEXT_COLOR_RED + "Invalid game ID: " + gameIdStr + ". Must be a number." + RESET_TEXT_COLOR);
        } catch (Exception e) {
            out.println(SET_TEXT_COLOR_RED + "Error joining game: " + e.getMessage() + RESET_TEXT_COLOR);
            e.printStackTrace();
        }
    }


    private void handleCreateGame(String gameName) {
        out.println("Creating game...");
        try {
            // Assuming facade.createGame returns the new GameData or at least the ID
            int newGameID = facade.createGame(gameName);
            if (newGameID > 0) {
                out.println("Successfully created game '" + gameName + "' with ID: " + newGameID);
            } else {
                out.println(SET_TEXT_COLOR_RED + "Failed to create game '" + gameName + "'. Server did not return a valid game ID." + RESET_TEXT_COLOR);
                out.println(SET_TEXT_COLOR_YELLOW + "(Check previous messages or server logs for details)." + RESET_TEXT_COLOR);
            }
        } catch (Exception e) {
            out.println(SET_TEXT_COLOR_RED + "An error occurred while trying to create the game: " + e.getMessage() + RESET_TEXT_COLOR);
            // Optionally print stack trace for debugging
            // e.printStackTrace();
        }
    }

    private boolean handleLogout() {
        if (facade.logout()) {
            out.println("You have logged out.");
            return false;
        } else {
            out.println(SET_TEXT_COLOR_RED + "Logout failed (check facade logs/output)." + RESET_TEXT_COLOR);
            return true;
        }
    }

    private String[] getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim().split("\\s+");
    }


    private GameData findGameByID(int gameID) {
        Collection<GameData> games = facade.listGames();
        if (games != null) {
            for (GameData game: games) {
                if (game.gameID() == gameID) {
                    return game;
                }
            }
        }
        return null;
    };

    private void printMenu() {
        out.println(SET_TEXT_COLOR_BLUE + """
                Available commands:
                  logout           - Log out the current user.
                  create <NAME>    - Create a new game.
                  list games       - List all available games.
                  join <ID> <COLOR>- Join a game as WHITE or BLACK (e.g., join 1234 WHITE).
                  observe <ID>     - Observe a game (e.g., observe 1234).
                  help             - Show this menu.
                """ + RESET_TEXT_COLOR);
    }
}
