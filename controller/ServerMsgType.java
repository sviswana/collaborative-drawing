package controller;
/*
 * List of message types received by the client from the server
 */

public enum ServerMsgType {
    BOARD_LIST,
    FREE_DRAW,
    DUPLICATE_NAME,
    BOARD_EXISTS,
    NOT_LOGGED_IN,
    BOARD_NOT_EXIST,
}
