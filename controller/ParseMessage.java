package controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

/*
 * This is a utility class used for parsing the messages sent by both 
 * the client and the server
 * 
 * This is a static class and thread safe. Operates only on local 
 * variables besides the static compiled patterns (which are thread safe
 * when different matchers are used
 * 
 */
public class ParseMessage {

    // Pre-compiled regex patterns
    static Pattern patternName = Pattern.compile("[A-Za-z]([A-Za-z0-9]{0,14})");
    static Pattern patternNum = Pattern.compile("[0-9]+");
    static Pattern patternCoordPair = Pattern.compile("[+-]?[0-9]+:[+-]?[0-9]+");
    // valid color list
    static String[] validcolors = {"red", "blue", "green", "yellow", "black", "white"};
    /**
     * This method parses messages received by the server from the client
     * Encapsulates message in a ClientMsg object.
     * 
     * @param input request string
     * @return
     * @throws RuntimeException
     */
    public static ClientMsg parseClientMsg(String input) throws RuntimeException {
        ClientMsg msg = new ClientMsg();

        // key value pairs are separated by ','
        String[] tokens = input.split(",");
        String reqType = tokens[0]; // first token defines the request type
        switch (reqType) {
        case "login" : 
            msg.setType(ClientMsgType.LOGIN); 
            break;
        case "logout" : 
            msg.setType(ClientMsgType.LOGOUT); 
            break;
        case "new" : 
            msg.setType(ClientMsgType.NEW_BOARD); 
            break;
        case "open" : 
            msg.setType(ClientMsgType.OPEN_BOARD); 
            break;
        case "close" : 
            msg.setType(ClientMsgType.CLOSE_BOARD); 
            break;
        case "freedraw" : 
            msg.setType(ClientMsgType.FREE_DRAW); 
            break;
        default : 
            throw new RuntimeException("Unknown request: " + tokens[0]);

        }
        // If there are more tokens, process them as key value pairs
        for (int i = 1; i < tokens.length; i++) {
            processKeyValue(msg, tokens[i]);
        }

        // Now make sure required fields are present 
        switch (reqType) {
        case "login" :
            if (msg.getUserName() == null) {
                throw new RuntimeException("user name missing"); 
            }
            if (tokens.length != 2) {
                throw new RuntimeException("Invalid attributes present"); 
            }
            break;
        case "new":
        case "open":
            if (msg.getBoardName() == null) {
                throw new RuntimeException("whiteboard name missing"); 
            }
            if (tokens.length != 2) {
                throw new RuntimeException("Invalid attributes present"); 
            }
            break;
        case "close":
        case "logout":
            if (tokens.length != 1) {
                throw new RuntimeException("tokens other than " + reqType + " present");
            }
            break;
        case "freedraw":
            if (msg.getColor() == null)
                throw new RuntimeException("Missing color attribute");
            if (msg.getLineSize() == -1)
                throw new RuntimeException("Missing line size attribute");
            if (msg.getCoordinateList() == null)
                throw new RuntimeException("Missing coord attribute");
            break;

        }
        return msg;
    }

