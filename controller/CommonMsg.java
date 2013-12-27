package controller;

import java.util.ArrayList;

/*
 * This class implements messages that are common to both server and client
 * as well as fields that are common (like board name, color, linesize etc
 * Once the request string (that comes across socket from either server or
 * client) is processed, this class helps to hold all the parsed information
 * in the form of an object
 * 
 * Provides get/set methods to manage all the data
 * Also provides methods (add* functions) to construct the text that is
 * used to send requests/responses across the socket to the other end
 */
public class CommonMsg {
    private String userName = null;
    private String boardName = null;
    private String color = null;
    // The list of coordinates are maintained in a string array
    // Each string represents one x,y coordinate in the form x:y
    // e.g "100:200" 
    private ArrayList<String> coordList = null;
    private int lineSize = -1;

    public CommonMsg() {
    }

    /**
     * User name is returned
     * @return String user name
     */
    public String getUserName () {
        return userName;
    }

    /**
     * Set the user field
     * @param userName
     */
    public void setUserName (String userName) {
        this.userName = userName;
    }

    /**
     * Returns White board name
     * @return String white board name
     */
    public String getBoardName () {
        return boardName;
    }

    /**
     * Set the white board name
     * @param boardName String
     */
    public void setBoardName (String boardName) {
        this.boardName = boardName;
    }

    /**
     * Return color in String form
     * @return String
     */
    public String getColor () {
        return color;
    }

    /**
     * Set the color
     * @param color String
     */
    public void setColor (String color) {
        this.color = color;
    }

    /**
     * Get the line size
     * @return int 
     */
    public int getLineSize () {
        return lineSize;
    }

    /**
     * Set the linesize
     * @param lineSize integer
     */
    public void setLineSize (int lineSize) {
        this.lineSize = lineSize;
    }

    /**
     * The list of coordinates are maintained in a string array
     * Each string represents one x,y coordinate in the form x:y
     * e.g "100:200" 
     * @return String array
     */
    public ArrayList<String> getCoordinateList () {
        return coordList;
    }

    /**
     * An array of strings representing the coordinates of drawings
     * Each string in the form "x:y" represents a coordinate
     * @param coordList Array of strings
     */
    public void setCoordinateList (ArrayList<String> coordList) {
        this.coordList = coordList;
    }

    /**
     * Utility function that helps in creating the text message that goes across
     * the socket connection between the server and the client
     * Used to add user to the request/response text (e.g (user=joe)
     * @param sb StringBuilder reference
     */
    protected void addUser(StringBuilder sb) {
        sb.append("user=");
        sb.append(getUserName());
    }

    /**
     * Used to add line size to the request/response text (e.g size=10)
     * @param sb StringBuilder reference
     */
    protected void addLineSize(StringBuilder sb) {
        sb.append("size=");
        sb.append(getLineSize());
    }

    /**
     * Used to add color to the request/response text (e.g color=red)
     * @param sb StringBuilder reference
     */
    protected void addColor(StringBuilder sb) {
        sb.append("color=");
        sb.append(getColor());
    }

    /**
     * Used to add whiteboard to the request/response text (e.g whiteboard=myboard)
     * @param sb StringBuilder reference
     */
    protected void addBoard(StringBuilder sb) {
        sb.append("whiteboard=");
        sb.append(getBoardName());
    }

    /**
     * Used to add whiteboard and user to the request/response text 
     * (e.g whiteboard=myboard,user=joe)
     * Since it is common to have both these in a message, a single
     * function is provided to create both
     * @param sb StringBuilder reference
     */
    protected void addBoardAndUser(StringBuilder sb) {
        addBoard(sb);
        addComma(sb);
        addUser(sb);
    }

    /**
     * Constructs the draw text string 
     * e.g "color=black,size=25,coord=300:400;420:500;20:200"
     * @param sb StringBuilder reference
     */
    protected void addDraw(StringBuilder sb) {
        addColor(sb);
        addComma(sb);
        addLineSize(sb);
        addComma(sb);
        addCoordinates(sb);
    }

    /**
     * Used to add all the coordinates that are used in draw messages
     * sent between client and server 
     * (e.g coord=300:400;420:500;20:200)
     *   * @param sb StringBuilder reference
     */
    protected void addCoordinates(StringBuilder sb) {
        ArrayList<String> list = getCoordinateList();
        if (list == null || list.size() == 0)
            return; // no coordinates set
        sb.append("coord=");
        int nElements = list.size();
        for (String s: list) {
            nElements--;
            sb.append(s);
            if (nElements > 0) addSeminColon(sb); // don't add ';' for the last element
        }
    }

    /**
     * Adds "," to separate key-value pairs
     * @param sb StringBuilder reference
     */
    protected void addComma(StringBuilder sb) {
        sb.append(",");
    }

    /**
     * Adds ";" to separate individual x-y coordinates in a list of coordinates
     * @param sb StringBuilder reference
     */
    protected void addSeminColon(StringBuilder sb) {
        sb.append(";");
    }

    /**
     * Adds ":" to separate users in the list of uses on a whiteboard
     * that is sent from server to client
     * @param sb StringBuilder reference
     */
    protected void addColon(StringBuilder sb) {
        sb.append(":");
    }

}
