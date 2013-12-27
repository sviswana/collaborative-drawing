package controller;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ClientMsgTest {

    /*
     * Static class ParseMessage is used to parse input string, which returns 
     * ClientMsg object 
     */

    // Test parsing login message
    @Test
    public void testLogin() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,user=JohnDoe");
        assertEquals (ClientMsgType.LOGIN, msg.getType());
        assertEquals ("JohnDoe", msg.getUserName());
    }
    // Test parsing login message with username set to 15 characters (which is the limit)
    @Test
    public void testLoginWith15Letters() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,user=JohnDoe12345678");
        assertEquals (ClientMsgType.LOGIN, msg.getType());
        assertEquals ("JohnDoe12345678", msg.getUserName());
    }
    // Test parsing new board creation message
    @Test
    public void testNewWb() {
        ClientMsg msg = ParseMessage.parseClientMsg("new,whiteboard=newWb1");
        assertEquals (ClientMsgType.NEW_BOARD, msg.getType());
        assertEquals ("newWb1", msg.getBoardName());
    }
    // Test parsing opening white board message
    @Test
    public void testOpenWb() {
        ClientMsg msg = ParseMessage.parseClientMsg("open,whiteboard=myb1");
        assertEquals (ClientMsgType.OPEN_BOARD, msg.getType());
        assertEquals ("myb1", msg.getBoardName());
    }
    // Test parsing closing white board message
    @Test
    public void testCloseWb() {
        ClientMsg msg = ParseMessage.parseClientMsg("close");
        assertEquals (ClientMsgType.CLOSE_BOARD, msg.getType());
    }
    // Test parsing free draw message
    @Test
    public void testFreeDraw() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=black,size=20,coord=2:4;40:50;99:120;111:333");
        String[] coords = { "2:4", "40:50", "99:120", "111:333" };
        assertEquals (ClientMsgType.FREE_DRAW, msg.getType());
        assertEquals ("black", msg.getColor());
        assertEquals (20, msg.getLineSize());
        assertEquals (Arrays.toString(coords), msg.getCoordinateList().toString());
    }
    // Test logout 
    @Test
    public void testLogout() {
        ClientMsg msg = ParseMessage.parseClientMsg("logout");
        assertEquals (ClientMsgType.LOGOUT, msg.getType());
    }
    // Test parsing free draw message

    // Test invalid user name (user name starting with number"
    @Test(expected = RuntimeException.class)
    public void testInvalidUserNamewithNumberFirst() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,user=2John");
    }
    // Test invalid user name (user name with invalid char '#'
    @Test(expected = RuntimeException.class)
    public void testInvalidUserName() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,user=John#Doe");
    }
    // Test invalid user name (user name with more than 15 characters)
    @Test(expected = RuntimeException.class)
    public void testInvalidUserNameWithMoreThan15Letters() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,user=JohnDoe123456789");
    }
    //  // Test invalid user name (user name with invalid char '#'
    @Test(expected = RuntimeException.class)
    public void testMissingUserNameInLogin() {

        ClientMsg msg = ParseMessage.parseClientMsg("login");
    }

    //  // Test invalid login token where comma after login is missing
    @Test(expected = RuntimeException.class)
    public void testInvalidRequestLogin() {

        ClientMsg msg = ParseMessage.parseClientMsg("login user");
    }

    //  // Test invalid login request where user=name is missing
    @Test(expected = RuntimeException.class)
    public void testInvalidRequestLoginNoUser() {

        ClientMsg msg = ParseMessage.parseClientMsg("login,color=black");
    }

    // Test parsing new board creation message with board name w/ > 15 lettters
    @Test(expected = RuntimeException.class)
    public void testNewWbNameWithMoreThan15Letters() {
        ClientMsg msg = ParseMessage.parseClientMsg("new,whiteboard=newWb12345678901");
        assertEquals (ClientMsgType.NEW_BOARD, msg.getType());
        assertEquals ("newWb1", msg.getBoardName());
    }
    //Test invalid board name (board name has '_' character
    @Test (expected = RuntimeException.class)
    public void testInvalidBoardName() {

        ClientMsg msg = ParseMessage.parseClientMsg("open,whiteboard=w_b");
    }
    //Test missing whiteboard=name attribute
    @Test (expected = RuntimeException.class)
    public void testMissingBoardName() {

        ClientMsg msg = ParseMessage.parseClientMsg("open,whitebo=myboard");
    }
    //Test extra attributes in open request (size=10 is invalid here)
    @Test (expected = RuntimeException.class)
    public void testExtraAttributesinOpen() {

        ClientMsg msg = ParseMessage.parseClientMsg("open,whiteboard=wb,size=10");
    }
    //Test close with extra attribute
    @Test (expected = RuntimeException.class)
    public void testExtraAttributesinClose() {

        ClientMsg msg = ParseMessage.parseClientMsg("close,color=red");
    }
    // Test invalid request type
    @Test (expected = RuntimeException.class)
    public void testInvalidRequestType() {

        ClientMsg msg = ParseMessage.parseClientMsg("someaction,whiteboard=wb,user=John");
    }
    // Invalid character (and not a digit in coordinates)
    @Test (expected = RuntimeException.class)
    public void testFreeDrawInvalidCoord() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=black,size=20,coord=2:4a;40:50;99:120;111:333");
    }
    // Invalid "size" (and not digits)
    @Test (expected = RuntimeException.class)
    public void testFreeDrawInvalidSize() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=black,size=large,coord=2:4;40:50;99:120;111:333");
    }
    // Invalid color (color=rose)
    @Test (expected = RuntimeException.class)
    public void testFreeDrawInvalidSize3() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=rose,size=10,coord=2:4;40:50;99:120;111:333");
    }
    // Missing color attribute in freedraw
    @Test (expected = RuntimeException.class)
    public void testFreeDrawMissingColor() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,size=10,coord=2:4;40:50;99:120;111:333");
    }
    // Missing size attribute in freedraw
    @Test (expected = RuntimeException.class)
    public void testFreeDrawMissingSize() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=black,coord=2:4;40:50;99:120;111:333");
    }
    // Missing coord attribute in freedraw
    @Test (expected = RuntimeException.class)
    public void testFreeDrawMissingCoord() {
        ClientMsg msg = ParseMessage.parseClientMsg("freedraw,color=black,size=10");
    }
    // Invalid logout with invalid key "whiteboard"
    @Test (expected = RuntimeException.class)
    public void testInvalidLogout() {
        ClientMsg msg = ParseMessage.parseClientMsg("logout,whiteboard=joe");
    }

}
