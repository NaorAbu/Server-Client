package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class RRQMsg extends TFTPMsg {

    private final String FileName;

    public String getFileName() {
        return FileName;
    }

    public RRQMsg(String FileName) {
        super((short) 1);
        this.FileName = FileName;


    }
}
