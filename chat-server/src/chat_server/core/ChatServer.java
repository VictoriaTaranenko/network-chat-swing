package chat_server.core;


import common.Library;
import network.ServerSocketThread;
import network.ServerSocketThreadListener;
import network.SocketThread;
import network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private final ChatServerListener listener;
    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private Vector<SocketThread> clients = new Vector<>();

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }


    public void start(int port) {
        if (server != null && server.isAlive())
            putLog("Server already started");
        else
            server = new ServerSocketThread(this, "Server", port, 2000);
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            putLog("Server is not running");
        } else {
            server.interrupt();
        }
    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) + Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server methods
     */
    //при стопе этот тред интерапт(прерывается), перестаем принимать новые сокеты
    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server thread started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        SqlClient.disconnect();
        for (int i = 0; i < clients.size(); i++) {//дропнули всех клиентов
            clients.get(i).close();
        }
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Server timeout");
    }

    @Override//подсоединяемся к клиенту и прилетает готовый сокет сюда
    //и мы на его основе создаем новый поток(оборачиваем сокет в поток) оставив только взаимосвязь
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        String name = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);//слушатель,имя, и сам сокет который нужно обернуть в поток
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket methods
     */

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");
    }

    @Override//удаляем клиента
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);
        //мы пишем о дисконекте клиентов(если они авторизованы и реконект
        if (client.isAuthorized() && !client.isReconnecting()) {//сообщили всем что клиент дисконектнулся
            sendToAuthClients(Library.getTypeBroadcast("Server", client.getNickname() + " disconnected"));
        }
        sendToAuthClients(Library.getUserList(getUsers()));
    }

    @Override//добавляем клиента
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        clients.add(thread);
    }

    @Override//метод где получаем сообщение - тут нам могут прислать сообщение кто авторизован и кто не авторизован
    //то есть тут мы обрабатываем сообщение с Клиента (private void sendMessage())
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized())//если не авторизован
            handleAuthMessage(client, msg);  //если авторизован - мы должны послать всем остальным, ну или если сообщение приватное то кому то
        else
            handleNonAuthMessage(client, msg);//сообщения не авторизованых - мы его авторизуем
    }

    private void handleNonAuthMessage(ClientThread client, String msg) {
        String arr[] = msg.split(Library.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Library.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNickname(login, password);
        if (nickname == null) {
            putLog("Invalid login attempt: " + login);
            client.authFail();
            return;
        } else {
            ClientThread oldClient = findClientByNickname(nickname);
            client.authAccept(nickname);
            if (oldClient == null) {//если мы не нашли старого то говорим - кто то к нам подконектился
                sendToAuthClients(Library.getTypeBroadcast("Server ", nickname + " connected"));
            } else {
                oldClient.reconnect();
                clients.remove(oldClient);
            }
        }

        sendToAuthClients(Library.getUserList(getUsers()));//шлем все авторизованым сообщение со списком

    }

    //сообще авторизован польз
    private void handleAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.TYPE_BCAST_CLIENT:
                sendToAuthClients(Library.getTypeBroadcast(client.getNickname(), arr[1]));
                break;
            default:           //если прилетает смс не подходящее к никаким типам сообщений
                client.sendMessage(Library.getMsgFormatError(msg));

        }
    }

    //можем посылать всем авторизованным клиентам(только авторизованым)
    private void sendToAuthClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMessage(msg);
        }
    }

    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

    private String getUsers() {//сообщение со списком обновленное
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Library.DELIMITER);
        }
        return sb.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname)) return client;
        }
        return null;
    }

}