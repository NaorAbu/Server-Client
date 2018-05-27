package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class DELRQMsg extends TFTPMsg {
    private final String fileName;


    public String getFileName() {
        return fileName;
    }

    public DELRQMsg(String s) {
        super((short) 8);
        this.fileName = s;

    }
}
