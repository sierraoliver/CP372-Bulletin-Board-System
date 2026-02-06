// board implementation

//imports 
import java.util.List;
import java.util.ArrayList;

public class Board {
    private int width, height;
    private int noteWidth, noteHeight;
    private List <String> validColors;
    private List <Note> notes;
    private List <Pin> pins;

    //board constructor
    public Board(int width, int height, int noteW, int noteH, List<String> colors){
        this.width = width;
        this.height = height;
        this.noteWidth = noteW;
        this.noteHeight = noteH;
        this.validColors = colors;
        this.notes = new ArrayList<>();
        this.pins = new ArrayList<>();
    }

    //getters
    public int get_width() { return width; }
    public int get_height() { return height; }
    public int get_noteW() { return noteWidth; }
    public int get_noteH() { return noteHeight; }
    public List<String> get_colors() { return validColors; }
    public List<Note> get_notes() { return notes; }
    public List<Pin> get_pins() { return pins; }

    /*
        Adds note to board if it passes all error checks
        Parameters:
            x - non-negative integer representing the x coordinate
            y - non-negative integer representing the y coordinate
            color - text representing the color of the note
            message - string to put put on the note
        Returns:
            response - either error message or success message
    */
    public synchronized String POST (int x, int y, String color, String message){
        //check non-negative integers
        if (x<0 || y<0){
            return "ERROR INVALID_FORMAT requires non-negative integer coordinates";
        }

        //validate bounds
        if (x + noteWidth > width || y + noteHeight > height){
            return "ERROR OUT_OF_BOUNDS note needs to be placed within the board boundaries";
        }

        //validate color
        if(!validColors.contains(color)){
            return "ERROR COLOR_NOT_SUPPORTED color " + color + " is not a valid color";
        }

        //check for complete overlap of notes
        for (Note existing: notes){
            if (existing.getX() == x && existing.getY() == y){
                return "ERROR COMPLETE_OVERLAP notes cannot overlap existing notes";
            }
        }

        //add note
        notes.add(new Note (x, y, color, false, 0, message));
        return "OK POST " + x + " " + y;

    }

    /*
        Returns all existing pins
        Parameters:
            None
        Returns:
            response - either error message or message of all pins
    */
    public synchronized String GET_PINS(){
        String response = "";

        //if no pins exists
        if (pins.size() == 0){
            return "NO_RESULTS no current pins exist on the board";
        }

        for (Pin existing : pins){
            response += "PIN " + existing.getX() + " " + existing.getY() + "\n";
        }

        return response;
    }

    /*
        Returns all notes that match potential filters
        Parameters:
            color - color of note
            x - x coordinate of interest
            y - y coordiante of interest
            substring - text to be contained in note messages
        Returns:
            response - either error message or message of all notes that match potential filters
    */
    public synchronized String GET (String color, Integer x, Integer y, String substring){
        String response = "";

        for (Note note : notes){
            //apply filters only if provided

            if (color != null && !note.get_color().equals(color)){
                continue;
            }

            if(x!=null && y !=null && !note.contains(x, y, noteWidth, noteHeight)){
                continue;
            }

            if (substring != null && !note.get_message().contains(substring)){
                continue;
            } 

            response += "NOTE " + note.getX() + " " + note.getY() + " " + note.get_pin_status() + " " + note.get_message() + "\n";

        }

        if (response.isEmpty()){
            return "NO_RESULTS";
        }

        return response;
    }

    /*
        Pins note at specified coordinate if a note exists there
        Parameters:
            x - x coordinate of interest
            y - y coordiante of interest
        Returns:
            response - either error message or success message
    */
    public synchronized String PIN(int x, int y){
        if (x<0 || y<0){
            return "ERROR INVALID_FORMAT requires non-negative integer coordinates";
        }

        if (x >= width || y >= height){
            return "ERROR OUT_OF_BOUNDS pin needs to be placed within the board boundaries";
        }

        // Check if pin already exists at this coordinate
        for (Pin pin : pins) {
            if (pin.getX() == x && pin.getY() == y) {
                return "ERROR INVALID_FORMAT pin already exists at this coordinate";
            }
        }

        boolean found = false;
        for(Note note: notes){
            if (note.contains(x, y, noteWidth, noteHeight)){
                note.set_num_pins(note.get_num_pins()+1);
                if (!note.get_pin_status()){
                    note.set_pin_status(true);
                }
                found = true;
                // DON'T return here — keep looping to pin ALL overlapping notes
            }
        }

        if (!found){
            return "ERROR NO_NOTE_AT_COORDINATE pin must be placed within an existing note";
        }

        pins.add(new Pin(x,y));
        return "OK PIN " + x + " " + y;
    }

    /*
        Unpins note at specified coordinate if a pin exists there
        Parameters:
            x - x coordinate of interest
            y - y coordiante of interest
        Returns:
            response - either error message or success message
    */
    public synchronized String UNPIN (int x, int y){
        //check non-negative integers
        if (x<0 || y<0){
            return "ERROR INVALID_FORMAT requires non-negative integer coordinates";
        }

        //validate bounds
        if (x + noteWidth > width || y + noteHeight > height){
            return "ERROR OUT_OF_BOUNDS coordinates needs to be within the board boundaries";
        }  

        //check if pin exists
        for (Pin pin: pins){
            if (pin.getX()==x && pin.getY()==y){
                pins.remove(pin);

                //if pin removed, update note(s) that include that pin
                for (Note note: notes){
                    if (note.contains(x, y, noteWidth, noteHeight)){
                        note.set_num_pins(note.get_num_pins()-1);
                        if (note.get_num_pins() == 0){
                            note.set_pin_status(false);
                        }
                    }
                }

                return "OK UNPIN " + x + " " + y;
            }
        }

        return "ERROR PIN_NOT_FOUND no pin at designated coordinates";

    }

    /*
        Removes all unpinned notes from the board
        Parameters:
            None
        Returns:
            response - message indicating it ran properly and the number of notes removed
    */
    public synchronized String SHAKE() {
        int count = 0;

        java.util.Iterator<Note> it = notes.iterator();
        while (it.hasNext()) {
            Note note = it.next();
            if (!note.get_pin_status()) {
                it.remove();
                count++;
            }
        }

        return "OK SHAKE " + count;
    }

    /*
        Clears the board - i.e removes all pins and notes from the board
        Parameters:
            None
        Returns:
            response - message indicating if it ran properly
    */
    public synchronized String CLEAR(){
        notes.clear();
        pins.clear();

        return "OK CLEAR";
    }

}
