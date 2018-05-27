package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class DataMsg extends TFTPMsg {

    private final short pSize;
    private final short bNum;
    private final byte[] data;


    public short getpSize() {
        return pSize;
    }

    public short getbNum() {
        return bNum;
    }

    public byte[] getData() {
        return data;
    }


    public DataMsg(short pSize, short bNum, byte[] data) {
        super((short) 3);
        this.pSize = pSize;
        this.bNum = bNum;
        this.data = data;
    }
}
