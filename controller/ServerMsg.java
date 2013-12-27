package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/*
 * This class handles messages that are received by the client
 * Once the request string (that comes across socket from the server)
 * is processed, this class helps to hold all the parsed information
 * in the form of an object
 * 
 * Provides get/set methods to manage all the data and also relies 
 * on the CommonMsg class (from which this class is derived)
 * Also provides methods (add* functions) to construct the text that is
 * used to send responses from the server to the client
 */

public class ServerMsg extends CommonMsg  {

    private ServerMsgType msgType;
    // The hashmap below is used to store each whiteboard and all the 
    // collaborators on each of those. The key is the whiteboard name
    // and the values are arrays of strings with user/collaborator names
    HashMap<String, ArrayList<String>> boardCollaboratorsMap = null;

    /**
     * Constructor
     * @param msgType enum for the message type
     */
    public ServerMsg(ServerMsgType msgType) {
        this.msgType = msgType;
    }

    /**
     * Constructor
     */
    public ServerMsg() {
    }

    /**
     * Gets the message type
     * @return msgType enum for the message type
     */
    public ServerMsgType getType() {
        return msgType;
    }

    /**
     * sets the message type 
     * @param type enum for the message type
     */
    public void setType(ServerMsgType type) {
        this.msgType = type;
    }

    /**
     * List of boards and the users on each of those boards in a hashmap
     * The key is the board name and the values are user names (stored as an ArrayList)
     * for that whiteboard
     * @return HashMap
     */
    public HashMap<String, ArrayList<String>> getBoardCollaboratorsList () {
        return boardCollaboratorsMap;
    }

    /**
     * Sets the board collaborator list
     * The key is the board name and the values are user names (stored as an ArrayList)
     * for that whiteboard
     * 
     * @param boardCollaboratorsMap HashMap<String, ArrayList<String>
     */
    public void setBoardCollaboratorsList (HashMap<String, ArrayList<String>> boardCollaboratorsMap) {
        this.boardCollaboratorsMap = boardCollaboratorsMap;
    }

    @Override
    /**
     * From the object creates a string representation. This
     * string is based on the protocol that is used for communication
     * exchanges between the client and the server
     * 
     * Depending on the type of the message, appropriate textual 
     * representation is created
     * @return String String representation for the protocol message
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        switch (getType()) {
        case BOARD_LIST: 
            sb.append("boardlist");
            HashMap<String, ArrayList<String>> map = getBoardCollaboratorsList();
            if (map != null) {
                for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                    addComma(sb);
                    String key = entry.getKey(); // board name
                    sb.append("whiteboard=");
                    sb.append(key);
                    ArrayList<String> list = entry.getValue();
                    if (list != null && list.size() != 0) {
                        sb.append(";userlist=");
                        int nElements = list.size();
                        for (String s: list) {
                            nElements--;
                            sb.append(s);
                            if (nElements > 0) addColon(sb);
                        }
                    }
                }

            }
            break;
        case FREE_DRAW:
            sb.append("freedraw");
            addComma(sb);
            addDraw(sb);
            break;
        case DUPLICATE_NAME:
            sb.append("duplicatename");
            addComma(sb);
            addUser(sb);
            break;
        case BOARD_EXISTS:
            sb.append("boardexists");
            addComma(sb);
            addBoard(sb);
            break;
        case NOT_LOGGED_IN:
            sb.append("notloggedin");
            addComma(sb);
            addUser(sb);
            break;
        case BOARD_NOT_EXIST:
            sb.append("boardnotexists");
            addComma(sb);
            addBoard(sb);
            break;
        }

        return sb.toString();
    }
}
