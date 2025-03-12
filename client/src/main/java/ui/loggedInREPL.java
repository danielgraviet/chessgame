package ui;

import client.ServerFacade;

import java.util.Scanner;

import static java.lang.System.out;

public class loggedInREPL {
    ServerFacade facade;

    public loggedInREPL(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        boolean loggedIn = true;
        while (loggedIn) {
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
                    out.print("Implement create game functionality");
                    break;
                case "help":
                    printMenu();
                    break;
                case "logout":
                    out.print("Implement logged out functionality");
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
                    create game: Creates a new game.
                    list games: List all games.
                    play game: Joins a specific game.
                    observe game: Observe a game.
                """);
    }
}
