package bgu.spl171.net.api.bidi;


import bgu.spl171.net.api.MessagesTypes.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;


public class TftpProtocol<T> implements BidiMessagingProtocol<TFTPMsg> {

    private static ConcurrentHashMap<Integer, String> connected = new ConcurrentHashMap<>();
    private static LinkedList<String> InUse = new LinkedList<>();
    private String path;
    private ConcurrentLinkedDeque<DataMsg> toSend;
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<TFTPMsg> connections;
    private Path p;
    private FileOutputStream out;
    private FileInputStream in;
    private byte[] bytes;
    private String fileName;
    private String fileDir = System.getProperty("user.dir") + File.separator + "Files";

    @Override
    public void start(int connectionId, Connections<TFTPMsg> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.toSend = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void process(TFTPMsg message) {
        boolean isConnected = true;
        if (!(message instanceof LOGRQMsg)) {
            if (!connected.containsKey(connectionId)) {
                isConnected = false;
                connections.send(connectionId, new ErrorMsg((short) 6, "NOT LOGGED IN YET!"));
            }
        }
        if (isConnected) {
            switch (message.getOpCode()) {
                case 1:// READ
                    if (isFileInFiles(((RRQMsg) message).getFileName()) && !((RRQMsg) message).getFileName().endsWith(".TheMuszkal")) {
                        fileName = ((RRQMsg) message).getFileName();
                        InUse.add(fileName);
                        try {
                            in = new FileInputStream(fileDir + File.separator + ((RRQMsg) message).getFileName());
                            if (in.available() >= 512) {
                                bytes = new byte[512];
                            } else {
                                bytes = new byte[in.available()];
                            }
                            in.read(bytes, 0, bytes.length);
                            if (bytes.length < 512) {
                                in.close();
                                in = null;
                                InUse.remove(fileName);
                            }
                            connections.send(connectionId, new DataMsg((short) bytes.length, (short) 1, bytes));
                        } catch (IOException e) {
                            connections.send(connectionId, new ErrorMsg((short) 2, "Error Reading File"));
                        }
                    } else {
                        connections.send(connectionId, new ErrorMsg((short) 1, "No Such File!"));
                    }
                    break;
                case 2://WRITE
                    if (!isFileInFiles(((WRQMsg) message).getFilename()) && !isFileInFiles(((WRQMsg) message).getFilename() + ".TheMuszkal")) {
                        try {
                            fileName = ((WRQMsg) message).getFilename();
                            InUse.add(fileName);
                            InUse.add(fileName+".TheMuszkal");
                            out = new FileOutputStream(fileDir + File.separator + ((WRQMsg) message).getFilename() + ".TheMuszkal");
                            connections.send(connectionId, new ACKMsg((short) 0));
                        } catch (FileNotFoundException e) {
                            connections.send(connectionId, new ErrorMsg((short) 0, "Cannot Write File"));
                        }
                    } else {
                        connections.send(connectionId, new ErrorMsg((short) 5, "File Already Exist"));
                    }
                    break;
                case 3://DATA
                    try {
                        out.write(((DataMsg) message).getData());
                        connections.send(connectionId, new ACKMsg(((DataMsg) message).getbNum()));
                        if (((DataMsg) message).getpSize() < 512) {
                            File f = new File(fileDir + File.separator + fileName + ".TheMuszkal");
                            f.renameTo(new File(fileDir + File.separator + fileName));
                            out.close();
                            out = null;
                            InUse.remove(fileName);
                            InUse.remove(fileName+".TheMuszkal");
                            process(new BCASTMsg(true, (fileName)));
                        }
                    } catch (IOException e) {
                        connections.send(connectionId, new ErrorMsg((short) 0, "Cannot Write File"));
                    }
                    break;
                case 4://ACK
                    if (((ACKMsg) message).getbNum() != 0) {
                        if (in != null) {
                            try {
                                if (in.available() >= 512) {
                                    bytes = new byte[512];
                                } else {
                                    bytes = new byte[in.available()];
                                }
                                in.read(bytes, 0, bytes.length);
                                if (bytes.length < 512) {
                                    in.close();
                                    in = null;
                                    InUse.remove(fileName);
                                }
                                connections.send(connectionId, new DataMsg((short) bytes.length, (short) (((ACKMsg) message).getbNum() + 1), bytes));
                            } catch (IOException e) {
                                connections.send(connectionId, new ErrorMsg((short) 0, "Cannot Write File"));
                            }
                        } else {
                            if (!toSend.isEmpty()) {
                                connections.send(connectionId, toSend.pollFirst());
                            }
                        }
                    }
                    break;
                case 5://ERROR
                    //we get error msg when there is illegal op code or client send Error
                if (((ErrorMsg) message).getErrCode() == 4) {
                    connections.send(connectionId, message);
                }else{
                    if (out != null) {
                        try {
                            path = fileDir;
                            path += "/" + fileName;
                            p = Paths.get(path);
                            Files.delete(p);
                            out.close();
                            out = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                    break;
                case 6://DIRQ
                    byte[] ans = new byte[0];
                    File folder = new File(fileDir);
                    File[] listOfFiles = folder.listFiles();
                    for (int i = 0; listOfFiles != null && i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile() && !listOfFiles[i].getName().endsWith(".TheMuszkal")) {
                            ans = addToByteArrPlus0Byte(ans, (listOfFiles[i].getName()).getBytes());
                        }
                    }
                    toSend.addAll(bytesToPackets(ans));
                    connections.send(connectionId, toSend.pollFirst());
                    break;
                case 7://LOGIN
                    if (connected.containsValue(((LOGRQMsg) message).getUserName()) || connected.containsKey(connectionId)) {
                        connections.send(connectionId, new ErrorMsg((short) 7, "Login username already connected"));
                    } else {
                        connected.put(connectionId, ((LOGRQMsg) message).getUserName());
                        connections.send(connectionId, new ACKMsg((short) 0));
                    }
                    break;
                case 8://DELETE
                    path = fileDir;
                    path += "/" + ((DELRQMsg) message).getFileName();
                    p = Paths.get(path);
                    if (InUse.contains(((DELRQMsg) message).getFileName())) {
                        connections.send(connectionId, new ErrorMsg((short) 2, "File Cannot Be Deleted"));
                    } else {
                        try {
                            if (Files.deleteIfExists(p)) {
                                connections.send(connectionId, new ACKMsg((short) 0));
                                process(new BCASTMsg(false, ((DELRQMsg) message).getFileName()));
                            } else {
                                connections.send(connectionId, new ErrorMsg((short) 1, "File Not Found"));
                            }
                        } catch (IOException e) {
                            connections.send(connectionId, new ErrorMsg((short) 2, "Cannot Delete File"));
                        }
                    }
                    break;
                case 9://BCAST
                    for (Map.Entry<Integer, String> entry : connected.entrySet()) {
                        connections.send(entry.getKey(), message);
                    }
                    break;
                case 10://DISCONNECT
                    connections.send(connectionId, new ACKMsg((short) 0));
                    connected.remove(connectionId);
                    break;
            }
        }

    }

    private List<DataMsg> bytesToPackets(byte[] ans) {
        LinkedList<DataMsg> toReturn = new LinkedList<>();
        short bNum = 1;
        int x = 512;  // chunk size
        byte[][] newArray = new byte[(ans.length / x) + 1][x];
        int len = ans.length;
        int counter = 0;
        for (int i = 0; i < len - x + 1; i += x) {
            newArray[counter++] = Arrays.copyOfRange(ans, i, i + x);
        }
        newArray[counter] = new byte[len % x];
        newArray[counter] = Arrays.copyOfRange(ans, len - len % x, len);
        for (byte[] b : newArray) {
            toReturn.add(new DataMsg((short) b.length, bNum++, b));
        }
        return toReturn;
    }

    private boolean isFileInFiles(String fileName) {
        path = fileDir;
        path += "/" + fileName;
        p = Paths.get(path);
        return Files.exists(p);
    }

    private byte[] addToByteArrPlus0Byte(byte[] toArr, byte[] fromArr) {
        byte[] ans = new byte[toArr.length + fromArr.length + 1];
        int ansPointer = 0;
        for (byte aToArr : toArr) {
            ans[ansPointer] = aToArr;
            ansPointer++;
        }
        for (byte aFromArr : fromArr) {
            ans[ansPointer] = aFromArr;
            ansPointer++;
        }
        ans[ansPointer] = '\0';
        return ans;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
