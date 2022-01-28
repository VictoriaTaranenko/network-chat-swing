package chat_server.core;

import common.Library;
import network.SocketThread;
import network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {

    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnecting; //состояние реконекта

    public boolean isReconnecting(){//гетер реконетка
        return isReconnecting;
    }
    //сокет реконектится,и если мы реконектимся нам не надо посылать смс о дисконекте
    void reconnect(){
        isReconnecting = true;//мы реконектимся для отображения сообщений в других частях
        close();  //и рвем себя
    }

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isAuthorized(){
        return isAuthorized;
    }

    public void authAccept(String nickname){
        isAuthorized = true;
        this.nickname = nickname;
        sendMessage(Library.getAuthAccept(nickname));//шлем сообщение клиенту об авторизации
    }

    public void  authFail(){
        sendMessage(Library.getAuthDenied());
        close();  //закрываем сокет с нашей стороны
    }
    public void msgFormatError(String msg){
        sendMessage(Library.getMsgFormatError(msg));
        close();
    }




}
