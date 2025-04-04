import server.Server;

public class Main {

    public static void main(String[] args) {
        Server server = new Server();
        server.run(8081);
    }
}

// confirm resign
// leave message should send to all clients.
// highlight moves