    /**
     * This method parses messages received by the client (from the server)
     * Encapsulates message in a ServerMsg object.
     * 
     * @param input request string
     * @return
     * @throws RuntimeException
     */
    public static ServerMsg parseServerMsg(String input) throws RuntimeException {
        ServerMsg msg = new ServerMsg();

        String[] tokens = input.split(",");
        String reqType = tokens[0];
        switch (reqType) {
        case "freedraw" : 
            msg.setType(ServerMsgType.FREE_DRAW); 
            break;
        case "boardlist":
            msg.setType(ServerMsgType.BOARD_LIST);
            break;
        case "duplicatename":
            msg.setType(ServerMsgType.DUPLICATE_NAME);
            break;
        case "boardexists":
            msg.setType(ServerMsgType.BOARD_EXISTS);
            break;
        case "notloggedin":
            msg.setType(ServerMsgType.NOT_LOGGED_IN);
            break;
        case "boardnotexists":
            msg.setType(ServerMsgType.BOARD_NOT_EXIST);
            break;
        default : 
            throw new RuntimeException("Unknown request: " + tokens[0]);

        }

        if (reqType.equals("boardlist")) {
            processBoardList(msg, input);
        }
        else {
            for (int i = 1; i < tokens.length; i++) {
                processKeyValue(msg, tokens[i]);
            }
        }

        return msg;
    }
    /**
     * Processes common key-value pairs (common to both server and client messages)
     * 
     * @param msg CommonMsg
     * @param str String
     * @throws RuntimeException
     */
    private static void processKeyValue (CommonMsg msg, String str) throws RuntimeException {

        String[] tokens = str.split("=");
        if (tokens.length != 2) { // not a key=value pair
            throw new RuntimeException ("not a key value pair");
        }

        switch (tokens[0]) {
        case "whiteboard" :
            if (!patternName.matcher(tokens[1]).matches())  
                throw new RuntimeException("Invalid board name");
            msg.setBoardName(tokens[1]);
            break;
        case "color":
            if (! Arrays.asList(validcolors).contains(tokens[1]) )
                throw new RuntimeException("Invalid color");
            msg.setColor(tokens[1]);
            break;
        case "user":
            if (!patternName.matcher(tokens[1]).matches())  
                throw new RuntimeException("Invalid user name");
            msg.setUserName(tokens[1]);
            break;
        case "size":
            if (!patternNum.matcher(tokens[1]).matches())  
                throw new RuntimeException("Invalid size");
            msg.setLineSize(Integer.parseInt(tokens[1]));
            break;
        case "coord":
            String[] coordinates = tokens[1].split(";");
            ArrayList<String> coordList = new ArrayList<String>();
            for (int i=0; i < coordinates.length; i++) {
                // Check each of the coordinate pairs are in proper form number:number
                if (!patternCoordPair.matcher(coordinates[i]).matches())  
                    throw new RuntimeException("Invalid coordinates");
                coordList.add(coordinates[i]);
            }
            msg.setCoordinateList(coordList);
            break;

        default:
            throw new RuntimeException("Unknown key: " + tokens[0]);
        }
    }

    /**
     * Processes boardList message sent by the server. This is a list of all the boards available 
     * on the server, along with collaborators for each of them.
     * 
     * @param msg ServerMsg
     * @param str String
     * @throws RuntimeException
     */
    private static void processBoardList(ServerMsg msg, String str) throws RuntimeException {
        String[] tokens = str.split(",");
        if (tokens.length==1) {
            // boardlist is empty
            return;
        }
        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        for (int i = 1; i < tokens.length; i++) {
            // Each token would be of the form "whiteboard=name;userlist=user1:user2:user3"
            String[] boardInfo = tokens[i].split(";");
            String[] boardName = boardInfo[0].split("=");
            if (!boardName[0].equals("whiteboard") ||
                    !patternName.matcher(boardName[1]).matches() ) {
                throw new RuntimeException ("Invalid board name");
            }
            ArrayList<String> uList = new ArrayList<String>();
            if (boardInfo.length > 1)
            {
                String[] userList =  boardInfo[1].split("=");
                if (!userList[0].equals("userlist")) {
                    throw new RuntimeException ("missing userlist");
                }
                String[] users = userList[1].split(":");
                for (int j=0; j<users.length; j++) {
                    if (!patternName.matcher(users[j]).matches()) {
                        throw new RuntimeException ("Invalid user name in userlist");
                    }
                    uList.add(users[j]);
                }
            }
            map.put(boardName[1], uList);
        }
        msg.setBoardCollaboratorsList(map);
    }

    /**
     * Utility routine to be used by the GUI to parse
     * the input user name and board name
     * 
     * @param name string to parse
     * @return true/false boolean
     */
    public static boolean isValidName(String name) {
        if (patternName.matcher(name).matches())
            return true;
        return false;

    }
}
