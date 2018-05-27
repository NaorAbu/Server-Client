package bgu.spl171.net.api.MessagesTypes;

public class ErrorMsg extends TFTPMsg {
    private final short ErrCode;
    private final String Error;

    public ErrorMsg(short ErrCode, String s) {
        super((short) 5);
        this.ErrCode = ErrCode;
        Error = s;
    }

    public short getErrCode() {
        return ErrCode;
    }

    public String getError() {
        return Error;
    }
}
