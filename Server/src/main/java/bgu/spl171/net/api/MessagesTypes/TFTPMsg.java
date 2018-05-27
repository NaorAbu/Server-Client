package bgu.spl171.net.api.MessagesTypes;

public abstract class TFTPMsg {

    private final short opCode;

    public TFTPMsg(short i) {
        opCode = i;
    }

    public short getOpCode() {
        return opCode;
    }
}
