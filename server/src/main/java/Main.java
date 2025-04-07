import server.Server;

public class Main {

    public static void main(String[] args) {
        Server server = new Server();
        server.run(8081);
    }
}

// double leave, people in game need leave message

// leave should take out of in game loop, and send to out of game loop. and send message to other client that they have left the game.
// mis mathc with resign and game loop.

