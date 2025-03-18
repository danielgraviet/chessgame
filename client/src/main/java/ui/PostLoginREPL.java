package ui;

import client.ServerFacade;
import model.game.GameData;

import java.sql.ClientInfoStatus;
import java.util.Collection;
import java.util.Objects;
import java.util.Scanner;

import static java.lang.System.out;

public class PostLoginREPL {
    // private restricts visibility to only the containing class.
    // final means it can only be assigned once.
    private final ServerFacade facade;
    private final PreLoginREPL loginREPL;
    private final Scanner scanner = new Scanner(System.in);


    public PostLoginREPL(ServerFacade facade, PreLoginREPL loginREPL) {
        this.facade = facade;
        this.loginREPL = loginREPL;
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
        if (input[0].equals("observe") && input.length == 2) {
            handleObserveGame(input[1]);
        } else if (input[0].equals("join") && input.length == 4 && input[1].equals("game")) {
            handleJoinGame(input[2], input[3]);
        } else if (input[0].equals("create") && input.length == 3 && input[1].equals("game")) {
            handleCreateGame(input[2]);
        } else {
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
            out.println("Attempting to observe game: " + gameId);
            GameData game = findGameByID(gameId);
            if (game == null) {
                out.println("Game with ID " + gameId + " not found.");
                return;
            }
            observeGame(game);
        } catch (NumberFormatException e) {
            out.println("Invalid game ID: " + gameIdStr);
        }
    }

    private void observeGame(GameData game) {
        RenderBoard.printBoard(game.game(), true);
        out.println("Observing " + game.gameName() + ". Type 'exit' to quit.");
        while (!scanner.nextLine().trim().equalsIgnoreCase("exit")) {
            out.println("Type 'exit' to stop observing game.");
        }
    }

    private void handleJoinGame(String gameIdStr, String color) {
        if (gameIdStr == null) {
            out.println("Error: Game ID cannot be null.");
            return;
        }
        try {
            int gameId = Integer.parseInt(gameIdStr);
            if (facade.joinGame(gameId, color)) {
                out.println("Joined game: " + gameIdStr);
                observeJoinedGame(gameIdStr, color);
            } else {
                out.println("Failed to join game.");
            }
        } catch (NumberFormatException e) {
            out.println("Error: game ID must be a number. You typed: " + gameIdStr);
        }
    }

    private void observeJoinedGame(String gameIdStr, String color) {
        try {
            int gameId = Integer.parseInt(gameIdStr);
            GameData game = findGameByID(gameId);
            if (game == null) {
                out.println("Game with ID " + gameId + " not found.");
                return;
            }
            boolean perspective = Objects.equals(color.toLowerCase(), "white");
            RenderBoard.printBoard(game.game(), perspective);
            out.println("Observing " + game.gameName() + ". Type 'exit' to quit.");
            while (!scanner.nextLine().trim().equalsIgnoreCase("exit")) {
                out.println("Type 'exit' to stop observing game.");
            }
        } catch (NumberFormatException e) {
            out.println("Invalid game ID: " + gameIdStr);
        }
    }

    private void handleCreateGame(String gameName) {
        out.println("Creating game...");
        facade.createGame(gameName);
        out.println("Created game " + gameName);
    }

    private boolean handleLogout() {
        if (facade.logout()) {
            out.println("You have logged out");
            loginREPL.run();
            return false;
        }
        return true;
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
        out.println("""
                Available commands:
                    Logout: "logout" - Current user will be logged out.
                    Create game: "create game <game name>" - Creates a new game.
                    List games: "list games" - List all games.
                    Join game: "join game <gameID> <BLACK/WHITE>" - Joins a specific game.
                    Observe game: "observe <gameID>" - Observe a game.
                    Help: "help" - Displays this help menu.
                """);
    }
}
