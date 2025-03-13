package ui;

import client.ServerFacade;

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
            switch (input[0]) {
                case "observe game":
                    out.print("Implement observe game functionality");
                    break;
                case "play game":
                    out.print("Implement play game functionality");
                    break;
                case "list games":
                    out.print("Implement list games functionality");
                    break;
                case "create game":
                    if (input.length != 2) {
                        out.print("Please enter create game and a valid game name.");
                        break;
                    }

                    facade.createGame(input[1]);
                    out.print("Created game " + input[1]);
                    break;
                case "help":
                    printMenu();
                    break;
                case "logout":
                    if (facade.logout()) {
                        out.println("You have logged out");
                        loggedIn = false;
                        loginREPL.run();
                    }
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
