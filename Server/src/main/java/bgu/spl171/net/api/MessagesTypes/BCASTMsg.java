package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class BCASTMsg extends TFTPMsg {
    private final Boolean isAdded;
    private final String fileName;

    public BCASTMsg(Boolean isAdded, String s) {
        super((short) 9);
        fileName = s;
        this.isAdded = isAdded;

    }

    public Boolean getAdded() {
        return isAdded;
    }

    public String getFileName() {
        return fileName;
    }
}
