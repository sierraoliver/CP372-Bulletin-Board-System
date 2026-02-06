//imports
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Server {
    public static void main(String[] args) {
        // Check minimum number of arguments
        // <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>
        if (args.length < 6){
            System.err.println("Usage: java Server <port> <board_width> <board_height> <note_width> <note_height> <color1> [color2] [color3] ...");
            System.err.println("Example: java Server 4554 200 100 20 10 red white green yellow");
            System.exit(1);
        }

        // Parse command line arguments
        int port = 0;
        int boardWidth = 0;
        int boardHeight = 0;
        int noteWidth = 0;
        int noteHeight = 0;
        
        try {
            port = Integer.parseInt(args[0]);
            boardWidth = Integer.parseInt(args[1]);
            boardHeight = Integer.parseInt(args[2]);
            noteWidth = Integer.parseInt(args[3]);
            noteHeight = Integer.parseInt(args[4]);
        } catch(NumberFormatException e) {
            System.err.println("Invalid numeric argument: " + e.getMessage());
            System.exit(1);
        }
        
        // Parse colors (everything from args[5] onwards)
        List<String> colors = new ArrayList<>();
        for (int i = 5; i < args.length; i++) {
            colors.add(args[i]);
        }

        // Validate arguments
        if (port < 1024 || port > 65535) {
            System.err.println("Port must be between 1024 and 65535");
            System.exit(1);
        }
        
        if (boardWidth <= 0 || boardHeight <= 0) {
            System.err.println("Board dimensions must be positive");
            System.exit(1);
        }
        
        if (noteWidth <= 0 || noteHeight <= 0) {
            System.err.println("Note dimensions must be positive");
            System.exit(1);
        }
        
        if (colors.isEmpty()) {
            System.err.println("At least one color must be specified");
            System.exit(1);
        }
        
        // Create shared board
        Board board = new Board(boardWidth, boardHeight, noteWidth, noteHeight, colors);
        
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
