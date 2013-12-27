package controller;

/*
 * List of message types received by the server from the client
 */
public enum ClientMsgType {
    LOGIN,
    LOGOUT,
    NEW_BOARD,
    OPEN_BOARD,
    CLOSE_BOARD,
    FREE_DRAW,
}
