package canvas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Canvas represents a drawing surface that allows the user to draw
 * on it freehand, with the mouse.
 */
@SuppressWarnings("serial")
public class Canvas extends JPanel {
    // image where the user's drawing is stored
    private Image drawingBuffer;
    //black: 0, red: 1, blue: 2, yellow: 3, green 4
    private boolean eraserState = false;
    public ColorType color = ColorType.BLACK;
    private ButtonGroup colorButtons;

    private JRadioButton eraseRadio;
    private JRadioButton blackRadio;
    private JRadioButton redRadio;
    private JRadioButton blueRadio;
    private JRadioButton yellowRadio;
    private JRadioButton greenRadio;
    private JSlider penSizeSlider;
    private JLabel penSizeLabel;
    private int minPenSize = 0;
    private int maxPenSize = 20;
    public int currentPenSize;
    private boolean boardSelected = false;
    private int invalidClickCount = 0;

    public CanvasClient canvasClient = null;

    private ArrayList<String> pointList;
    /**
     * Make a canvas.
     * @param width width in pixels
     * @param height height in pixels
     */
    public Canvas(int width, int height, WhiteboardFrame frame, CanvasClient client) {
        this.setPreferredSize(new Dimension(width, height));
        addDrawingController();

        //Create color buttons
        eraseRadio = new JRadioButton("eraser");
        eraseRadio.setSelected(true);
        eraseRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.ERASE
             */
            public void actionPerformed(ActionEvent e) {
                eraserState = true;
                color = ColorType.ERASE;
            }
        });

        blackRadio = new JRadioButton("black");
        blackRadio.setSelected(true);
        blackRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.BLACK
             */
            public void actionPerformed(ActionEvent e){
                eraserState = false;
                color = ColorType.BLACK;
            }
        });
        redRadio = new JRadioButton("red");
        redRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.RED
             */
            public void actionPerformed(ActionEvent e){
                eraserState = false;
                color = ColorType.RED;
            }
        });
        blueRadio = new JRadioButton("blue");
        blueRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.BLUE
             */
            public void actionPerformed(ActionEvent e){
                eraserState = false;
                color = ColorType.BLUE;
            }
        });
        yellowRadio = new JRadioButton("yellow");
        yellowRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.YELLOW
             */
            public void actionPerformed(ActionEvent e){
                eraserState = false;
                color = ColorType.YELLOW;
            }
        });
        greenRadio = new JRadioButton("green");
        greenRadio.addActionListener(new ActionListener() {
            /**
             * Sets the color variable to ColorType.GREEN
             */
            public void actionPerformed(ActionEvent e){
                eraserState = false;
                color = ColorType.GREEN;
            }
        });

        //Add to button group so only one can be selected at a time.
        colorButtons = new ButtonGroup();
        colorButtons.add(blackRadio);
        colorButtons.add(redRadio);
        colorButtons.add(blueRadio);
        colorButtons.add(yellowRadio);
        colorButtons.add(greenRadio);
        colorButtons.add(eraseRadio);

        //Add buttons to the canvas
        this.add(eraseRadio);
        this.add(blackRadio);
        this.add(redRadio);
        this.add(blueRadio);
        this.add(yellowRadio);
        this.add(greenRadio);


        //Create the pen size slider
        currentPenSize = 3; //defaults to medium size
        penSizeSlider = new JSlider(JSlider.HORIZONTAL,minPenSize,maxPenSize,currentPenSize);
        penSizeSlider.setMajorTickSpacing(5);
        penSizeSlider.setMinorTickSpacing(1);
        penSizeSlider.setPaintTicks(true);
        penSizeSlider.setPaintLabels(true);
        penSizeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                if(!source.getValueIsAdjusting()){
                    currentPenSize = source.getValue();
                }
            }

        });
        this.add(penSizeSlider);

        penSizeLabel = new JLabel("Adjust pen size");
        penSizeLabel.setName("penSizeLabel");
        this.add(penSizeLabel);

        //Stores the points in the line currently being drawn
        pointList = new ArrayList<String>();

        canvasClient = client;

        // note: we can't call makeDrawingBuffer here, because it only
        // works *after* this canvas has been added to a window.  Have to
        // wait until paintComponent() is first called.
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    public void paintComponent(Graphics g) {
        // If this is the first time paintComponent() is being called,
        // make our drawing buffer.
        if (drawingBuffer == null) {
            makeDrawingBuffer();
        }

        // Copy the drawing buffer to the screen.
        g.drawImage(drawingBuffer, 0, 0, null);
        g.setColor(Color.BLACK);

    }


    /**
     * Make the drawing buffer and draw some starting content for it.
     */
    private void makeDrawingBuffer() {
        drawingBuffer = createImage(getWidth(), getHeight());
        fillWithWhite();
    }

    /**
     * Make the drawing buffer entirely white.
     */
    public void fillWithWhite() {
        final Graphics2D g = (Graphics2D) drawingBuffer.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0,  0,  getWidth(), getHeight());

        // IMPORTANT!  every time we draw on the internal drawing buffer, we
        // have to notify Swing to repaint this component on the screen.
        this.repaint();
    }


    /**
     * Draw a line between two points (x1, y1) and (x2, y2), specified in
     * pixels relative to the upper-left corner of the drawing buffer.
     * 
     * @param x1 - from x
     * @param y1 - from y
     * @param x2 - to x
     * @param y2 - to y
     * @param localDraw boolean to indicate local draw request or server sent this request for redraw
     */
    public void drawLineSegment(int x1, int y1, int x2, int y2, boolean localDraw) {
        Graphics2D g = (Graphics2D) drawingBuffer.getGraphics();
        if(!boardSelected){
            // If the user attempts to draw directly on the canvas without first selecting a board, 
            // increasingly irritated reminders are output.
            switch(invalidClickCount){
            case 0: 
                JOptionPane.showMessageDialog(this,"Choose a whiteboard to begin drawing.");
                break;
            case 1: 
                JOptionPane.showMessageDialog(this,"Please choose a whiteboard to begin drawing!");
                break;
            case 2:
                JOptionPane.showMessageDialog(this,"Look to the right hand side to choose a whiteboard");
                break;
            case 3:
                JOptionPane.showMessageDialog(this,"Did you read the other messages? Choose a whiteboard to begin drawing!");
                break;
            default:
                JOptionPane.showMessageDialog(this,"JUST CHOOSE A WHITEBOARD! >_<");
                break;
            }

            invalidClickCount++;
        }
        else{ 
            //Set the color to white if the user is drawing with an eraser locally.
            if (eraserState && localDraw) {
                g.setColor(Color.WHITE);
            }
            else{
                //Otherwise set color based on the color variable.
                switch(color){
                case BLACK: g.setColor(Color.BLACK);  break;                  
                case RED: g.setColor(Color.RED);  break;
                case BLUE: g.setColor(Color.BLUE); break;
                case YELLOW: g.setColor(Color.YELLOW); break;
                case GREEN: g.setColor(Color.GREEN); break;
                case ERASE: g.setColor(Color.WHITE); break;
                }
            }
            //If the currentPenSize is set to 0, default the size to 1.
            if(currentPenSize==0){
                g.setStroke(new BasicStroke(1));
            }
            //Otherwise set the stroke size equal to the currentPenSize.
            else{
                g.setStroke(new BasicStroke(currentPenSize));
            }
            g.drawLine(x1, y1, x2, y2);
        }
        // IMPORTANT!  every time we draw on the internal drawing buffer, we
        // have to notify Swing to repaint this component on the screen.
        this.repaint();
    }

    /**
     * Add the mouse listener that supports the user's freehand drawing.
     */
    private void addDrawingController() {
        DrawingController controller = new DrawingController();
        addMouseListener(controller);
        addMouseMotionListener(controller);
    }


    /**
     * DrawingController handles the user's freehand drawing.
     */
    private class DrawingController implements MouseListener, MouseMotionListener {
        // store the coordinates of the last mouse event, so we can
        // draw a line segment from that last point to the point of the next mouse event.
        private int lastX, lastY; 

        /**
         * When mouse button is pressed down, start drawing. 
         * Add the current point to the list of Points.
         */
        public void mousePressed(MouseEvent e) {
            lastX = e.getX();
            lastY = e.getY();
            pointList = new ArrayList<String>();
            pointList.add(new Point(lastX,lastY).toString());
        }

        /**
         * When mouse moves while a button is pressed down,
         * draw a line segment.
         * Add the current point to the list of Points.
         */
        public void mouseDragged(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            drawLineSegment(lastX, lastY, x, y, true);
            lastX = x;
            lastY = y;
            pointList.add(new Point(lastX,lastY).toString());

        }

        /**
         * Send a free draw request to the server.
         * Then reset the pointList
         */
        public void mouseReleased(MouseEvent e) { 
            String colorString = "";
            switch(color){
            case BLACK: colorString = "black"; break;
            case RED: colorString = "red"; break;
            case BLUE: colorString = "blue"; break;
            case YELLOW: colorString = "yellow"; break;
            case GREEN: colorString = "green"; break;
            case ERASE: break;
            }
            if (eraserState) {
                colorString = "white";
            }
            canvasClient.sendFreeDrawRequest(colorString, currentPenSize, pointList);
            // reset pointList after mouse has been released
            pointList = new ArrayList<String>();
        }
        // Ignore all these other mouse events.
        public void mouseMoved(MouseEvent e) { }
        public void mouseClicked(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
    }

    /**
     * Sets boardSelected equal to true the first time a board is selected.
     */
    public void setBoardSelected(){
        boardSelected = true;
    }

    /**
     * Set the color of the drawing utensil to the given int.
     * 
     * @param newColor  an int representing the color to be switched to
     */
    public void setColor(ColorType newColor){
        color = newColor;
    }

    /**
     * Set the currentPenSize of the drawing utensil to the given int.
     * 
     * @param newPenSize  an int representing the pen size to be switched to
     */
    public void setPenSize(int newPenSize){
        currentPenSize = newPenSize;
    }

    /**
     * Return the boolean that represents whether or not is the current utensil 
     * of choice is not.
     * 
     * @return eraserState	boolean, true if the eraser is on and false if it not
     */
    public boolean getEraserState(){
        return eraserState;
    }

    /**
     * Used to keep track of coordinates and override the toString method
     * for a more suitable String representation.
     *
     */
    class Point {
        private int x;
        private int y;
        public Point(int xCoord, int yCoord){
            x = xCoord;
            y = yCoord;
        }
        @Override
        public String toString(){
            return x+":"+y;
        }
    }


    /*
     * Main program. Make a window containing a Canvas.
     */
    public static void main(String[] args) {
        // set up the UI (on the event-handling thread)
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                WhiteboardFrame window = new WhiteboardFrame("Freehand Canvas"); 
                window.pack();
                window.setVisible(true);
            }
        });
    }
}