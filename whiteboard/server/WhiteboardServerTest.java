package whiteboard.server;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import java.util.HashMap;

import org.junit.Test;


import controller.ClientMsg;
import controller.ClientMsgType;
import controller.ParseMessage;
import controller.ServerMsg;
import controller.ServerMsgType;


/** 
 * @category no_didit
 * 
 * Testing Strategy:
 * For each of the following cases, check that the server responds correctly to the clients' requests
 * LoginLogoutTest- verify that a single client can login and logout of server
 * NotLoggedInTest- verify that client can't create board without logging in
 * DuplicateUsernameTest- verify that two clients can't be logged in with the same username
 * CreateOpenCloseBoardTest- verify that a single client can create, open, and close board
 * FreedrawTest- verify that a single client can draw on board
 * LogoutTest- verify that a user is removed from current board after logging out
 * TwoClientsComprehensiveTest
 *          - verify that multiple clients with different usernames can both login to server
 *          - verify that multiple boards can be created on server
 *          - verify that clients can switch boards
 *          - verify that when a client draws on board, server only sends new sketch to clients on that board
 */
public class WhiteboardServerTest {

    /* LogIn And LogOut
     * Run server on port 4443
     * Create a client
     * Client logs in with username "user1" and server responds with a BOARD_LIST message
     * Client logs out. Server removes client from board1.
     */
    @Test
    public void LoginLogoutTest() throws InterruptedException, IOException {
        startServer(4443);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("localhost",4443);
            socket.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // client sends LOGIN message
            ClientMsg loginMsg = new ClientMsg(ClientMsgType.LOGIN);
            out.println(loginMsg);

            // server should respond with BOARD_LIST message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg1.getType());
            assertEquals(null, recvdMsg1.getBoardCollaboratorsList()); // boardlist should be empty

            // Client sends logout message
            ClientMsg logOutMsg = new ClientMsg(ClientMsgType.LOGOUT);
            out.println(logOutMsg);

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* Creating Board Before Logging In
     * Run server on port 4445
     * Create a client
     * Client sends CREATE_BOARD message without logging in.
     * Server responds with NOT_LOGGED_IN message.
     */
    @Test
    public void NotLoggedInTest() throws InterruptedException, IOException {
        startServer(4445);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("localhost",4445);
            socket.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // client sends NEW_BOARD message
            ClientMsg newBoardMsg = new ClientMsg(ClientMsgType.NEW_BOARD);
            newBoardMsg.setBoardName("board1");
            out.println(newBoardMsg);

            // Since client is not logged in, server responds with NOT_LOGGED_IN message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.NOT_LOGGED_IN, recvdMsg1.getType());

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* Duplicate UserNames
     * Run server on port 4446
     * Connect two clients to server.
     * Both clients try to login with the same username.
     * The second client should recieve a DUPLICATE_NAME message from ther server.
     */
    @Test
    public void DuplicateUsernameTest() throws InterruptedException, IOException {
        startServer(4446);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket1;
        Socket socket2;
        try {
            // connect client1 to server
            socket1 = new Socket("localhost",4446);
            socket1.setSoTimeout(3000);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
            PrintWriter out1 = new PrintWriter(socket1.getOutputStream(),true);
            // connect client2 to server
            socket2 = new Socket("localhost",4446);
            socket2.setSoTimeout(3000);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
            PrintWriter out2 = new PrintWriter(socket2.getOutputStream(),true);

            // client1 sends LOGIN message
            ClientMsg client1LoginMsg = new ClientMsg(ClientMsgType.LOGIN);
            client1LoginMsg.setUserName("user1");
            out1.println(client1LoginMsg);
            // server should respond with BOARD_LIST message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg1.getType());
            assertEquals(null, recvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client2 sends LOGIN message with same username
            ClientMsg client2LoginMsg = new ClientMsg(ClientMsgType.LOGIN);
            client2LoginMsg.setUserName("user1");
            out2.println(client2LoginMsg);
            // server should respond with DUPLICATE_NAME message
            ServerMsg recvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.DUPLICATE_NAME, recvdMsg2.getType());

            socket1.close();
            socket2.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* Creating, Opening, and Closing board
     * Run server on port 4447
     * Create a client
     * Client logs in with username "user1" and server responds with a BOARD_LIST message
     * Client creates and opens a new board "board1" and server responds with BOARD_LIST
     * Client closes board1 and server responds by sending client a BOARD_LIST message
     * Client logs out. Server removes client from board1.
     */
    @Test
    public void CreateOpenCloseBoardTest() throws InterruptedException, IOException {
        startServer(4447);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("localhost",4447);
            socket.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // client sends LOGIN message
            ClientMsg loginMsg = new ClientMsg(ClientMsgType.LOGIN);
            loginMsg.setUserName("user1");
            out.println(loginMsg);

            // server should respond with BOARD_LIST message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg1.getType());
            assertEquals(null, recvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client sends NEW_BOARD message
            ClientMsg newBoardMsg = new ClientMsg(ClientMsgType.NEW_BOARD);
            newBoardMsg.setBoardName("board1");
            out.println(newBoardMsg);

            // client sends OPEN_BOARD message
            ClientMsg openBoardMsg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            openBoardMsg.setBoardName("board1");
            out.println(openBoardMsg);

            // server should respond with BOARD_LIST message 
            HashMap<String, ArrayList<String>> boardlist = new HashMap<String, ArrayList<String>>();
            ArrayList<String> userlist1 = new ArrayList<String>();
            userlist1.add("user1");
            boardlist.put("board1", userlist1); 
            ServerMsg recvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg2.getType());
            assertEquals(boardlist, recvdMsg2.getBoardCollaboratorsList()); //boardlist should contain [board1=use1]

            // client closes board1
            ClientMsg closeBoardMsg = new ClientMsg(ClientMsgType.CLOSE_BOARD);
            out.println(closeBoardMsg);

            // server responds by sending an updated BOARD_LIST message to all users on that board
            userlist1.remove("user1");
            boardlist.put("board1", userlist1); // remove user1 from board1
            ServerMsg recvdMsg3 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg3.getType());
            assertEquals(boardlist, recvdMsg3.getBoardCollaboratorsList()); //boardlist should contain [board1=]

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* FREEDRAW
     * Run server on port 4448
     * Create a client
     * Client logs in with username "user1" and server responds with a BOARD_LIST message
     * Client creates and opens a new board "board1" and server responds with BOARD_LIST
     * Client sends FREEDRAW message and server propagates sketch back to client.
     */
    @Test
    public void FreedrawTest() throws InterruptedException, IOException {
        startServer(4448);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("localhost",4448);
            socket.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // client sends LOGIN message
            ClientMsg loginMsg = new ClientMsg(ClientMsgType.LOGIN);
            loginMsg.setUserName("user1");
            out.println(loginMsg);

            // server should respond with BOARD_LIST message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg1.getType());
            assertEquals(null, recvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client sends NEW_BOARD message
            ClientMsg newBoardMsg = new ClientMsg(ClientMsgType.NEW_BOARD);
            newBoardMsg.setBoardName("board1");
            out.println(newBoardMsg);

            // client sends OPEN_BOARD message
            ClientMsg openBoardMsg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            openBoardMsg.setBoardName("board1");
            out.println(openBoardMsg);

            // server should respond with BOARD_LIST message 
            HashMap<String, ArrayList<String>> boardlist = new HashMap<String, ArrayList<String>>();
            ArrayList<String> userlist1 = new ArrayList<String>();
            userlist1.add("user1");
            boardlist.put("board1", userlist1); 
            ServerMsg recvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg2.getType());
            assertEquals(boardlist, recvdMsg2.getBoardCollaboratorsList()); //boardlist should contain [board1=[use1]]

            // client sends FREE_DRAW message
            ClientMsg freeDrawMsg = new ClientMsg(ClientMsgType.FREE_DRAW);
            freeDrawMsg.setColor("black");
            freeDrawMsg.setLineSize(10);
            ArrayList<String> coordList= new ArrayList<String>();
            coordList.add("1:1");
            coordList.add("1:2");
            coordList.add("1:3");
            coordList.add("2:3");
            freeDrawMsg.setCoordinateList(coordList);
            out.println(freeDrawMsg);

            // server should respond by propagating the sketch back to user
            ServerMsg recvdMsg3 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.FREE_DRAW, recvdMsg3.getType());

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* Client logs out without closing board. Client should be removed from current board.
     * Run server on port 4449
     * Create a client
     * Client logs in with username "user1" and server responds with correct BOARD_LIST message
     * Client creates and opens a new board "board1" and server responds with correct BOARD_LIST.
     * Client logs out. Server removes client from board1.
     * Client logs in again with username "user1" and server should responds with correct BOARD_LIST message.
     */
    @Test
    public void LogoutTest() throws InterruptedException, IOException {
        startServer(4449);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket;
        try {
            socket = new Socket("localhost",4449);
            socket.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // client sends LOGIN message
            ClientMsg loginMsg = new ClientMsg(ClientMsgType.LOGIN);
            loginMsg.setUserName("user1");
            out.println(loginMsg);

            // server should respond with BOARD_LIST message
            ServerMsg recvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg1.getType());
            assertEquals(null, recvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client sends NEW_BOARD message
            ClientMsg newBoardMsg = new ClientMsg(ClientMsgType.NEW_BOARD);
            newBoardMsg.setBoardName("board1");
            out.println(newBoardMsg);

            // client sends OPEN_BOARD message
            ClientMsg openBoardMsg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            openBoardMsg.setBoardName("board1");
            out.println(openBoardMsg);

            // server should respond with BOARD_LIST message 
            HashMap<String, ArrayList<String>> boardlist = new HashMap<String, ArrayList<String>>();
            ArrayList<String> userlist1 = new ArrayList<String>();
            userlist1.add("user1");
            boardlist.put("board1", userlist1); 
            ServerMsg recvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg2.getType());
            assertEquals(boardlist, recvdMsg2.getBoardCollaboratorsList()); //boardlist should contain [board1=use1]

            // Client sends logout message
            ClientMsg logOutMsg = new ClientMsg(ClientMsgType.LOGOUT);
            out.println(logOutMsg);

            // client sends LOGIN message
            ClientMsg loginMsg2 = new ClientMsg(ClientMsgType.LOGIN);
            out.println(loginMsg2);

            // server should respond with BOARD_LIST message
            userlist1.remove("user1");
            boardlist.put("board1", userlist1);
            ServerMsg recvdMsg3 = ParseMessage.parseServerMsg(nextNonEmptyLine(in));
            assertEquals(ServerMsgType.BOARD_LIST, recvdMsg3.getType());
            assertEquals(boardlist, recvdMsg3.getBoardCollaboratorsList()); //boardlist should contain [board1=[]]

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /* Comprehensive Test With Two Clients
     * Run server on port 4450
     * Create two clients
     * Client1 logs in with username "user1", and server responds with a BOARD_LIST message
     * Client2 logs in with username "user2", and server responds with a BOARD_LIST message
     * Client1 creates and opens a new board "board1", and server sends BOARD_LIST to both clients
     * Client2 creates and opens a new board "board2", and server sends BOARD_LIST to both clients
     * Client1 draws on board1, and server propagates sketch to client1 only.
     * Client2 closes board2, and server sends BOARD_LIST to client2
     * Client2 opens used board board1 and server updates BOARD_LIST for both clients
     * Server also sends client2 current sketch on board1
     * Client2 draws on board1, and server propagates new sketch to both clients
     * Client1 closes board1, and server updates BOARD_LIST for both clients
     * Client2 closes board1, and server updates BOARD_LIST for client2 only
     * Client1 logs out
     * Client2 logs out
     */
    @Test
    public void TwoClientsComprehensiveTest() throws InterruptedException, IOException {
        startServer(4450);
        Thread.sleep(100); // Avoid race condition where we try to connect to server too early
        Socket socket1;
        Socket socket2;
        try {
            // connect client1 to server
            socket1 = new Socket("localhost",4450);
            socket1.setSoTimeout(3000);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
            PrintWriter out1 = new PrintWriter(socket1.getOutputStream(),true);
            // connect client2 to server
            socket2 = new Socket("localhost",4450);
            socket2.setSoTimeout(3000);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
            PrintWriter out2 = new PrintWriter(socket2.getOutputStream(),true);

            // client1 sends LOGIN message
            ClientMsg loginMsg1 = new ClientMsg(ClientMsgType.LOGIN);
            loginMsg1.setUserName("user1");
            out1.println(loginMsg1);

            // server should respond to client1 with BOARD_LIST message
            ServerMsg c1RecvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, c1RecvdMsg1.getType());
            assertEquals(null, c1RecvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client2 sends LOGIN message
            ClientMsg loginMsg2 = new ClientMsg(ClientMsgType.LOGIN);
            loginMsg2.setUserName("user2");
            out2.println(loginMsg2);

            // server should respond to client2 with BOARD_LIST message
            ServerMsg c2RecvdMsg1 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg1.getType());
            assertEquals(null, c2RecvdMsg1.getBoardCollaboratorsList()); //boardlist should be empty

            // client1 creates board1 by sending NEW_BOARD message
            ClientMsg c1NewBoard1Msg = new ClientMsg(ClientMsgType.NEW_BOARD);
            c1NewBoard1Msg.setBoardName("board1");
            out1.println(c1NewBoard1Msg);

            // client1 sends OPEN_BOARD message
            ClientMsg c1OpenBoard1Msg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            c1OpenBoard1Msg.setBoardName("board1");
            out1.println(c1OpenBoard1Msg);

            // server should updated BOARD_LIST to both clients
            HashMap<String, ArrayList<String>> boardlist = new HashMap<String, ArrayList<String>>();
            ArrayList<String> userlist1 = new ArrayList<String>();
            userlist1.add("user1");
            boardlist.put("board1", userlist1); 
            ServerMsg c1RecvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, c1RecvdMsg2.getType());
            assertEquals(boardlist, c1RecvdMsg2.getBoardCollaboratorsList()); //boardlist should contain [board1=[use1]]
            ServerMsg c2RecvdMsg2 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg2.getType());
            assertEquals(boardlist, c2RecvdMsg2.getBoardCollaboratorsList()); //boardlist should contain [board1=[use1]]

            // client2 creates board2 by sending NEW_BOARD message
            ClientMsg c2NewBoard2Msg = new ClientMsg(ClientMsgType.NEW_BOARD);
            c2NewBoard2Msg.setBoardName("board2");
            out2.println(c2NewBoard2Msg);

            // client2 sends OPEN_BOARD message
            ClientMsg c2OpenBoard2Msg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            c2OpenBoard2Msg.setBoardName("board2");
            out2.println(c2OpenBoard2Msg);

            // server should updated BOARD_LIST to both clients
            ArrayList<String> userlist2 = new ArrayList<String>();
            userlist2.add("user2");
            boardlist.put("board2", userlist2);
            ServerMsg c1RecvdMsg3 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, c1RecvdMsg3.getType());
            assertEquals(boardlist, c1RecvdMsg3.getBoardCollaboratorsList()); //boardlist should contain [board1=[user1], board2=[user2]]
            ServerMsg c2RecvdMsg3 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg3.getType());
            assertEquals(boardlist, c2RecvdMsg3.getBoardCollaboratorsList()); //boardlist should contain [board1=[user1], board2=[user2]]

            // client1 draws on board1 by sending FREE_DRAW message
            ClientMsg freeDrawMsg = new ClientMsg(ClientMsgType.FREE_DRAW);
            freeDrawMsg.setColor("black");
            freeDrawMsg.setLineSize(10);
            ArrayList<String> coordList= new ArrayList<String>();
            coordList.add("1:1");
            coordList.add("1:2");
            coordList.add("1:3");
            coordList.add("2:3");
            freeDrawMsg.setCoordinateList(coordList);
            out1.println(freeDrawMsg);

            // server should respond by propagating the sketch back to only client1
            // because client2 in not on board1
            ServerMsg c1RecvdMsg4 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.FREE_DRAW, c1RecvdMsg4.getType());

            // client2 switches from board2 to board1

            // client2 closes board2
            ClientMsg c2CloseBoard2Msg = new ClientMsg(ClientMsgType.CLOSE_BOARD);
            out2.println(c2CloseBoard2Msg);

            // server responds by sending an updated BOARD_LIST message to client2 only
            // because client 2 is the only user on board2
            userlist2.remove("user2");
            boardlist.put("board2", userlist2);
            ServerMsg c2RecvdMsg4 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg4.getType());
            assertEquals(boardlist, c2RecvdMsg4.getBoardCollaboratorsList()); //boardlist should contain [board1=[user1], board2=[]]

