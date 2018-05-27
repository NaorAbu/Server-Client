package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class WRQMsg extends TFTPMsg {

    private final String Filename;

    public String getFilename() {
        return Filename;
    }

    public WRQMsg(String Filename) {
        super((short)2);
        this.Filename = Filename;
    }
}
