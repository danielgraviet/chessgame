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
                case "observe game":
                    out.println("Implement observe game functionality");
                    break;
                case "play game":
                    out.println("Implement play game functionality");
                    break;
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
                    if (input[0].equals("create") && input.length == 3 && input[1].equals("game")) {
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
                    logout: Current user will be logged out.
                    create game: <game name> - Creates a new game.
                    list games: List all games.
                    play game: Joins a specific game.
                    observe game: Observe a game.
                """);
    }
}