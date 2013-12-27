package controller;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/*
 * Static class ParseMessage is used to parse input string, which returns 
 * ServerMsg object 
 */

public class ServerMsgTest {

    // Parse board list message
    @Test
    public void testBoardList() {
        HashMap<String, ArrayList<String>> map;
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist,whiteboard=wb1;userlist=u1:u2");
        String[] users = { "u1", "u2" };
        assertEquals (ServerMsgType.BOARD_LIST, msg.getType());
        map = msg.getBoardCollaboratorsList();
        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
            ArrayList<String> value = entry.getValue();
            // verify all the user names from userlist are correct
            assertEquals(value.toString(), Arrays.toString(users));

        }
    }
    // Parse empty board list (empty list is valid)
    @Test
    public void testEmptyBoardList() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist");
        assertEquals (ServerMsgType.BOARD_LIST, msg.getType());
        assertEquals (null, msg.getBoardCollaboratorsList());
    }
    // Parse freedraw message
    @Test
    public void testFreeDraw() {
        ServerMsg msg = ParseMessage.parseServerMsg("freedraw,color=black,size=25,coord=300:400;420:500");
        String[] coords = { "300:400", "420:500" };
        assertEquals (ServerMsgType.FREE_DRAW, msg.getType());
        assertEquals ("black", msg.getColor());
        assertEquals (Arrays.toString(coords), msg.getCoordinateList().toString());
    }
    // Parse duplicatename message
    @Test
    public void testDuplicateName() {
        ServerMsg msg = ParseMessage.parseServerMsg("duplicatename,user=alex");
        assertEquals (ServerMsgType.DUPLICATE_NAME, msg.getType());
        assertEquals ("alex", msg.getUserName());
    }
    // Parse board exists message
    @Test
    public void testBoardExists() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardexists,whiteboard=GreatBoard");
        assertEquals (ServerMsgType.BOARD_EXISTS, msg.getType());
        assertEquals ("GreatBoard", msg.getBoardName());
    }
    // Parse not logged in message
    @Test
    public void testNotLoggedIn() {
        ServerMsg msg = ParseMessage.parseServerMsg("notloggedin,user=Mike");
        assertEquals (ServerMsgType.NOT_LOGGED_IN, msg.getType());
        assertEquals ("Mike", msg.getUserName());
    }
    // Parse board not exist in message
    @Test
    public void testBoardNotExist() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardnotexists,whiteboard=UglyBoard");
        assertEquals (ServerMsgType.BOARD_NOT_EXIST, msg.getType());
        assertEquals ("UglyBoard", msg.getBoardName());
    }
    // Parse invalid user list and throw exception
    @Test (expected = RuntimeException.class)
    public void testInvalidUserList() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist,whiteboard=wb1;userlist=u%:u2");
    }
    // Parse missing user list and throw exception
    @Test (expected = RuntimeException.class)
    public void testMissingUserList() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist,whiteboard=wb1;users=u1:u2");
    }
    // Parse invalid board name and throw exception
    @Test (expected = RuntimeException.class)
    public void testInvalidBoardName() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist,whiteboard=wb#s;users=u1:u2");
    }
    // Parse missing whiteboard key and throw exception
    @Test (expected = RuntimeException.class)
    public void testMissingBoardListKey() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardlist,board=wb;users=u1:u2");
    }
    // Parse missing user name
    @Test (expected = RuntimeException.class)
    public void testMissingUserinNotLoggedInMsg() {
        ServerMsg msg = ParseMessage.parseServerMsg("notloggedin,board=wb");
    }
    // Parse missing whiteboardboard key
    @Test (expected = RuntimeException.class)
    public void testMissingBoardinBoardNotExists() {
        ServerMsg msg = ParseMessage.parseServerMsg("boardnotexists,board=wb");
    }

}
