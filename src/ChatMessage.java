import java.io.Serializable;

/**
 * ChatMessage
 * <p>
 * Message for chat
 *
 * @author Geon An, Arron Smith
 * @version November 26, 2018
 */
final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;

    // Here is where you should implement the chat message object.
    // Variables, Constructors, Methods, etc.

    private int messageType;
    private String message;
    private String username;

    public ChatMessage(int messageType, String username, String message) {
        this.messageType = messageType;
        this.username = username;
        this.message = message;
    }

    public ChatMessage(int messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }
}
