package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerSocketThread extends Thread {

    private int port;
    private int timeout;
    ServerSocketThreadListener listener;

    public ServerSocketThread(ServerSocketThreadListener listener, String name, int port, int timeout){
        super(name);
        this.port = port;
        this.timeout = timeout;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        listener.onServerStart(this);
        try(ServerSocket server = new ServerSocket(port)){//создаем обьект который слушает порт при помощи accept()
            server.setSoTimeout(timeout);
            listener.onServerSocketCreated(this,server);
            while (!isInterrupted()){
                Socket socket;
                try{
                    socket = server.accept();//при срабатывании accept(соединении сокетов) accept()-создает сокет
                }catch (SocketTimeoutException e){// сокет клиента запрашивает потоки ввода вывода у этого создавшегося сокета
                    listener.onServerTimeout(this,server);
                    continue;
                }
                listener.onSocketAccepted(this,server,socket);//у меня у этого сервера вот этот сокет принялся(подключился)
            }
        }catch (IOException e){
            listener.onServerException(this,e);
        }finally {
            listener.onServerStop(this);
        }
    }
}
