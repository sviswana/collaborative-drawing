package canvas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import controller.ParseMessage;
/**
 * A class that represents the frame of the whiteboard. Contains the canvas as well as
 * all of the components that assist in inputing/storing information about the 
 * board and/or the user.
 *
 */
@SuppressWarnings("serial")
public class WhiteboardFrame extends JFrame {
    private Canvas canvas;

    private JLabel usernameLabel;
    private JLabel boardLabel;
    private JLabel serverIPLabel;
    private JLabel serverPortLabel;
    private DefaultTableModel currentUserModel;

    private JTable currentUserTable;
    // // rep invariant
    // Contains list of all users on the currently selected board

    private DefaultTableModel currentBoardModel;
    private JTable currentBoardTable;
    // rep invariant
    // Contains list of all boards available on the server.

    private GroupLayout layout;
    private JButton logout;
    private JScrollPane boardScrollPane;
    private JScrollPane userScrollPane;

    private JLabel newBoardPrompt1;
    private JLabel newBoardPrompt2;
    private JButton newBoardButton;

    private String username = "";
    private String boardName = "";
    private String serverIP = "";
    private int serverPort = 4444;
    private WhiteboardFrame thisFrame;

    //Stores the names of the boards and the current users
    ArrayList<String> boards = new ArrayList<String>();
    // rep invariant
    // Contains list of all boards available on the server.

    ArrayList<String> users = new ArrayList<String>();
    // rep invariant
    // Contains list of all users on the currently selected board

    private CanvasClient canvasClient = null;
    public WhiteboardFrame(String name) {
        super(name);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setSize(800,600);
        this.setResizable(false);

        thisFrame = this;

        //Create logout button
        logout = new JButton("Logout");
        logout.setPreferredSize(new Dimension(200,100));
        logout.addActionListener(new ActionListener() {
            /**
             * Prompts the user to verify that they want to logout.
             * If they click yes, then the user will be logged out and the
             * program will end.
             */
            public void actionPerformed(ActionEvent e){
                int answer = JOptionPane.showConfirmDialog(thisFrame,
                        "Are you sure you would like to logout? This will end the program.", "Confirm Logout",
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION){
                    canvasClient.sendLogoutRequest();
                    checkRep();
                    endProgram();
                }            
            }
        });


        //Create a label to prompt the user to click on the following button
        //in order to create a new board.
        newBoardPrompt1 = new JLabel("Click on the button to create a new whiteboard or access");
        newBoardPrompt1.setName("newBoardPrompt1");
        newBoardPrompt1.setPreferredSize(new Dimension(100,100));
        newBoardPrompt2 = new JLabel("an existing board by selecting it from the 'Available Boards' table");
        newBoardPrompt2.setName("newBoardPrompt2");
        newBoardPrompt2.setPreferredSize(new Dimension(100,100));

        newBoardButton = new JButton("Create new whiteboard");
        newBoardButton.setName("boardDone");   
        newBoardButton.addActionListener(new ActionListener(){
            /**
             * On button click, calls setBoard() which prompts the user to 
             * enter a board name.
             */
            public void actionPerformed(ActionEvent e){
                setBoard();
            }
        });

        //Creating current user table
        String columnNames[] = {"Users on current board"};
        Object[][] noUserData = {};
        currentUserModel = new DefaultTableModel(noUserData, columnNames){
            /**
             * Ensures that the text in the cells is not editable directly.
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };
        currentUserTable = new JTable(currentUserModel);
        userScrollPane = new JScrollPane(currentUserTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add(userScrollPane);

        // Need to limit the size of these tables. Otherwise, it draws too big
        userScrollPane.setMaximumSize(new Dimension(250,220));

        //Fills in details about the currentUserTable
        currentUserTable.setFillsViewportHeight(true);
        currentUserTable.setName("currentUserTable");
        Color ivory = new Color(255, 255, 208);
        currentUserTable.setBackground(ivory);
        currentUserTable.setMinimumSize(new Dimension(260,230));


        //Creating current board table
        String boardColumnNames[] = {"Available boards on server"};
        Object[][] noBoardData = {};

        currentBoardModel = new DefaultTableModel(noBoardData, boardColumnNames){
            /**
             * Ensures that the cells in the tables are not editable directly.
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };

        currentBoardTable = new JTable(currentBoardModel);
        boardScrollPane = new JScrollPane(currentBoardTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add(boardScrollPane);

        // Need to limit the size of these tables. Otherwise, it draws too big
        boardScrollPane.setMaximumSize(new Dimension(250,220));

        //Fills in details about the currentBoardTable
        currentBoardTable.setFillsViewportHeight(true);
        currentBoardTable.setName("currentUserTable");
        Color mint = new Color(204,255, 229);
        currentBoardTable.setBackground(mint);
        currentBoardTable.setMinimumSize(new Dimension(260,230));
        currentBoardTable.addMouseListener(new MouseAdapter(){
            /**
             * On a mouse click, the user is disconnected from their current
             * table and switched to the table that they selected from the list.
             */
            public void mouseClicked(MouseEvent e){
                int row = currentBoardTable.getSelectedRow();
                int column = currentBoardTable.getSelectedColumn();
                if(row >= 0){ // look for only valid selection
                    String newBoard = (String)currentBoardModel.getValueAt(row, column);
                    if(newBoard != null){
                        if (boardName != null) {
                            // close existing board first
                            canvasClient.sendCloseBoardRequest();
                            checkRep();
                        }
                        //Updates the  boardName
                        boardName = newBoard;
                        //Clears the canvas
                        canvas.fillWithWhite();
                        canvasClient.sendOpenBoardRequest(boardName);
                        checkRep();
                        boardLabel.setText("Current board: "+boardName);
                        canvas.setBoardSelected();
                    }

                }

            }
        });

