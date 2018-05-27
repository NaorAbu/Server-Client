package bgu.spl171.net.api.bidi;

import bgu.spl171.net.srv.ConnectionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {

    private Map<Integer, ConnectionHandler<T>> connections;
    private AtomicInteger idCount;

    public ConnectionsImpl() {
        connections = new HashMap<>();
        idCount = new AtomicInteger(1);

    }

    public void register(ConnectionHandler<T> handler) {
        connections.put(idCount.getAndIncrement(), handler);
    }

    public int getID(ConnectionHandler<T> handler){
        for (Map.Entry<Integer, ConnectionHandler<T>> entry : connections.entrySet()) {
            if(handler.equals(entry.getValue())){
                return entry.getKey();
            }
        }
        return 0;
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (connections.containsKey(connectionId)) {
            connections.get(connectionId).send(msg);
            return true;
        } else return false;
    }

    @Override
    public void broadcast(T msg) {
        for (Map.Entry<Integer, ConnectionHandler<T>> entry : connections.entrySet()) {
            entry.getValue().send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        if (connections.containsKey(connectionId)) {
            ConnectionHandler cht = connections.get(connectionId);
            try {
                cht.close();
                connections.remove(connectionId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
