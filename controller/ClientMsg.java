package controller;

/*
 * This class handles messages that are received by the server
 * Once the request string (that comes across socket from the client)
 * is processed, this class helps to hold all the parsed information
 * in the form of an object
 * 
 * Provides get/set methods to manage all the data and also relies 
 * on the CommonMsg class (from which this class is derived)
 * Also provides methods (add* functions) to construct the text that is
 * used to send requests from the client to the server
 */

public class ClientMsg extends CommonMsg {
    private ClientMsgType msgType;

    /**
     * Constructor
     * @param msgType message Type enum
     */
    public ClientMsg(ClientMsgType msgType) {
        this.msgType = msgType;
    }

    /**
     * Constructor
     */
    public ClientMsg() {
    }

    /**
     * Gets the enum for the message type
     * @return ServerMsgType message Type enum
     */
    public ClientMsgType getType() {
        return msgType;
    }

    /**
     * Sets the message type
     * @param type Message type enum
     */
    public void setType(ClientMsgType type) {
        this.msgType = type;
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
        case LOGIN:
            sb.append("login");
            addComma(sb);
            addUser(sb);
            break;
        case LOGOUT:
            sb.append("logout");
            break;
        case NEW_BOARD:
            sb.append("new");
            addComma(sb);
            addBoard(sb);
            break;
        case OPEN_BOARD:
            sb.append("open");
            addComma(sb);
            addBoard(sb);
            break;
        case CLOSE_BOARD:
            sb.append("close");
            break;
        case FREE_DRAW:
            sb.append("freedraw");
            addComma(sb);
            addDraw(sb);
            break;
        default:
        }
        return sb.toString();
    }
}
