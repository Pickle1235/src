import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * ChatServer
 * <p>
 * Server for chat
 *
 * @author Geon An, Arron Smith
 * @version November 26, 2018
 */
final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private ChatFilter filter;
    private SimpleDateFormat date = new SimpleDateFormat("HH:mm:ss");
    private static int anonNum = 1;

    private ChatServer(int port, String filter) {
        this.port = port;
        try {
            this.filter = new ChatFilter(filter);
        } catch (FileNotFoundException e) {
            System.out.println("Filter file was not found! Please correct the filepath and try again.");
            System.exit(0);
        }
    }

    private ChatServer(String filter) {
        this.port = 1500;
        try {
            this.filter = new ChatFilter(filter);
        } catch (FileNotFoundException e) {
            System.out.println("Filter file was not found! Please correct the filepath and try again.");
            System.exit(0);
        }
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            stuff:
            while (true) {
                System.out.println(date.format(new Date()) + " " + "Server waiting for Clients on port " + this.port + ".");
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                if (((ClientThread) r).username.equals("Anonymous")) {

                } else {
                    for (ClientThread client : clients) {
                        if (((ClientThread) r).username.equals(client.username)) {
                            ((ClientThread) r).writeMessage("Username already exists!\n" +
                                    "Please choose a unique username and try again.");
                            ((ClientThread) r).close(1);
                            System.out.println(date.format(new Date()) + " New user " + ((ClientThread) r).username +
                                    " denied for non-unique username.");
                            continue stuff;
                        }
                    }
                }
                clients.add((ClientThread) r);
                t.start();
                System.out.println(date.format(new Date()) + " " + ((ClientThread) r).username + " just connected.");
            }
        } catch (BindException e) {
            System.out.println("Server is already running.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized private boolean directMessage(String message, String username, String sender) {
        if (!sender.equals(username)) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).username.equals(username)) {
                    clients.get(i).writeMessage(date.format(new Date()) + " " + sender + " -> " + username + ": " + message);
                    System.out.println(date.format(new Date()) + " " + sender + " -> " + username + ": " + message);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        ChatServer server;
        if (args.length == 1) {
            server = new ChatServer(args[0]);
        } else if (args.length == 2) {
            try {
                server = new ChatServer(Integer.parseInt(args[0]), args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Incorrect arguments! Please input an integer for port argument.");
                return;
            }
        } else {
            System.out.println("Incorrect arguments. Please correct to one of the following formats:" +
                    "\n    Java ChatServer <words_to_filter.txt>" +
                    "\n    Java ChatServer <port_number> <words_to_filter.txt>");
            return;
        }
        server.start();
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client
            while (true) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                    if (cm.getMessageType() == 1) {
                        close(0);
                        return;
                    } else if (cm.getMessageType() == 0) {
                        String message = cm.getMessage();
                        message = filter.filter(message);
                        broadcast(username + ": " + message);
                    } else if (cm.getMessageType() == 2) {
                        if (directMessage(cm.getMessage(), cm.getUsername(), username)) {
                            writeMessage(date.format(new Date()) + " " + username + " -> " + cm.getUsername() + ": " + cm.getMessage());
                        } else {
                            writeMessage("User does not exist!");
                        }
                    } else if (cm.getMessageType() == 3) {
                        String message = "List: ";
                        String[] users = listUsers();
                        if (users.length == 0) {
                            writeMessage("No other users!");
                        } else {
                            for (String user : users) {
                                message += "\n" + user;
                            }
                            writeMessage(message);
                        }
                    }
                } catch (SocketException e) {
                    close(-1);
                    return;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean writeMessage(String msg) {
            if (this.socket.isConnected()) {
                try {
                    sOutput.writeObject(msg);
                } catch (Exception e) {
                    System.out.println("Whoopsie");
                    return true;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * Closes the inputs, outputs, and the socket.
         * Also calls remove(int id, int logout)
         *
         * @param logout Determines which type of disconnect occurred
         *               -1: unexpected
         *               0: logout
         *               1: non-unique username
         */
        private void close(int logout) {
            try {
                this.sOutput.close();
                this.sInput.close();
                this.socket.close();
                remove(this.id, logout);
            } catch (Exception e) {
                System.out.println("closed");
            }
        }

        public String[] listUsers() {
            String[] users = new String[clients.size() - 1];
            int i = 0;
            for (ClientThread user : clients) {
                if (this.id != user.id) {
                    users[i] = user.username;
                    i++;
                }
            }
            return users;
        }
    }

    private synchronized void broadcast(String message) {
        for (ClientThread client : clients) {
            client.writeMessage(date.format(new Date()) + " " + message);
        }
        System.out.println(date.format(new Date()) + " " + message);
    }

    /**
     * Removes the user from the clients list.
     *
     * @param id     The unique id used in the clients list.
     * @param logout Determines which type of disconnect occurred
     *               -1: unexpected
     *               0: logout
     */
    private synchronized void remove(int id, int logout) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).id == id) {
                switch (logout) {
                    case -1:
                        System.out.println(date.format(new Date()) + " " + "User " + clients.get(i).username +
                                " unexpectedly disconnected.");
                        clients.remove(i);
                        i--;
                        break;
                    case 0:
                        System.out.println(date.format(new Date()) + " " + "User " + clients.get(i).username +
                                " logged out.");
                        clients.remove(i);
                        i--;
                        break;
                }
            }
        }
    }
}
