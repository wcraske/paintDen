import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 62421;
    public PrintWriter out;
    public MessageListener messageListener;


    Client(MessageListener listener) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to the chat server!");
            messageListener = listener;

            // Setting up input and output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to handle incoming messages
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        messageListener.onMessageReceived(serverResponse);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
           
        } catch (IOException e) {
            e.printStackTrace();
        }

        
    }


    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public void sendMessage(String message){
        out.println(message);


    }

    public static void connection(){
            

    }
}