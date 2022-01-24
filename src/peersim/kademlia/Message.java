package peersim.kademlia;

/**
 * Message class provide all functionalities to manage the various messages, principally LOOKUP messages (messages from
 * application level sender designated to another application level).
 */
// ______________________________________________________________________________________
public class Message extends SimpleEvent {

    /**
     * Message Type: EMPTY (used to construct empty message)
     */
    public static final int MSG_EMPTY = 0;
    /**
     * Message Type: FINDNODE (message regarding node find)
     */
    public static final int MSG_FINDNODE = 1;
    /**
     * Message Type: ROUTE (message during lookup)
     */
    public static final int MSG_ROUTE = 2;
    /**
     * Message Type: RESPONSE (response message to a findvalue or findnode)
     */
    public static final int MSG_RESPONSE = 3;
    /**
     * Message Type: TIMEOUT (timeout message to a findvalue or findnode)
     */
    public static final int TIMEOUT = 100;

    /**
     * internal generator for unique message IDs
     */
    private static long ID_GENERATOR = 0;
    /**
     * This Object contains the body of the message, no matter what it contains
     */
    public Object body = null;

    /**
     * ID of the message. this is automatically generated univocally, and should not change
     */
    public long msgId;

    /**
     * ACK number of the message. This is in the response message.
     */
    public long ackId;

    /**
     * Id of the search operation
     */
    public long operationId;

    /**
     * Recipient address of the message
     */
    public KademliaNode receiver;


    public KademliaNode sender;

    public boolean newLookup;

    /**
     * Source address of the message: has to be filled at application level
     */
    public KademliaNode src;

    /**
     * This is the node we are looking for
     */
    public KademliaNode target;



    // ______________________________________________________________________________________________

    /**
     * Creates an empty message by using default values (message type = MSG_LOOKUP and <code>new String("")</code> value for the
     * body of the message)
     */
    public Message() {
        this(MSG_EMPTY, "");
    }

    /**
     * Create a message with specific type and empty body
     *
     * @param messageType int type of the message
     */
    public Message(int messageType) {
        this(messageType, "");
    }


    /**
     * Creates a message with specific type and body
     *
     * @param messageType int type of the message
     * @param body        Object body to assign (shallow copy)
     */
    public Message(int messageType, Object body) {
        super(messageType);
        this.msgId = (ID_GENERATOR++);
        this.body = body;
    }


    /**
     * Constructor for timeout messages
     * @param messageType
     * @param operationId
     */
    public Message(int messageType, long operationId, KademliaNode sender, KademliaNode receiver){
        super(messageType);
        this.msgId = (ID_GENERATOR++);
        this.operationId = operationId;

        //note that the current code simulates that the receiver of a message sends after some latency a timeout to sender
        //this means that the source of this message is the receiver of the timeout request
        this.sender = sender;
        this.receiver = receiver;
        this.newLookup = false;
    }


    /**
     * Encapsulates the creation of a message.
     *
     * @param body Object
     * @return Message
     */
    public static final Message makeEmptyMessage(Object body, int type) {
        return new Message(type, body);
    }


    /**
     * Print the message info.
     *
     * @return The printed message.
     */
    public String toString() {
        String s = "[ID=" + msgId + "][DEST=" + receiver + "]";
        return s + "[Type=" + messageTypetoString() + "] BODY=(...)";
    }

    /**
     * Copy the message.
     *
     * @return The copy message.
     */
    public Message copy() {
        Message dolly = new Message();
        dolly.type = this.type;
        dolly.target = this.target;
        dolly.src = this.src;
        dolly.sender = this.sender;
        dolly.receiver = this.receiver;
        dolly.operationId = this.operationId;
        dolly.body = this.body; // deep cloning?
        dolly.newLookup = this.newLookup;
        return dolly;
    }

    /**
     * Translates the type of message to a string.
     *
     * @return The corresponding string value.
     */
    public String messageTypetoString() {
        switch (type) {
            case MSG_EMPTY:
                return "MSG_EMPTY";
            case MSG_FINDNODE:
                return "MSG_FINDNODE";
            case MSG_ROUTE:
                return "MSG_ROUTE";
            case MSG_RESPONSE:
                return "MSG_RESPONSE";
            default:
                return "UNKNOWN:" + type;
        }
    }


    public void setTarget(KademliaNode target){
        this.target = target;
    }

}
