
public class Note{
    private int x;
    private int y;
    private String color;
    private boolean pinned;
    private int num_pins;
    private String message;

    public Note(int x, int y, String color,  boolean pinned, int num_pins, String message){
        this.x = x;
        this.y = y;
        this.color = color;
        this.pinned = pinned;
        this.num_pins = num_pins;
        this.message = message;

    }

    //getters
    public int getX() { return x; }
    public int getY() { return y; }
    public String get_color() { return color; }
    public String get_message() { return message; }
    public int get_num_pins() { return num_pins; }
    public boolean get_pin_status() { return pinned; }

    //setters
    public void setX(int x) {
        this.x = x;
        return; 
    }
    public void setY(int y) { 
        this.y = y;
        return; 
    }
    public void set_color(String color) { 
        this.color = color;
        return; 
    }
    public void set_message(String message) { 
        this.message = message;
        return; 
    }
    public void set_num_pins(int num) { 
        this.num_pins = num;
        return; 
    }
    public void set_pin_status(boolean pinned) { 
        this.pinned = pinned;
        return; 
    }

    //helper methods
    public boolean contains(int px, int py, int noteWidth, int noteHeight) {
        return px >= x && px < x + noteWidth && 
               py >= y && py < y + noteHeight;
    }

}