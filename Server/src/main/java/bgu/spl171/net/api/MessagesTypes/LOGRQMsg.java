package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class LOGRQMsg extends TFTPMsg {

    private final String userName;

    public String getUserName() {
        return userName;
    }

    public LOGRQMsg(String s) {
        super((short) 7);
        userName = s;
    }
}
