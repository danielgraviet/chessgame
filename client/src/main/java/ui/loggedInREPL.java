package ui;

import chess.ChessGame;
import client.ServerFacade;
import model.game.GameData;

import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.out;

public class loggedInREPL {
    ServerFacade facade;
    loginREPL loginREPL;

    public loggedInREPL(ServerFacade facade, loginREPL loginREPL) {
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

            switch (command) {
                case "list games":
                    out.println("Executing list games...");
                    Collection<GameData> listOfGames = facade.listGames();
                    if (listOfGames == null || listOfGames.isEmpty()) {
                        out.println("No games found");
                    } else {
                        out.println("Found " + listOfGames.size() + " games");

                        // print header
                        out.printf("%-8s %-15s %-15s %-15s%n", "Game ID", "White Player", "Black Player", "Game Name");
                        out.println("-------------------------------------------------------");
                        for (GameData game : listOfGames) {
                            String whiteUsername = game.whiteUsername() != null ? game.whiteUsername() : "None";
                            String blackUsername = game.blackUsername() != null ? game.blackUsername() : "None";
                            out.printf("%-8d %-15s %-15s %-15s%n",
                                    game.gameID(),
                                    whiteUsername,
                                    blackUsername,
                                    game.gameName());
                        }
                    }
                    break;
                default:
                    // multi-word commands
                    if (input[0].equals("observe") && input.length == 2) {
                        try {
                            int gameId = Integer.parseInt(input[1]);
                            out.println("Observing game: " + gameId);

                            Collection<GameData> games = facade.listGames();
                            GameData targetGame = null;
                            if (games != null) {
                                for (GameData game: games) {
                                    if (game.gameID() == gameId) {
                                        targetGame = game;
                                        break;
                                    }
                                }
                            }

                            if (targetGame == null) {
                                out.println("Game with ID " + gameId + " not found.");
                            } else {
                                renderBoard.printBoard(targetGame.game(), true);
                                out.println("Observing " + targetGame.gameName() + ". Type 'exit' to quit.");
                                Scanner scanner = new Scanner(System.in);
                                while(!scanner.nextLine().trim().equalsIgnoreCase("exit")) {
                                    out.println("Type 'exit' to stop observing game.");
                                }
                            }
                        } catch (NumberFormatException e) {
                            out.println("Invalid game ID: " + input[1]);
                        }

                    } else if (input[0].equals("join") && input.length == 4 && input[1].equals("game")) {
                        if (facade.joinGame(Integer.parseInt(input[2]), input[3])) {
                            out.println("Joined game: " + input[2]);
                            // add implementation to show that the user joined the game. display the current game info.
                        } else {
                            out.println("Failed to join game.");
                        }
                    } else if (input[0].equals("create") && input.length == 3 && input[1].equals("game")) {
                        out.println("Creating game...");
                        facade.createGame(input[2]);
                        out.println("Created game " + input[2]);
                    } else if (command.equals("help")) {
                        printMenu();
                    } else if (command.equals("logout")) {
                        if (facade.logout()) {
                            out.println("You have logged out");
                            loggedIn = false;
                            loginREPL.run();
                        }
                    } else {
                        out.println("Unknown command: " + command);
                        out.println("Type 'help' for available commands.");
                    }
                    break;
            }
        }
    }

    private String[] getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim().split("\\s+");
    }

    private void printMenu() {
        out.println("""
                Available commands:
                    Logout: Current user will be logged out.
                    Create game: <game name> - Creates a new game.
                    List games: List all games.
                    Join game: <gameID> <BLACK/WHITE> - Joins a specific game.
                    Observe game: Observe a game.
                """);
    }
}