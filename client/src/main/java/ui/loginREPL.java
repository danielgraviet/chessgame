package ui;

import client.ServerFacade;

import java.util.Scanner;

import static java.lang.System.out;

public class loginREPL {
    ServerFacade facade;
    loggedInREPL loggedInREPL;

    public loginREPL(ServerFacade facade) {
        this.facade = facade;
        loggedInREPL = new loggedInREPL(facade);
    }

    public void run() {
        boolean loggedIn = false;
        out.print("Chess Game Started. Enter 'help' to get started.");
        out.print("\n>");
        while (!loggedIn) {
            String[] input = getUserInput();
            switch (input[0]) {
                case "register":
                    // needs len of 4 bc "register, username, password, email"
                    if (input.length != 4){
                        out.println("Please enter username, password, and email.");
                        break;
                    }
                    if (facade.register(input[1], input[2], input[3])) {
                        out.println("User successfully registered.");
                        loggedIn = true;
                        break;
                    }
                    out.println("Registration failed.");

                case "login":
                    if (input.length != 3){
                        out.println("Please enter username and password.");
                        break;
                    }
                    if (facade.login(input[1], input[2])) {
                        out.println("User successfully logged in.");
                        loggedIn = true;
                        break;
                    } else {
                        out.println("Login failed.");
                        continue;
                    }
                case "quit":
                    out.println("You have quit the client. Goodbye!");
                    return;

                case "help":
                    printMenu();
                    break;

            }
            if (!loggedIn) out.print("> ");
        }
        loggedInREPL.run();
    }

    private String[] getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim().split("\\s+");
    }

    private void printMenu() {
        out.println("""
                Available commands:
                    register: <username> <password> <email> - Register a new user.
                    login: <username> <password> - Login an existing user.
                    quit: Quit the client.
                    help: Show this menu
                """);
    }

    public static void main(String[] args) {
        String serverName = args.length > 0 ? args[0] : "localhost:8080";
        ServerFacade facade = new ServerFacade(serverName);
        loginREPL loginREPL = new loginREPL(facade);
        loginREPL.run();
    }
}
