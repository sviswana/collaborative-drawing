package whiteboard.server;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class Whiteboard {
    public String name;
    public ArrayList<String> sketches;
    public ArrayList<String> currentUsernames;
    public HashMap<String, PrintWriter> printWriterMap = new HashMap<String, PrintWriter>();

    /**
     * Constructs a whiteboard
     * Add new user to currentUsernames
     * Add user's PrintWriter to map
     * @param boardName unique name that identifies this whiteboard
     * @param username  name of user who creates the whiteboard
     * @param print_writer PrintWriter of the user
     */
    public Whiteboard(String boardName, String username, PrintWriter print_writer){
        this.name = boardName;
        this.sketches = new ArrayList<String>();
        this.currentUsernames = new ArrayList<String>();
        this.addUser(username, print_writer);
    }

    /**
     * Constructs a whiteboard
     * Users will be added later
     * @param boardName
     */
    public Whiteboard(String boardName){
        this.name = boardName;
        this.sketches = new ArrayList<String>();
        this.currentUsernames = new ArrayList<String>();
    }

    /**
     * @return name of board
     */
    public String getBoardName(){
        return this.name;
    }

    /**
     * Adds a new user to the whiteboard and adds its PrintWriter to map
     * @param newUser name of new user
     * @param print_writer PrintWriter of new user
     */
    public void addUser(String newUser, PrintWriter print_writer){
        this.currentUsernames.add(newUser);
        this.addPrintWriter(newUser, print_writer);
    }

    /**
     * Removes specified user from the whiteboard
     * Removes user and its corresponding PrintWriter from map
     * @param user name of user to be removed
     */
    public void removeUser(String user){
        this.currentUsernames.remove(user);
        this.removePrintWriter(user);
    }

    /**
     * @return list of usernames of all current users of the whiteboard
     */
    public ArrayList<String> getUsernames(){
        return this.currentUsernames;
    }

    /**
     * Adds a new sketch to whiteboard
     * @param newSketch a new sketch to be added to whiteboard
     */
    public void addSketch(String newSketch){
        this.sketches.add(newSketch);
    }

    /**
     * @return all sketches on the whiteboard
     */
    public ArrayList<String> getSketches(){
        return this.sketches;
    }

    /**
     * @return map where keys are usernames and values are their corresponding PrintWriters
     */
    public HashMap<String,PrintWriter> getPrintWriterMap(){
        return printWriterMap;

    }

    /**
     * Add new user and its corresponding PrintWriter to the map
     * @param username name of user
     * @param print_writer user's print writer object
     */
    public void addPrintWriter(String username, PrintWriter print_writer){
        this.printWriterMap.put(username, print_writer);
    }

    /**
     * Remove user and its corresponding PrintWriter from the map
     * @param username name of user to be removed
     */
    public void removePrintWriter(String username){
        this.printWriterMap.remove(username);
    }

}
