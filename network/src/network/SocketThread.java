package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {

    private final SocketThreadListener listener;
    private final Socket socket;
    private DataOutputStream out;

    public SocketThread(SocketThreadListener listener,String name, Socket socket) {
       super(name);
        this.socket = socket;
        this.listener = listener;
        start();
    }


    @Override
    public void run() {
        try{
            listener.onSocketStart(this,socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this,socket);
            while (!isInterrupted()){
                String msg = in.readUTF(); //всегда ждем сообщений которые прилетят
                listener.onReceiveString(this,socket, msg); //сообщаем что мы получили листнеру
            }
        }catch (IOException e) {
            listener.onSocketException(this,e);
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
               listener.onSocketException(this,e);
            }
            listener.onSocketStop(this);
        }

    }
    //всегда готовы записать в поток ввода сокета
    public synchronized boolean sendMessage(String msg){
        try{
            out.writeUTF(msg);
            out.flush();
            return true;
        }catch (IOException e) {
            listener.onSocketException(this,e);
            close();
            return false;
        }
    }

    public synchronized void close(){
        interrupt();
        try{
            socket.close();
        }catch (IOException e){
            listener.onSocketException(this,e);
        }
    }
}
