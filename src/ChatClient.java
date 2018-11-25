import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * ChatClient
 * <p>
 * Client of chat
 *
 * @author Geon An, Arron Smith
 * @version November 26, 2018
 */
final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private static boolean logout = false;

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    private ChatClient(int port, String username) {
        this("localhost", port, username);
    }

    private ChatClient(String username) {
        this("localhost", 1500, username);
    }

    private ChatClient() {
        this("localhost", 1500, "Anonymous");
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
            System.out.println("Connection accepted " + socket.getLocalSocketAddress());
        } catch (IOException e) {
            System.out.println("Server not found.");
            return false;
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            System.out.println("Server not connected!");
            //e.printStackTrace();
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults
        Scanner scanner = new Scanner(System.in);
        ChatClient client;
        switch (args.length) {
            case 0:
                client = new ChatClient();
                break;
            case 1:
                client = new ChatClient(args[0]);
                break;
            case 2:
                client = new ChatClient(Integer.parseInt(args[0]), args[1]);
                break;
            case 3:
                client = new ChatClient(args[0], Integer.parseInt(args[1]), args[2]);
                break;
            default:
                client = null;
                System.out.println("Incorrect arguments. Please correct to one of the following formats:" +
                        "\n    Java ChatClient" +
                        "\n    Java ChatClient <username>" +
                        "\n    Java ChatClient <username> <portNumber>" +
                        "\n    Java ChatClient <username> <portNumber> <serverAddress>");
                return;
        }

        // Create your client and start it
        //client = new ChatClient("localhost", 1500, "CS 180 Student");
        if (!client.start()) {
            return;
        }


        // Send an empty message to the server
        //client.sendMessage(new ChatMessage(0, ""));


        while (true) {
            ChatMessage message;
            String text = scanner.nextLine();
            String username;
            String[] words = text.split(" ");
            String send = "";
            int stop;
            if (text.indexOf(' ') != -1) {
                stop = text.indexOf(' ');
            } else {
                stop = text.length();
            }

            if (text.equals("")) {
                System.out.println("You cannot send an empty message!");
            } else if (text.charAt(0) == '/') {
                switch (text.substring(0, stop)) {
                    case "/logout":
                        if (words.length != 1) {
                            System.out.println("Incorrect Arguments. Follow the format: /logout");
                        } else {
                            logout = true;
                            message = new ChatMessage(1, text);
                            client.sendMessage(message);
                            try {
                                client.sOutput.close();
                                client.sInput.close();
                                client.socket.close();
                            } catch (Exception e) {
                                System.out.println("Whoopsies. Something went wrong!");
                            }
                        }
                        return;
                    case "/msg":
                        if (words.length < 3) {
                            System.out.println("Incorrect Arguments. Follow the format: /msg <username> <message>");
                        } else {
                            for (int i = 2; i < words.length - 1; i++) {
                                send += words[i] + " ";
                            }
                            send += words[words.length - 1];
                            username = words[1];
                            if (username.equals("Anonymous")) {
                                System.out.println("You cannot send a direct message to Anonymous users.");
                            } else if (client.username.equals(username)) {
                                System.out.println("You cannot send a direct message to yourself!");
                            } else {
                                message = new ChatMessage(2, username, send);
                                client.sendMessage(message);
                            }
                        }
                        break;
                    case "/help":
                        if (words.length != 1) {
                            System.out.println("Incorrect Arguments. Follow the format: /help");
                        } else {
                            System.out.println("List of commands: \n" +
                                    "/help\n" +
                                    "/list\n" +
                                    "/logout\n" +
                                    "/msg <username> <message>");
                        }
                        break;
                    case "/list":
                        if (words.length != 1) {
                            System.out.println("Incorrect Arguments. Follow the format: /list");
                        } else {
                            message = new ChatMessage(3, " ");
                            client.sendMessage(message);
                        }
                        break;
                    default:
                        System.out.println("Unknown command!");
                        System.out.println("List of commands: \n" +
                                "/help\n" +
                                "/list\n" +
                                "/logout\n" +
                                "/msg <username> <message>");
                }
            } else {
                message = new ChatMessage(0, text);

                client.sendMessage(message);
            }
        }
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                }
            } catch (SocketException e) {
                if (logout) {
                    System.out.println("Logout successful.");
                } else {
                    System.out.println("Server connection lost.");
                    System.exit(0);
                }
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("Disconnected from server.");
                System.exit(0);
            } catch (ClassNotFoundException e) {
                System.out.println("Whoops! Something went wrong!");
            }
        }
    }
}
//TEST