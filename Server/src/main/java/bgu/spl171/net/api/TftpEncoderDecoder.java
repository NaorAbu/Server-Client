package bgu.spl171.net.api;

import bgu.spl171.net.api.MessagesTypes.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TftpEncoderDecoder<T> implements MessageEncoderDecoder<TFTPMsg> {

    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
    private short opCode = -1;
    private short pSize = -1;
    private short bNum = -1;
    private int dataCounter = 0;
    private Boolean isAdded = null;
    private byte[] bytes = new byte[1 << 10];
    private int byteslen = 0;
    private TFTPMsg msg;

    @Override
    public TFTPMsg decodeNextByte(byte nextByte) {
        if (opCode == -1) { //indicates that we are still reading the length
            lengthBuffer.put(nextByte);
            if (!lengthBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the length
                lengthBuffer.flip();
                opCode = lengthBuffer.getShort();
                lengthBuffer.clear();
                if (opCode == 6) {//DIRQ
                    TFTPMsg msg = new DIRQMsg();
                    reset();
                    return msg;
                } else if (opCode == 10) {//DISCONNECT
                    msg = new DISCMsg();
                    reset();
                    return msg;
                }
            }
        } else {
            switch (opCode) {
                case 1:
                    if (nextByte != '\0') {
                        pushByte(nextByte);
                    } else {
                        msg = new RRQMsg(new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                        reset();
                        return msg;
                    }
                    break;
                case 2:
                    if (nextByte != '\0') {
                        pushByte(nextByte);
                    } else {
                        msg = new WRQMsg(new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                        reset();
                        return msg;
                    }
                    break;
                case 3:
                    if (pSize == -1) {
                        lengthBuffer.put(nextByte);
                        if (!lengthBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the length
                            lengthBuffer.flip();
                            pSize = lengthBuffer.getShort();
                            if(pSize<0){
                                return new ErrorMsg((short)0, "PACKET SIZE INVALID");
                            }
                            bytes = new byte[pSize];
                            lengthBuffer.clear();
                        }
                    } else {
                        if (bNum == -1) {
                            lengthBuffer.put(nextByte);
                            if (!lengthBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the length
                                lengthBuffer.flip();
                                bNum = lengthBuffer.getShort();
                                lengthBuffer.clear();
                                if (pSize == 0) {
                                    TFTPMsg msg = new DataMsg(pSize, bNum, bytes);
                                    reset();
                                    return msg;
                                }
                            }
                        } else {
                            if (dataCounter < pSize) {
                                pushByte(nextByte);
                                dataCounter++;
                            }
                            if (dataCounter == pSize) {
                                TFTPMsg msg = new DataMsg(pSize, bNum, bytes);
                                reset();
                                return msg;
                            }
                        }
                    }
                    break;
                case 4:
                    if (bNum == -1) {
                        lengthBuffer.put(nextByte);
                        if (!lengthBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the length
                            lengthBuffer.flip();
                            bNum = lengthBuffer.getShort();
                            lengthBuffer.clear();
                            msg = new ACKMsg(bNum);
                            reset();
                            return msg;
                        }
                    }
                    break;
                case 5:
                    if (bNum == -1) {
                        lengthBuffer.put(nextByte);
                        if (!lengthBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the length
                            lengthBuffer.flip();
                            bNum = lengthBuffer.getShort();
                            lengthBuffer.clear();
                        }
                    } else {
                        if (nextByte != '\0') {
                            pushByte(nextByte);
                        } else {
                            msg = new ErrorMsg(bNum, new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                            reset();
                            return msg;
                        }
                    }
                    break;
                case 7:
                    if (nextByte != '\0') {
                        pushByte(nextByte);
                    } else {
                        msg = new LOGRQMsg(new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                        reset();
                        return msg;
                    }
                    break;
                case 8:
                    if (nextByte != '\0') {
                        pushByte(nextByte);
                    } else {
                        msg = new DELRQMsg(new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                        reset();
                        return msg;
                    }
                    break;
                case 9:
                    if (isAdded == null) {
                        isAdded = nextByte == 1;

                    } else {
                        if (nextByte != '\0') {
                            pushByte(nextByte);
                        } else {
                            msg = new BCASTMsg(isAdded, new String(bytes, 0, byteslen, StandardCharsets.UTF_8));
                            reset();
                            return msg;
                        }
                    }
                    break;

                default:
                    msg = new ErrorMsg((short) 4, "UNKNOWN OPERATION CODE!");
                    reset();
                    return msg;
            }
        }
        return null;
    }


    private void pushByte(byte nextByte) {
        if (byteslen >= bytes.length) {
            bytes = Arrays.copyOf(bytes, byteslen * 2);
        }
        bytes[byteslen++] = nextByte;
    }

    private void reset() {
        bNum = -1;
        opCode = -1;
        byteslen = 0;
        bytes = new byte[1<<10];
        pSize = -1;
        dataCounter = 0;
        lengthBuffer.clear();
    }


    @Override
    public byte[] encode(TFTPMsg message) {
        List<Byte> byteLinkedList = new LinkedList<Byte>();
        addToList(byteLinkedList, shortToBytes(message.getOpCode()));
        switch (message.getOpCode()) {
            case 1://READ
                try {
                    addToList(byteLinkedList, ((RRQMsg) message).getFileName().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
            case 2://WRITE
                try {
                    addToList(byteLinkedList, ((WRQMsg) message).getFilename().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
            case 3://DATA PACKET
                addToList(byteLinkedList, shortToBytes(((DataMsg) message).getpSize()));
                addToList(byteLinkedList, shortToBytes(((DataMsg) message).getbNum()));
                addToList(byteLinkedList, ((DataMsg) message).getData());
                break;
            case 4://ACK
                addToList(byteLinkedList, shortToBytes(((ACKMsg) message).getbNum()));
                break;
            case 5://ERROR
                addToList(byteLinkedList, shortToBytes(((ErrorMsg) message).getErrCode()));
                try {
                    addToList(byteLinkedList, ((ErrorMsg) message).getError().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
            case 7://LOGIN
                try {
                    addToList(byteLinkedList, ((LOGRQMsg) message).getUserName().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
            case 8://DELETE
                try {
                    addToList(byteLinkedList, ((DELRQMsg) message).getFileName().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
            case 9://BCAST
                byteLinkedList.add((byte) (((BCASTMsg) message).getAdded() ? 1 : 0));
                try {
                    addToList(byteLinkedList, ((BCASTMsg) message).getFileName().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                byteLinkedList.add((byte) '\0');
                break;
        }
        return listToArr(byteLinkedList);
    }

    private byte[] listToArr(List<Byte> list) {
        byte[] ans = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ans[i] = list.get(i);
        }
        return ans;
    }

    private void addToList(List<Byte> list, byte[] arr) {
        for (byte b : arr) {
            list.add(b);
        }
    }

    private byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte) ((num >> 8) & 0xFF);
        bytesArr[1] = (byte) (num & 0xFF);
        return bytesArr;
    }
}