        //Sets labels for relevant information
        usernameLabel = new JLabel("Welcome!");
        int fontSize = 20;
        String fontStyle = usernameLabel.getFont().getName();
        usernameLabel.setFont(new Font(fontStyle, Font.BOLD, fontSize));
        boardLabel = new JLabel("Current board: ");
        int fontSizeBoard = 16;
        String fontStyleBoard = boardLabel.getFont().getName();
        boardLabel.setFont(new Font(fontStyleBoard, Font.BOLD, fontSizeBoard));
        serverIPLabel = new JLabel("Server IP: ");
        serverPortLabel = new JLabel("Server Port: "+serverPort);

        //Prompts the user for the serverIP then sets up the client.
        setServerIP();
        canvasClient = new CanvasClient(thisFrame, serverIP, serverPort);
        //Prompts the user to enter a username
        setUsername();

        //Creates the canvas that will be used to draw on.
        canvas = new Canvas(800, 600, this, canvasClient);


        thisFrame.addWindowListener(new java.awt.event.WindowAdapter() {

            /**
             * Gives the user a confirmation pop up when he or she tries to 
             * close the window. If they click the 'Yes' option, then they 
             * will be logged out and the program will end.
             */
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int answer = JOptionPane.showConfirmDialog(thisFrame,
                        "Would you like to exit the program?", "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION){
                    canvasClient.sendLogoutRequest();
                    checkRep();
                    endProgram();
                }
            }
        });


        //Set layout
        layout = new GroupLayout(this.getContentPane());
        this.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(canvas)
                .addGroup(layout.createParallelGroup()
                        .addComponent(usernameLabel)
                        .addComponent(boardLabel)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(serverIPLabel)
                                .addComponent(serverPortLabel))

                                .addComponent(logout)
                                .addComponent(newBoardPrompt1)
                                .addComponent(newBoardPrompt2)
                                .addComponent(newBoardButton)
                                .addComponent(boardScrollPane)
                                .addComponent(userScrollPane))
                );
        layout.setVerticalGroup(layout.createParallelGroup()
                .addComponent(canvas)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(usernameLabel)
                        .addComponent(boardLabel)
                        .addGroup(layout.createParallelGroup()
                                .addComponent(serverIPLabel)
                                .addComponent(serverPortLabel))

                                .addComponent(logout)
                                .addComponent(newBoardPrompt1)
                                .addComponent(newBoardPrompt2)
                                .addComponent(newBoardButton)
                                .addComponent(boardScrollPane)
                                .addComponent(userScrollPane))
                );

    }
    /**
     * Opens a prompt for the user to input a username, and updates 
     * the board accordingly. Also sends a login request to the server.
     */
    private void setUsername(){
        username = "";
        usernameLabel.setText("Please enter your username");
        username = (String) JOptionPane.showInputDialog("Please enter your username:");
        //If the user presses cancel or exits out, the program will terminate.
        if(username == null){
            endProgram();
        }
        else if (!ParseMessage.isValidName(username)) {
            JOptionPane.showMessageDialog(this, "Names must start with a letter. Only letters and numbers allowed in the name. A limit of 15 characters.");
            setUsername();
        }
        usernameLabel.setText("Welcome, "+username+"!");

        //Sends a login request to the server
        canvasClient.sendLoginRequest(username);
        checkRep();
    }
    /**
     * Prompts the user to input an IP address and updates the board.
     */
    private void setServerIP(){
        serverIP = "";
        serverIPLabel.setText("none");
        serverIP = (String) JOptionPane.showInputDialog("Please enter the IP address of the server: ");
        //If the user presses cancel or exits out, the program will terminate.
        if(serverIP == null){
            endProgram();
        }
        serverIPLabel.setText("Server IP: "+serverIP);
        canvasClient = new CanvasClient(this, serverIP, serverPort);
    }


    /**
     * Used for creating a new board. Prompts the user to enter the name of
     * a board to be created. If the board already exists, give the user the
     * appropriate message. If not, clear the whiteboard and send a new board
     * request and an open board request to the server. Update the frame info
     * accordingly.
     */
    private void setBoard() {
        String oldBoardName = boardName;
        boardName = JOptionPane.showInputDialog("Please enter the name of your new board");

        //If the board is a duplicate, the display a message to the user.
        if(boards.contains(boardName)){
            JOptionPane.showMessageDialog(this, "That board already exists. Please select it from the table, or input a new board name.");
        }
        else if(boardName == null){
            // restore the old name as user cancelled or did not type in any thing in the dialog box
            // stay with the current board
            boardName = oldBoardName;
        }
        else if (!ParseMessage.isValidName(boardName)) {
            JOptionPane.showMessageDialog(this, "Names must start with a letter. Only letters and numbers allowed in the name. A limit of 15 characters.");
            setBoard();
        }
        else{
            if (oldBoardName != null) {
                // Need to close the old board
                canvasClient.sendCloseBoardRequest();
                checkRep();
            }
            //Clear the canvas
            canvas.fillWithWhite();
            //Send requests to the server to create and open a new board.
            canvasClient.sendNewBoardRequest(boardName);
            checkRep();
            canvasClient.sendOpenBoardRequest(boardName);
            checkRep();
            boardLabel.setText("Current board: "+boardName);
            canvas.setBoardSelected();
        }

    }

    /** 
     * Called if the user tried to create a username that is already in use.
     * Notifies the user, then calls setUsername() to prompt the user to
     * input another username.
     */
    public void duplicateUsername(){
        //If the user clicks cancel or exits the window, terminate the program
        if(username == null) {
            endProgram();
        }
        username = "";
        usernameLabel.setText("Please enter your username");
        //Send a message to the user notifying them that their username is taken
        //Prompt the user to enter another username.
        JOptionPane.showMessageDialog(this,"Sorry that username is taken. Please input another one.");
        setUsername();
    }

    /** 
     * Called when the user enters an IP address that is invalid.
     * Prompts the user to do both again.
     */
    public void invalidIP(){
        JOptionPane.showMessageDialog(this, "Invalid IP address. Please try again.");
        setServerIP();
    }

    public void connectionError(String error) {
        JOptionPane.showMessageDialog(thisFrame, error);
        endProgram();
    }

    /**
     * Ends the program.
     */
    public void endProgram(){
        System.exit(0);
    }

    /**
     * Return the Canvas object stored in canvas.
     * 
     * @return  Canvas canvas - represents the drawing space
     */
    public Canvas getCanvas(){
        return canvas;
    }

    /**
     * Updates both the board table and the user table using information
     * given by the client.
     * @param map HashMap with board name as key and users on each board as value
     */
    public void updateBoardAndUserTables(HashMap<String, ArrayList<String>> map) {
        ArrayList<String> boardList = new ArrayList<String>();  
        ArrayList<String> userlist = new ArrayList<String>();
        if (map != null) {
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                String bName = entry.getKey(); // board name is key
                boardList.add(bName);
                if (bName.equals(boardName)) {
                    userlist = entry.getValue(); // user list is the value
                    ///updateUserTable(userlist);
                }
            }
        }
        //alphabetize boards
        Collections.sort(users);
        Collections.sort(boards);
        updateUserTable(userlist);
        updateBoardTable(boardList);

    }

    /**
     * Updates the currentBoardModel with the given list of boards.
     * 
     * @param boardList an array list with the current boards that need to be
     *          added to the table.
     */
    public void updateBoardTable(ArrayList<String> boardList) {
        boards = boardList;
        //alphabetize boards
        Collections.sort(boards);
        int totalRows = currentBoardModel.getRowCount();
        //System.out.println("board updating"+boardList);
        // remove existing rows of the table
        for (int i=totalRows; i >0; i--)
            currentBoardModel.removeRow(i-1);

        //Adds all of the current boards to the table
        if (boardList.size() > 0) {
            for (String s : boardList)
                currentBoardModel.addRow(new Object[] {s});
        }
    }



    /**
     * Updates the currentUserModel with the given list of users.
     * 
     * @param userList an array list with the current users that are
     *      on the current board.
     */
    public void updateUserTable(ArrayList<String> userList){
        users = userList;
        Collections.sort(userList);
        int totalRows = currentUserModel.getRowCount();
        //System.out.println("users updating"+userList);

        // remove existing rows of the table
        for(int i=totalRows; i > 0; i--){
            currentUserModel.removeRow(i-1);
        }
        if(userList.size() > 0){
            for(String s: userList)
                currentUserModel.addRow(new Object[] {s});
        }
    }

    /**
     * Checks the rep invariant. The list of boards and the list of users
     * have to be the same length as what is stored in the respective tables.
     */
    public void checkRep(){
        assert(currentBoardTable.getRowCount() == boards.size());
        assert(currentUserTable.getRowCount() == users.size());
    }


}