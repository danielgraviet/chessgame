package ui;

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
                    Collection<GameData> games = facade.listGames();
                    if (games == null || games.isEmpty()) {
                        out.println("No games found");
                    } else {
                        out.println("Found " + games.size() + " games");
                        for (GameData game : games) {
                            out.println(game);
                        }
                    }
                    break;
                default:
                    // multi-word commands
                    if (input[0].equals("observe") && input.length == 3 && input[1].equals("observe")) {
                        out.println("Observing game: " + input[2]);
                        // add observe game function
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