            // client2 opens board1 by sending OPEN_BOARD message
            ClientMsg c2OpenBoard1Msg = new ClientMsg(ClientMsgType.OPEN_BOARD);
            c2OpenBoard1Msg.setBoardName("board1");
            out2.println(c2OpenBoard1Msg);

            // server should updated BOARD_LIST to both clients
            userlist1.add("user2");
            boardlist.put("board1", userlist1);
            ServerMsg c1RecvdMsg5 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, c1RecvdMsg5.getType());
            assertEquals(boardlist, c1RecvdMsg5.getBoardCollaboratorsList()); //boardlist should contain [board1=[user1, user2], board2=[]]
            ServerMsg c2RecvdMsg5 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(boardlist, c2RecvdMsg5.getBoardCollaboratorsList()); //boardlist should contain [board1=[user1, user2], board2=[]]
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg5.getType());

            // server should send client2 current the current sketch on board1
            ServerMsg c2RecvdMsg6 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.FREE_DRAW, c2RecvdMsg6.getType());

            // client2 draws on board2 by sending FREE_DRAW message
            ClientMsg freeDrawMsg2 = new ClientMsg(ClientMsgType.FREE_DRAW);
            freeDrawMsg2.setColor("green");
            freeDrawMsg2.setLineSize(10);
            ArrayList<String> coordList2= new ArrayList<String>();
            coordList2.add("1:0");
            coordList2.add("1:1");
            coordList2.add("1:2");
            coordList2.add("1:3");
            freeDrawMsg2.setCoordinateList(coordList2);
            out2.println(freeDrawMsg2);

            //server should respond by propagating the sketch to both clients
            ServerMsg c1RecvdMsg6 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.FREE_DRAW, c1RecvdMsg6.getType());
            ServerMsg c2RecvdMsg7 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.FREE_DRAW, c2RecvdMsg7.getType());

            // client1 closes board1
            ClientMsg c1CloseBoard1Msg = new ClientMsg(ClientMsgType.CLOSE_BOARD);
            out1.println(c1CloseBoard1Msg);

            // server responds by sending an updated BOARD_LIST message to both clients
            userlist1.remove("user1");
            boardlist.put("board1", userlist1);
            ServerMsg c1RecvdMsg7 = ParseMessage.parseServerMsg(nextNonEmptyLine(in1));
            assertEquals(ServerMsgType.BOARD_LIST, c1RecvdMsg7.getType());
            assertEquals(boardlist, c1RecvdMsg7.getBoardCollaboratorsList()); //boardlist should contain [board1=[user2], board2=[]]
            ServerMsg c2RecvdMsg8 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg8.getType());
            assertEquals(boardlist, c2RecvdMsg8.getBoardCollaboratorsList()); //boardlist should contain [board1=[user2], board2=[]]

            // client2 closes board1
            ClientMsg c2CloseBoard1Msg = new ClientMsg(ClientMsgType.CLOSE_BOARD);
            out2.println(c2CloseBoard1Msg);

            // server responds by sending an updated BOARD_LIST message to client2 only
            // because client 2 is the only user on board1
            userlist1.remove("user2");
            boardlist.put("board1", userlist1);
            ServerMsg c2RecvdMsg9 = ParseMessage.parseServerMsg(nextNonEmptyLine(in2));
            assertEquals(ServerMsgType.BOARD_LIST, c2RecvdMsg9.getType());
            assertEquals(boardlist, c2RecvdMsg9.getBoardCollaboratorsList()); //boardlist should contain [board1=[], board2=[]]

            // client1 sends logout message
            ClientMsg logOut1Msg = new ClientMsg(ClientMsgType.LOGOUT);
            out1.println(logOut1Msg);

            // client2 sends logout message
            ClientMsg logOut2Msg = new ClientMsg(ClientMsgType.LOGOUT);
            out2.println(logOut2Msg);


            socket1.close();
            socket2.close();
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }


    // Utility function to start the WhiteboardServer
    private static void startServer(final int port) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    WhiteboardServer.runWhiteboardServer(port);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

    public static String nextNonEmptyLine(BufferedReader in) throws IOException {
        while (true) {
            String ret = in.readLine();
            if (ret == null || !ret.equals(""))
                return ret;
        }
    }  


}