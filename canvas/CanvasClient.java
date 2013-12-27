package canvas;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;

import controller.ClientMsg;
import controller.ClientMsgType;
import controller.ParseMessage;
import controller.ServerMsg;

import java.util.concurrent.LinkedBlockingQueue;

/*
 * Class that implements methods to facilitate communication with the
 * white board server. It has routines that accept requests from the GUI and sends 
 * those to the server
 * 
 * Also, it implements methods that receive packets from the server, parse and inform the 
 * GUI.
 * 
 * This class is thread safe as all network activity is done in the worker thread context
 * and does not block the event dispatch thread 
 */
public class CanvasClient {
    public final String serverIP;
    private final int serverPort;

    public WhiteboardFrame wbFrame;
    private Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    // A blocking queue is implemented where the GUI adds messages to be
    // sent to the server while a worker thread picks the messages and sends them
    // to the server - this way the event dispatch thread is not blocked

    private LinkedBlockingQueue<String> msgQ;

    public CanvasClient(WhiteboardFrame wbFrame, String IP, int port){
        serverIP = IP;
        serverPort = port;
        this.wbFrame = wbFrame;
        msgQ = new LinkedBlockingQueue<String>();


        try {
            socket = new Socket(serverIP, serverPort);
            try{
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Now create a swing worker thread to transmit requests to the server
                // You can't block main event dispatch thread for socket transmit as that 
                // may take time and freeze the UI
                SendWorkerThread sendWorkerThread = new SendWorkerThread(out, msgQ);
                sendWorkerThread.execute();

                // Now create a swing worker thread to read from the socket
                // You can't block main event dispatch thread for socket read as that 
                // would block - so need a  worker thread
                ReceiveWorkerThread receiveWorkerThread = new ReceiveWorkerThread(in);
                receiveWorkerThread.execute();
                // returns right away to the caller
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        catch(Exception e){
            //If the connection is not made, have the user enter another
            //IP address + port combination
            wbFrame.invalidIP();
        }
    }

    /**
     * This routine is used to send the request string to the 
     * server. This runs in the context of the event dispatch thread
     * and adds the string to a blocking queue.
     * A worker thread later picks the string from the queue and sends it out
     * 
     * @param req string that needs to be sent to the server
     */
    private void sendMessageToServer(String req) {
        msgQ.add(req);
    }

    /**
     * Method to send login request to the server
     * 
     * @param userName user name string
     */
    public void sendLoginRequest(String userName) {

        ClientMsg sendMsg = new ClientMsg(ClientMsgType.LOGIN);
        sendMsg.setUserName(userName);
        sendMessageToServer(sendMsg.toString());
    }

    /**
     * Send logout request to the server
     */
    public void sendLogoutRequest() {

        ClientMsg sendMsg = new ClientMsg(ClientMsgType.LOGOUT);
        sendMessageToServer(sendMsg.toString());
    }

    /**
     * Send a request for creating a new board
     * @param boardName board name string
     */
    public void sendNewBoardRequest(String boardName) {
        ClientMsg sendMsg = new ClientMsg(ClientMsgType.NEW_BOARD);
        sendMsg.setBoardName(boardName);
        sendMessageToServer(sendMsg.toString());
    }

    /**
     * Send a request for opening an existing board
     * @param boardName board name string
     */
    public void sendOpenBoardRequest(String boardName) {
        ClientMsg sendMsg = new ClientMsg(ClientMsgType.OPEN_BOARD);
        sendMsg.setBoardName(boardName);
        sendMessageToServer(sendMsg.toString());
    }

    /**
     * Send a request for closing the board that the user is 
     * currently working on
     */
    public void sendCloseBoardRequest() {
        ClientMsg sendMsg = new ClientMsg(ClientMsgType.CLOSE_BOARD);
        sendMessageToServer(sendMsg.toString());
    }

    /**
     * 
     * @param color color name string like black, blue etc
     * @param lineSize integer line/stroke size
     * @param coordList Array of strings with each string
     *      representing a (x,y) pair 
     *  	e.g { "2:4", "40:50", "111:333" }
     */
    public void sendFreeDrawRequest(String color, int lineSize, ArrayList<String> coordList) {
        ClientMsg sendMsg = new ClientMsg(ClientMsgType.FREE_DRAW);
        sendMsg.setColor(color);
        sendMsg.setLineSize(lineSize);
        sendMsg.setCoordinateList(coordList);
        sendMessageToServer(sendMsg.toString());
    }


    /**
     * 
     * This is a swing worker thread to send a message over
     * the socket to the server
     * doInBackground() will retrieve the message from the queue and 
     * send the line over the out connection
     * 
     * This is thread safe as the Blocking Queue is safe. This is also
     * a separate thread (and not EDT). Out connection is not used by any other thread
     */
    public class SendWorkerThread extends SwingWorker<String, Void> {
        private PrintWriter out;
        LinkedBlockingQueue<String> queue;

        public SendWorkerThread(PrintWriter out, LinkedBlockingQueue<String> msgQ ) {
            this.out = out;
            this.queue = msgQ;
        }

        @Override
        protected String doInBackground() throws Exception
        {
            while (true) {
                try {

                    // remove one entry at a time and transmit to the server
                    String line = queue.take();
                    this.out.println(line);
                }
                catch (Exception e) {
                    break;
                    // user will be notified properly through a pop-up box (rather than stack trace)
                    // (see done method below)
                }
            }
            return ("Error sending to server - closing the application");

        }
        @Override
        public void done(){
            try {
                String error;
                error = get();
                // Displays a nice pop-up box to the user, indicating that messages can't be sent to server 
                wbFrame.connectionError(error);

            } catch (Exception e) {

            }
        }
    }

    /**
     * 
     * This is a swing worker thread to wait on the socket and receive
     * incoming packets from the server
     * doInBackground() will read lines from the socket and schedule
     * a runnable object to execute on the swing event dispatch thread
     * for thread safety
     */
    private class ReceiveWorkerThread extends SwingWorker<String, Void> {
        private BufferedReader in;


        public ReceiveWorkerThread(BufferedReader in) {
            this.in = in;
        }

        @Override
        protected String doInBackground() throws Exception
        {
            try {
                // read from the socket
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    // schedule this line to be parsed and processed in event dispatch thread
                    SwingUtilities.invokeLater(new ProcessReceivedData(line));
                }
            }
            catch (Exception e) {
                // user will be notified properly through a pop-up box (rather than stack trace)
                // (see done method below)
            }
            return ("Error receiving from server - closing the application");

        }
        @Override
        public void done(){
            try {
                String error;
                error = get();
                // Displays a nice pop-up box to the user, indicating that messages can't be sent to server
                wbFrame.connectionError(error);

            } catch (Exception e) {

            } 
        }
    }

    /**
     * Procedure that runs in the context of the event dispatch thread
     * to process the line received from the server
     * This method is called for every line received one at a time
     * It parses the line received and constructs a serverMsg object
     * which can be queried later
     */
    private class ProcessReceivedData implements Runnable {
        private String response;

        private ProcessReceivedData(String response) {
            this.response = response;

        }
        public void run() {
            ServerMsg respMsg = null;
            try {
                // parse the incoming message
                respMsg = ParseMessage.parseServerMsg(response);
            } catch (Exception e) {
                respMsg = null;
            }

            if (respMsg != null) {

                switch(respMsg.getType()) {
                case BOARD_LIST:
                    HashMap<String, ArrayList<String>> map;  
                    map = respMsg.getBoardCollaboratorsList();
                    wbFrame.updateBoardAndUserTables(map);
                    break;

                case FREE_DRAW:
                    ColorType currentColor = wbFrame.getCanvas().color;
                    int currentSize = wbFrame.getCanvas().currentPenSize;
                    String drawColor = respMsg.getColor();
                    ColorType drawColorInt = ColorType.BLACK;
                    int drawPenSize = respMsg.getLineSize();
                    if(drawColor.equals("red")){
                        drawColorInt = ColorType.RED;
                    }
                    else if(drawColor.equals("blue")){
                        drawColorInt = ColorType.BLUE;
                    }
                    else if(drawColor.equals("yellow")){
                        drawColorInt = ColorType.YELLOW;
                    }
                    else if(drawColor.equals("green")){
                        drawColorInt = ColorType.GREEN;
                    }
                    else if(drawColor.equals("white")){
                        drawColorInt = ColorType.ERASE; 
                    }
                    ArrayList<String> pointList = respMsg.getCoordinateList();
                    wbFrame.getCanvas().setColor(drawColorInt);
                    wbFrame.getCanvas().setPenSize(drawPenSize);
                    for(int i = 0; i < pointList.size()-1; i++){

                        //iterating through pointList to capture the coordinates
                        String coords1[] = pointList.get(i).split(":");
                        String coords2[] = pointList.get(i+1).split(":");
                        int xCoord1 = Integer.parseInt(coords1[0]);
                        int yCoord1 = Integer.parseInt(coords1[1]);
                        int xCoord2 = Integer.parseInt(coords2[0]);
                        int yCoord2 = Integer.parseInt(coords2[1]);
                        wbFrame.getCanvas().drawLineSegment(xCoord1, yCoord1, xCoord2, yCoord2,false);
                    }
                    // After drawing is complete, restore original color and pen size
                    wbFrame.getCanvas().setColor(currentColor);
                    wbFrame.getCanvas().setPenSize(currentSize);

                    break;

                case DUPLICATE_NAME:
                    wbFrame.duplicateUsername();
                    break;
                default:
                }
            }
        }
    }
}