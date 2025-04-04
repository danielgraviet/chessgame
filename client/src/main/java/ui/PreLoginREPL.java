package ui;

import client.ServerFacade;

import java.util.Scanner;

import static java.lang.System.out;

public class PreLoginREPL {
    ServerFacade facade;
    PostLoginREPL loggedInREPL;
    private final String serverDomain;

    public PreLoginREPL(ServerFacade facade, String serverDomain) {
        this.facade = facade;
        this.serverDomain = serverDomain;
        this.loggedInREPL = new PostLoginREPL(facade, this, this.serverDomain);
    }

    public void run() {
        boolean loggedIn = false;
        out.print("Chess Game Started. Enter 'help' to get started.");
        // Use a consistent prompt style
        out.print("\n[LOGGED OUT] >>> ");
        while (!loggedIn) {
            String[] input = getUserInput();

            // --- Handle empty input before the switch ---
            if (input == null || input.length == 0 || input[0].isEmpty()) {
                out.println("Please enter a command.");
                out.print("[LOGGED OUT] >>> ");
                continue;
            }

            String command = input[0].toLowerCase();

            switch (command) {
                case "register":
                    if (input.length != 4){
                        out.println("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
                        break;
                    }
                    if (facade.register(input[1], input[2], input[3])) {
                        out.println("User successfully registered and logged in.");
                        loggedIn = true; // Assume auto-login on successful registration
                    } else {
                        out.println("Registration failed.");
                    }
                    break;

                case "login":
                    if (input.length != 3){
                        out.println("Usage: login <USERNAME> <PASSWORD>");
                        break; // Break after usage message
                    }
                    // Attempt login
                    if (facade.login(input[1], input[2])) {
                        out.println("User successfully logged in.");
                        loggedIn = true;
                    } else {
                        out.println("Login failed.");
                    }
                    break;

                case "quit":
                    out.println("Exiting Chess Client. Goodbye!");
                    return; // Exit the run() method entirely

                case "help":
                    printMenu();
                    break;

                default:
                    out.println("Invalid command.");
                    out.println("Type 'help' for a list of valid commands.");
                    break;
            } // End of switch

            // Reprint prompt only if the user is still not logged in AND didn't quit
            if (!loggedIn) {
                out.print("[LOGGED OUT] >>> ");
            }
        } // End of while loop

        // If the loop finishes (loggedIn is true), run the next REPL
        loggedInREPL.run();
    }

    private String[] getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim().split("\\s+");
    }

    private void printMenu() {
        out.println("""
                Available commands:
                    register: "register <username> <password> <email>" - Register a new user.
                    login: "login <username> <password>" - Login an existing user.
                    quit: "quit" - Quit the client.
                    help: "help" - Show this menu
                """);
    }

    public static void main(String[] args) {
        String serverHttpUrl = "http://localhost:8081";
        String serverWsDomain = "localhost:8081";

        if (args.length >= 1) {
            serverHttpUrl = args[0];
            try {
                java.net.URI uri = new java.net.URI(serverHttpUrl);
                serverWsDomain = uri.getHost() + ":" + uri.getPort();
                if (uri.getPort() == -1) {
                    serverWsDomain = uri.getHost() + (uri.getScheme().equals("https") ? ":443" : ":80");
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not parse WebSocket domain from " + serverHttpUrl + ". Using default: " + serverWsDomain);
            }
        }
        if (args.length >= 2) {
            serverWsDomain = args[1];
        }

        System.out.println("Connecting to ServerFacade at: " + serverHttpUrl);
        System.out.println("WebSocket connections will target: " + serverWsDomain);

        ServerFacade facade = new ServerFacade(serverHttpUrl);
        PreLoginREPL loginREPL = new PreLoginREPL(facade, serverWsDomain);
        loginREPL.run();
    }
}

