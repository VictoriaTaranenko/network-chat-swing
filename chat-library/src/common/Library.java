package common;

public class Library {

    /*
     * auth_request login password    запрос авторизации
     * auth_accept nickname           подтверждение авторизации
     * auth_error                     ошибка авторизации
     * broadcast msg                   сообщения которые будут посылаться всем
     * msg_format_error msg
     * user_list±user1±user2±user3±....
     * auth_changenick                запрос о смене ника
     * */
    public static final String DELIMITER = "±";
    public static final String AUTH_REQUEST = "/auth_request"; //запрос авторизации
    public static final String AUTH_ACCEPT = "/auth_accept";   // подтверждение авторизации
    public static final String AUTH_DENIED = "/auth_denied";  //отказ в авторизации
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";
    //если мы вдруг не поняли, что за сообщение и не смогли разобрать
    public static final String TYPE_BROADCAST = "/bcast";  //сообщение от сервера, и сообщение сервера(которое послал клиент всем пользователям)
    //то есть сообщение которое будет посылаться всем
    public static final String TYPE_BCAST_CLIENT = "/client_msg";//клиент послал для всех смс
    public static final String USER_LIST = "/user_list";
    public static final String NEW_NICKNAME = "/auth_changenick";


    public static String getTypeBcastClient(String msg){
        return TYPE_BCAST_CLIENT + DELIMITER + msg;
    }



    public static String getUserList(String users){
        return USER_LIST + DELIMITER + users;
    }

    //сокет авторизовался
    public static String getAuthRequest(String login, String password){
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }
    //сокет получил подтверждение авторизации
    public static String getAuthAccept(String nickname){
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getChengeNewNickName(String nickname){
        return NEW_NICKNAME + DELIMITER + nickname;
    }

    public static String getAuthDenied(){
        return AUTH_DENIED;
    }

    public static String getMsgFormatError(String message){
        return  MSG_FORMAT_ERROR + DELIMITER + message;
    }

    public static String getTypeBroadcast(String src, String message){
        return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() + DELIMITER + src + DELIMITER + message;
    }


}