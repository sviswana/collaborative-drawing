package whiteboard.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import controller.ServerMsg;
import controller.ServerMsgType;
import controller.ParseMessage;
import controller.ClientMsg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashSet;
import java.util.Set;

/*
 * This class implements the whiteboard server
 * It facilitates multiple users working on the same whiteboard
 * Maintains multiple boards as well
 * Implements methods to send and receive data from multiple
 * clients and process them
 * it also stores all the mouse draw actions of every user on the whiteboard
 * and persists them across whiteboard closes
 * 
 */
public class WhiteboardServer {
    // tracks each board and list of users on that board
    public HashMap<String, ArrayList<String>> currentBoardMap; 
    // Rep invariant
    // List of all active boards and users on each of those boards
    // Should contain only users who are currently working on that board
    // No user should be present more than once in any userlist

    public ArrayList<Whiteboard> currentWhiteboards; 
    // Rep invariant 
    // List that maintains list of all whiteboards
    // Name of each board must be unique
    // Also users on each of those boards must be currently logged in
    // and unique

    private final ServerSocket serverSocket;

    // A single queue that contains requests from all the clients
    private LinkedBlockingQueue<ArrayList<Object>> msgQ;
    // List of all connections

    private ArrayList<ConnectionInfo> connectionList;
    // rep invariant
    // connectionList maintains list of all connectionInfo objects
    // Users who are logged in should be all unique

    // Object used for ensuring synchronization
    private Object serverLock = new Object();

    /**
     * Creates a WhiteboardServer that listens for connections on port
     * @param port port number, requires 0 <= port <= 65535
     * @throws IOException
     */
    public WhiteboardServer(int port) throws IOException{
        serverSocket = new ServerSocket(port);
        this.currentWhiteboards = new ArrayList<Whiteboard>();
        this.currentBoardMap = new HashMap<String, ArrayList<String>>();
        this.msgQ = new LinkedBlockingQueue<ArrayList<Object>>();
        connectionList = new ArrayList<ConnectionInfo>();
    }

