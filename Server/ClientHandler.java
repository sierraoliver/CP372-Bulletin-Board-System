//imports
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

public class ClientHandler implements Runnable{
    private Socket socket;
    private Board board;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, Board board){
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // send handshake info to client
            out.println("BOARD " + board.get_width() + " " + board.get_height());
            out.println("NOTE_SIZE " + board.get_noteW() + " " + board.get_noteH());
            out.println("COLORS " + String.join(" ", board.get_colors()));
            out.println("READY");

            // read commands in loop
            String command;
            while ((command = in.readLine()) != null) {
                String response = parseAndExecute(command);
                if (response != null) {
                    out.println(response);
                }

                if (command.trim().equals("DISCONNECT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly");
        } finally {
            cleanup();
        }
    }

    /*
        Parses incoming command and calls the appropriate Board method
        Parameters:
            command - the raw command string from the client
        Returns:
            response - the server response string to send back
    */
    private String parseAndExecute(String command) {
        try {
            // split command into parts
            String[] parts = command.trim().split(" ");
            String commandType = parts[0].toUpperCase();

            switch (commandType) {
                case "POST":
                    return handlePost(parts);

                case "GET":
                    return handleGet(parts);

                case "PIN":
                    return handlePin(parts);

                case "UNPIN":
                    return handleUnpin(parts);

                case "SHAKE":
                    return board.SHAKE();

                case "CLEAR":
                    return board.CLEAR();

                case "DISCONNECT":
                    return null;

                default:
                    return "ERROR UNKNOWN_COMMAND";
            }
        } catch (Exception e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    /*
        Parses and validates POST command before calling Board
        Parameters:
            parts - the split command string array
        Returns:
            response - success or error message from Board
    */
    private String handlePost(String[] parts) {
        // POST <x> <y> <color> <message>
        if (parts.length < 5) {
            return "ERROR INVALID_FORMAT POST requires x, y, color, and message";
        }

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        String color = parts[3];

        // rejoin remaining parts
        String message = String.join(" ", java.util.Arrays.copyOfRange(parts, 4, parts.length));

        return board.POST(x, y, color, message);
    }

    /*
        Parses and validates GET command before calling Board
        Parameters:
            parts - the split command string array
        Returns:
            response - matching notes/pins or NO_RESULTS
    */
    private String handleGet(String[] parts) {
        // check if GET PINS
        if (parts.length > 1 && parts[1].equals("PINS")) {
            return board.GET_PINS();
        }

        String color = null;
        Integer x = null, y = null;
        String substring = null;

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("color=")) {
                color = parts[i].substring(6); // remove "color="

            } else if (parts[i].startsWith("contains=")) {
                x = Integer.parseInt(parts[i].substring(9)); // remove "contains="
                y = Integer.parseInt(parts[i + 1]); 
                i++; 

            } else if (parts[i].startsWith("refersTo=")) {
                substring = parts[i].substring(9); // remove "refersTo="
            }
        }

        return board.GET(color, x, y, substring);
    }

    /*
        Parses and validates PIN command before calling Board
        Parameters:
            parts - the split command string array
        Returns:
            response - success or error message from Board
    */
    private String handlePin(String[] parts) {
        // PIN <x> <y>
        if (parts.length != 3) {
            return "ERROR INVALID_FORMAT PIN requires x and y coordinates";
        }

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        return board.PIN(x, y);
    }

    /*
        Parses and validates UNPIN command before calling Board
        Parameters:
            parts - the split command string array
        Returns:
            response - success or error message from Board
    */
    private String handleUnpin(String[] parts) {
        // UNPIN <x> <y>
        if (parts.length != 3) {
            return "ERROR INVALID_FORMAT UNPIN requires x and y coordinates";
        }

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        return board.UNPIN(x, y);
    }

    // cleans up socket and streams when client disconnects
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Client connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
      
}
