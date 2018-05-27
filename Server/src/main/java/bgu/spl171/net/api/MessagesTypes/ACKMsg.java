package bgu.spl171.net.api.MessagesTypes;

/**
 * Created by muszk on 1/11/2017.
 */
public class ACKMsg extends TFTPMsg {
    private final short bNum;

    public short getbNum() {
        return bNum;
    }

    public ACKMsg(short bNum) {
        super((short) 4);
        this.bNum = bNum;
    }
}