    /**
     * Run the server, listening for client connections and handling them.
     * Never returns unless an exception is thrown.
     * it also creates a processing thread that takes requests 
     * from a queue and processes them
     * 
     * @throws IOException if the main server socket is broken
     *                     (IOExceptions from individual clients do *not* terminate serve())
     */
    public void serve() throws IOException {

        // this thread will take request off a single queue and process them
        Thread processingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processRequest();
                } 
                catch (Exception e)  { }
            }
        });

        processingThread.start();

        // Wait for client connect requests and spawn a thread
        // to service those requests
        while (true) {
            // block until a client connects
            final Socket socket = serverSocket.accept();

            // spawn a thread to handle the client
            Thread thread= new Thread(new Runnable(){
                public void run(){
                    try {
                        handleConnection(socket);
                    } catch (IOException e) {
                        e.printStackTrace(); // but don't terminate serve()
                    } 
                }
            });
            thread.start();
        }
    }

    /**
     * This method receives messages from the client and puts them 
     * on a single queue. processRequest() will take each of those
     * requests and process them
     * 
     * @param socket connection to the client
     * @throws IOException
     */
    private void handleConnection(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ClientMsg recvdMsg = null;

        // connInfo object contains connection status and other info 
        // like socket out stream, user name etc for this particular connection
        ConnectionInfo connInfo = new ConnectionInfo();
        connInfo.setPrintWriter(out);

        synchronized(serverLock) {
            connectionList.add(connInfo);
        }

        try {
            // now wait to read the request from the client
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                // Since we need to pass both connection info and the received message
                // to processRequest() and since only one object can be added to the queue
                // at a time, we create an ArrayList object and add both to that list
                ArrayList<Object> reqInfo;

                // Now parse the received request and in case of error
                // send error message back and continue to wait for next request
                try {
                    recvdMsg = ParseMessage.parseClientMsg(line);
                }
                catch (RuntimeException e) { // catches parser exceptions
                    StringBuilder sb = new StringBuilder("");
                    sb.append("error,");
                    sb.append(e.getMessage());
                    synchronized(serverLock) {
                        // send the parser error message to the client
                        connInfo.getPrintWriter().println(sb.toString());
                    }
                    continue;
                }
                reqInfo = new ArrayList<Object>();

                reqInfo.add(connInfo);
                reqInfo.add(recvdMsg);

                // add it to the message queue to get processed
                // Once this object is put on the queue, this thread does not access them
                // anymore except in case of exception (see catch block below)
                msgQ.add(reqInfo);

            }
        }
        catch (IOException e) { 
            // Since the connInfo and recvdMsg objects are being accessed by the
            // processRequest thread, we need to synchronize access for thread safety
            synchronized (serverLock) {
                // on connection failure, need to clean up
                connInfo.setLoggedIn(false);
                // If user was connected to any board, disconnect that
                detachUserFromBoard(connInfo.getWhiteboard(), connInfo.getUserName(), connInfo);
                connectionList.remove(connInfo);
            }
        }
    }

    /**
     * This method is run from a single thread dedicated to processing 
     * all the requests from the clients. Since only single thread operates 
     * on all the requests, thread safety is easier to accomplish
     * Only during error handling of socket connections in handleConnection(),
     * we need to synchronize access as cleanup procedure needs to be run. 
     * To facilitate that, we use a single object lock on which all accesses are
     * synchronized
     * @throws InterruptedException
     */
    public void processRequest() throws InterruptedException {
        ArrayList<Object> req;
        ConnectionInfo connInfo;
        ClientMsg msg;

        while (true) {
            // The req will contain 2 objects, the connection info and the parsed message object
            req = msgQ.take();

            try {
                // acquire the single global lock to protect the integrity of the white board server
                synchronized (serverLock) {

                    connInfo = (ConnectionInfo)req.get(0);
                    msg = (ClientMsg)req.get(1);

                    switch (msg.getType()) {
                    case LOGIN:
                        processLogin(connInfo, msg);
                        break;
                    case NEW_BOARD:
                        processNewBoard(connInfo, msg);
                        break;
                    case OPEN_BOARD:
                        processOpenBoard(connInfo, msg);
                        break;
                    case CLOSE_BOARD:
                        processCloseBoard(connInfo, msg);
                        break;
                    case FREE_DRAW:
                        processFreeDraw(connInfo, msg);
                        break;
                    case LOGOUT:
                        processLogout(connInfo, msg);
                        break;
                    default :
                        // ignore unknown message type

                    }

                }
                // Make sure that the server's rep invariants are preserved
                checkRep();
            }
            catch (Exception e) {
                // There may be exception as the above methods send responses
                // to the client. We just ignore and move to process the next message
                // Socket read on the same socket will fail and that will take 
                // care of cleaning up the connections and associated resources
                // with the white board
            }
        }
    }

    /**
     * Process login request from the client
     * 
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processLogin (ConnectionInfo connInfo, ClientMsg msg) {
        // if user is already logged in, ignore this request
        if (connInfo.isLoggedIn()) return;

        String newUserName = msg.getUserName();
        // if the name being used is already logged in, fail the request
        for (ConnectionInfo cInfo : connectionList) {
            // need to avoid looking at the current connection
            // while checking other connections, only look for users who are logged in
            if (cInfo != connInfo && cInfo.isLoggedIn() &&
                    cInfo.getUserName().equals(newUserName)) {
                ServerMsg resp = new ServerMsg(ServerMsgType.DUPLICATE_NAME);
                resp.setUserName(newUserName);
                connInfo.getPrintWriter().println(resp);

                return;
            }
        }
        // can login successfully now
        connInfo.setLoggedIn(true);
        connInfo.setUserName(newUserName);

        ServerMsg resp = new ServerMsg(ServerMsgType.BOARD_LIST);
        // send the list of all boards to the client
        resp.setBoardCollaboratorsList(currentBoardMap);
        connInfo.getPrintWriter().println(resp);
    }

    /**
     * Process new board request
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processNewBoard (ConnectionInfo connInfo, ClientMsg msg) {

        if (!connInfo.isLoggedIn()) {
            // Send error message to the client
            sendNotLoggedInErrorMessage(connInfo);
            return;
        }
        String boardName = msg.getBoardName();
        // If, white board is already created, fail the request
        for (Whiteboard wb: currentWhiteboards) {
            if (wb.getBoardName().equals(boardName)) {
                ServerMsg resp = new ServerMsg(ServerMsgType.BOARD_EXISTS);
                resp.setBoardName(msg.getBoardName());
                connInfo.getPrintWriter().println(resp);
                return;
            }
        }
        // Now create the board
        Whiteboard newBoard = new Whiteboard(boardName);
        currentWhiteboards.add(newBoard); //update board list
        ArrayList<String> userList = new ArrayList<String>(); // empty user list
        currentBoardMap.put(boardName, userList); //update board map
        // set board name in connection info for this client
        connInfo.setWhiteboard(newBoard);

    }

    /**
     * Process open board request
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processOpenBoard (ConnectionInfo connInfo, ClientMsg msg) {

        boolean isNewBoard = true;
        if (!connInfo.isLoggedIn()) {
            // Send error message to the client
            sendNotLoggedInErrorMessage(connInfo);
            return;
        }
        String boardName = msg.getBoardName();
        String userName = connInfo.getUserName();

        for (Whiteboard board : currentWhiteboards){
            if (board.getBoardName().equals(boardName)){
                isNewBoard = false;
            }
        }
        if (isNewBoard) {
            processNewBoard(connInfo,msg);
        }


        // Go through the list of all the boards and add the user to an 
        // existing board. If board does not exist, we ignore the request
        for (Whiteboard board : currentWhiteboards){
            if (board.getBoardName().equals(boardName)){
                connInfo.setWhiteboard(board);
                // if board is already opened by the user, don't add the user again
                if (!board.getUsernames().contains(userName)) {
                    board.addUser(userName, connInfo.getPrintWriter()); //update board list 
                    currentBoardMap.get(boardName).add(userName); //update board map
                }
                ServerMsg resp = new ServerMsg(ServerMsgType.BOARD_LIST);
                resp.setBoardCollaboratorsList(currentBoardMap);
                // Broadcast the list of boards and users on it to all
                for (ConnectionInfo cInfo: connectionList) {
                    cInfo.getPrintWriter().println(resp);
                }
                // send user's current board's sketch list
                sendSketches(connInfo);
                return;
            }
        }
        // if it gets here, the board does not exist and user is trying to open it
        ServerMsg resp = new ServerMsg(ServerMsgType.BOARD_NOT_EXIST);
        resp.setBoardName(msg.getBoardName());
        connInfo.getPrintWriter().println(resp);

    }

    /**
     * Process close board request
     * 
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processCloseBoard (ConnectionInfo connInfo, ClientMsg msg) {
        if (!connInfo.isLoggedIn()) {
            // Send error message to the client
            sendNotLoggedInErrorMessage(connInfo);
            return;
        }
        Whiteboard currentBoard = connInfo.getWhiteboard();
        String userName = connInfo.getUserName();

        detachUserFromBoard (currentBoard, userName, connInfo);

    }

    /**
     * Process free draw requests
     * 
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processFreeDraw (ConnectionInfo connInfo, ClientMsg msg) {
        // If user not logged in or not opened a whiteboard, ignore request
        if (!connInfo.isLoggedIn() || connInfo.getWhiteboard() == null) 
            return; 

        if (!connInfo.isLoggedIn()) {
            // Send error message to the client
            sendNotLoggedInErrorMessage(connInfo);
            return;
        }
        if (connInfo.getWhiteboard() == null) { 
            return; //ignore
        }
        Whiteboard currentBoard = connInfo.getWhiteboard();

        // Update the board with the most recent update from the client
        currentBoard.addSketch(msg.toString());
        // we can return the message as-is back to all the clients 
        // connected to this board so they can update their screens
        for (String username: currentBoard.currentUsernames){
            PrintWriter printWriter = currentBoard.getPrintWriterMap().get(username);
            printWriter.println(msg.toString());
        }
    }

    /**
     * Process logout request
     * 
     * @param connInfo connection object describing client connection
     * @param msg object containing parsed request from client
     */
    public void processLogout (ConnectionInfo connInfo, ClientMsg msg) {

        if (!connInfo.isLoggedIn()) return; // if not logged in, ignore request

        Whiteboard currentBoard = connInfo.getWhiteboard();
        String userName = connInfo.getUserName();
        connInfo.setLoggedIn(false);
        detachUserFromBoard (currentBoard, userName, connInfo );

        connInfo.setUserName(null);

    }

    private void sendNotLoggedInErrorMessage(ConnectionInfo connInfo) {
        // Send error message to the client
        ServerMsg resp = new ServerMsg(ServerMsgType.NOT_LOGGED_IN);
        connInfo.getPrintWriter().println(resp);

    }
    /**
     * Builds a string with all the existing sketches so the new user can
     * recreate the board to its most recent state
     *  
     * @param connInfo connection object describing client connection
     */
    private void sendSketches(ConnectionInfo connInfo) {
        StringBuilder sb = new StringBuilder("");
        // each stroke from client is maintained as a sketch (which
        // itself is a string
        int numSketches = connInfo.getWhiteboard().getSketches().size();

        // return if board is empty
        if (numSketches == 0) return;

        for (String sketch: connInfo.getWhiteboard().getSketches()){
            sb.append(sketch);
            if (--numSketches > 0) // the last new line is sent as part of socket write
                sb.append("\n");
        }

        connInfo.getPrintWriter().println(sb.toString());

    }

    /**
     * Detach the user from the board
     * 
     * @param currentBoard board from which the user needs to be disconnected
     * @param userName user name
     * @param connInfo connection object describing client connection
     */
    public void detachUserFromBoard(Whiteboard currentBoard, String userName, ConnectionInfo connInfo) {

        if (currentBoard != null) {
            if (userName != null) {
                currentBoard.removeUser(userName);
                currentBoardMap.get(currentBoard.getBoardName()).remove(userName);
            }
            // broadcast the changes in board user list to others
            // connected to this board including this user if he/she 
            // still connected. This method can also be called from
            // handleConnection in case of socket error
            ServerMsg resp = new ServerMsg(ServerMsgType.BOARD_LIST);
            resp.setBoardCollaboratorsList(currentBoardMap);

            if (connInfo.isLoggedIn())
                connInfo.getPrintWriter().println(resp);
            for (String uName: currentBoard.currentUsernames){
                PrintWriter printWriter = currentBoard.getPrintWriterMap().get(uName);
                printWriter.println(resp);
            }
            connInfo.setWhiteboard(null);
        }
    }

    /**
     * Main method that starts the white board server
     * @param args
     */
    public static void main(String[] args) {
        int port = 4444; // default port
        try {
            runWhiteboardServer(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start WhiteboardServer running on the specified port
     * @param port The network port on which the server should listen.
     */
    public static void runWhiteboardServer(int port) throws IOException {
        final WhiteboardServer server = new WhiteboardServer(port);
        server.serve();
    }

    /*
     *  Private utility class that is used to contain status
     *  about each client connection
     *  It tracks whether user is logged in or not, user name
     *  white board they are working on
     */
    private class ConnectionInfo {
        private PrintWriter out;
        private Whiteboard board;
        private boolean loggedIn;
        private String username;

        /**
         * Constructor
         */
        public ConnectionInfo() {
            this.out = null;
            this.board = null;
            this.loggedIn = false;
            this.username = null;
        }

        /**
         * Set method for setting Connection to socket output stream
         * @param out Connection to socket output stream
         */
        public void setPrintWriter (PrintWriter out) {
            this.out = out;
        }

        /**
         * Get method for getting Connection to socket output stream
         * @return Connection to socket output stream
         */
        public PrintWriter getPrintWriter() {
            return this.out;
        }

        /**
         * Set the whiteboard that this user is currently using
         * @param board
         */
        public void setWhiteboard (Whiteboard board) {
            this.board = board;
        }

        /**
         * Get current whiteboard that this user is connected to
         * @return whiteboard
         */
        public Whiteboard getWhiteboard() {
            return this.board;
        }

        /**
         * Set the user name
         * @param username string
         */
        public void setUserName (String username) {
            this.username = username;
        }

        /**
         * Get the user name
         * @return user name string
         */
        public String getUserName() {
            return this.username;
        }

        /**
         * Check whether user is logged in
         * @return
         */
        public boolean isLoggedIn () {
            return loggedIn;
        }

        /**
         * Set the status of user logged in or not
         * @param loggedIn boolean
         */
        public void setLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }
    }

    /**
     * Adds a new Whiteboard object to server
     * @param newWhiteboard new whiteboard being added to server
     */
    public void addWhiteboard(Whiteboard newWhiteboard){
        this.currentWhiteboards.add(newWhiteboard);
    }

    /**
     * @return List of current whiteboards
     */
    public ArrayList<Whiteboard> getWhiteboards(){
        return this.currentWhiteboards;
    }

    /**
     * Utility to check rep invariants. 
     * Should only be called when the system is in consistent state
     * i.e at the end of processing of a request and not in the middle of processing
     * 
     * Here, we check the following
     * 1. All logged in users are unique
     * 2. All whiteboards on the server have only user names that
     *    are currently logged in and also unique
     * 3. Check each board object is unique as well as the board name it has
     * 4. Verify each of the white boards that the connection object refers to
     *    is present in the list of boards maintained by the server
     * 5. Verify each of the boards and userlists on the currentBoardMap are valid
     */
    public void checkRep() {
        synchronized (serverLock) {
            // by using sets, we are making sure there are only unique users/boards
            // on the server
            Set<String> loggedUsers = new HashSet<String>();
            Set<String> boardNames = new HashSet<String>();
            Set<Whiteboard> whiteboards = new HashSet<Whiteboard>();

            // Go through the list of all users who are logged in
            // and ensure those are all unique
            for (ConnectionInfo cInfo : connectionList) {
                if (cInfo.isLoggedIn()) {
                    assert(loggedUsers.add(cInfo.getUserName()));
                }
            }

            // check users on each of those boards are actually currently logged in
            for (Whiteboard wb : currentWhiteboards) {
                ArrayList<String> userlist = wb.getUsernames();
                for (String name : userlist) {
                    assert(loggedUsers.contains(name));
                }
            }

            // check each board name as well as board objects are unique
            for (Whiteboard wb : currentWhiteboards) {
                assert(whiteboards.add(wb));
                assert(boardNames.add(wb.getBoardName()));
            }

            // Now walk through connectionlist and ensure that each of the 
            // whiteboards that the user is referring to are present in the 
            // current whiteboards
            for (ConnectionInfo cInfo : connectionList) {
                if (cInfo.isLoggedIn() && cInfo.getWhiteboard() != null) {
                    assert(whiteboards.contains(cInfo.getWhiteboard()));
                }
            }

            // Verify currentBoardMap which is used to send each client BOARD_LIST message
            Set<String> allUsers = new HashSet<String>();
            for (Map.Entry<String, ArrayList<String>> entry : currentBoardMap.entrySet()) {
                String bName = entry.getKey(); // board name is key
                assert(boardNames.contains(bName));
                ArrayList<String> ulist = entry.getValue(); // user list is the value
                for (String s : ulist) {
                    // we will add this user to a set and make sure that no user is present in 
                    // more than one board as they can work on only one board at a time
                    assert(allUsers.add(s));
                    // Make sure each user is currently logged in
                    assert(loggedUsers.contains(s));
                }
            }
        }
    }
}
