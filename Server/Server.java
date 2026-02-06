//imports
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class Server {
    public static void main(String[] args) {
        final int BOARD_WIDTH = 1000;
        final int BOARD_HEIGHT = 700;
        final int NOTE_WIDTH = 50;
        final int NOTE_HEIGHT = 150;
        final List <String> COLORS = Arrays.asList("red", "yellow", "green", "blue");

        if (args.length == 0){
            System.err.println("Run program on server with format: java Server <port>");
            System.exit(1);
        }

        int port = 0;
        try{
            port = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e){
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
        }
        
        // Create shared board
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, NOTE_WIDTH, NOTE_HEIGHT, COLORS);
        
        // Start server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);
            System.out.println("To stop server press Ctrl+C");
            System.out.println("---------------------------------------------------");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Spawn new thread for this client
                new Thread(new ClientHandler(clientSocket, board)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
