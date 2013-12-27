package controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

/*
 * This tests the generation of text that is exchanged across client
 * and server (implementing the white board collaboration protocol) based
 * on message objects. 
 * 
 * We provided ClientMsg and ServerMsg objects so the user can use those to create
 * key=value pairs. This way, they can easily generate the text representation of 
 * the protocol that goes on the wire 
 * 
 * There are no exception tests for this module as we assume valid values are used for
 * setting various fields. Anyway, the other side on receiving this text, will run this 
 * through a parser which will catch all the errors
 * 
 */
public class MessageGenTest {

    /*
     * ****************  Client Message generation tests  ************************
     * 
     */
    // test generation of login message from clientmsg object
    @Test
    public void testLoginMsg() {
        String expected = "login,user=joe";
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.LOGIN);
        msg.setUserName("joe");
        assert(msg.toString().equals(expected));
    }

    // test generation of new board message from clientmsg object
    @Test
    public void testNewBoardMsg() {
        String expected = "new,whiteboard=wb1";
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.NEW_BOARD);
        msg.setBoardName("wb1");
        assert(msg.toString().equals(expected));
    }
    // test generation of open board message from clientmsg object
    @Test
    public void testOpenBoardMsg() {
        String expected = "open,whiteboard=wb1";
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.OPEN_BOARD);
        msg.setBoardName("wb1");
        assert(msg.toString().equals(expected));
    }
    // test generation of free draw message from clientmsg object
    @Test
    public void testFreeDrawMsg() {
        String expected = "freedraw,color=black,size=20,coord=2:4;40:50";
        ArrayList<String> coordList = new ArrayList<String>();
        coordList.add("2:4");
        coordList.add("40:50");
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.FREE_DRAW);
        msg.setLineSize(20);
        msg.setColor("black");
        msg.setCoordinateList(coordList);
        assert(msg.toString().equals(expected));
    }
    // test generation of close board message from clientmsg object
    @Test
    public void testCloseBoardMsg() {
        String expected = "close";
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.CLOSE_BOARD);
        assert(msg.toString().equals(expected));
    }
    // test generation of logout board message from clientmsg object
    @Test
    public void testLogoutMsg() {
        String expected = "logout";
        ClientMsg msg = new ClientMsg();
        msg.setType(ClientMsgType.LOGOUT);
        assert(msg.toString().equals(expected));
    }
    //

    /*
     * ****************  Sever Msg generation tests  ************************
     * 
     */
    // test generation of boardlist message from ServerMsg object
    @Test
    public void testBoardListMsg() {
        String expected = "boardlist,whiteboard=wb1;userlist=u1:u2";
        ServerMsg msg = new ServerMsg();
        msg.setType(ServerMsgType.BOARD_LIST);

        String[] users = { "u1", "u2" };
        ArrayList<String> userlist = new ArrayList<String>(Arrays.asList(users));
        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        map.put("wb1", userlist);
        msg.setBoardCollaboratorsList(map);
        assert(msg.toString().equals(expected));
    }
    // test generation of duplicate name message from ServerMsg object
    @Test
    public void testDuplicateNameMsg() {
        String expected = "duplicatename,user=john";
        ServerMsg msg = new ServerMsg();
        msg.setType(ServerMsgType.DUPLICATE_NAME);
        msg.setUserName("john");
        assert(msg.toString().equals(expected));
    }
    // test generation of board not exists message from ServerMsg object
    @Test
    public void testBoardNotExistMsg() {
        String expected = "boardnotexists,whiteboard=myboard";
        ServerMsg msg = new ServerMsg();
        msg.setType(ServerMsgType.BOARD_NOT_EXIST);
        msg.setBoardName("myboard");
        assert(msg.toString().equals(expected));
    }
    // test generation of board exists message from ServerMsg object
    @Test
    public void testBoardExistMsg() {
        String expected = "boardexists,whiteboard=myboard";
        ServerMsg msg = new ServerMsg();
        msg.setType(ServerMsgType.BOARD_EXISTS);
        msg.setBoardName("myboard");
        assert(msg.toString().equals(expected));
    }
}
