package ui;

import client.ServerFacade;

import java.util.Scanner;

import static java.lang.System.out;

public class loginREPL {
    ServerFacade facade;

    public loginREPL(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        boolean loggedIn = false;
        out.print("Chess Game Started. Enter 'help' to get started.");
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

                case "quit":
                    out.println("Bye!");
                    return;

                case "help":
                    printMenu();
                    break;

            }
        }
    }

    private String[] getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().split(" ");
    }

    private void printMenu() {
        out.println("This should print the help menu");
    }

    public static void main(String[] args) {
        String serverName = args.length > 0 ? args[0] : "localhost:8080";
        ServerFacade facade = new ServerFacade(serverName);
        loginREPL loginREPL = new loginREPL(facade);
        loginREPL.run();
    }
}
