import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 */

public class Message implements Serializable {

    // The different types of message sent by the Client
    // GETUSERS to receive the list of the users connected
    // MESSAGE an ordinary text message
    // SIGNOUT to disconnect from the Server
    private MessageType type;
    private String message;
    
    // constructor
    Message(MessageType type, String message) {
        this.type = type;
        this.message = message;
    }
    
    MessageType getType() {
        return type;
    }

    String getMessage() {
        return message;
    }
}