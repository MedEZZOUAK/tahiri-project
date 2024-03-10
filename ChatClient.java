import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ChatClient{

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static ObjectOutputStream os = null;
    // The input stream
    private static ObjectInputStream is = null;

    private static Scanner scan = null;
    
    private static String username = null;
    /*
     * To start the ChatClient in console mode use one of the following command
     * > java ChatClient 
     */
    public static void main(String[] args) {
        // ask the user for the server's IP address ,if the user does not provide the IP address then the default IP address is localhost
        System.out.println("Enter the IP address of the server: ");
        Scanner sc = new Scanner(System.in);
        String host = sc.nextLine();
        // ask the user for the port number of the server, if the user does not provide the port number then the default port number is 5000
        System.out.println("Enter the port number of the server: ");
        String port = sc.nextLine();
        // ask the user for the username
        System.out.println("Enter your username: ");
        username = sc.next();
        if (host.length() == 0) {
            host = "localhost";
        }
        if (port.length() == 0) {
            port = "5000";
        }
        int portNumber = Integer.parseInt(port);
        try 
        {
            clientSocket = new Socket(host, portNumber);
            scan = new Scanner(System.in);
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            is = new ObjectInputStream(clientSocket.getInputStream());
            String msg = "Connection accepted " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
            display(msg);
        } 
        catch (UnknownHostException e) 
        {
            
            System.err.println("Don't know about host " + host);
        } 
        catch (IOException e) 
        {
            System.err.println("Couldn't get I/O for the connection to the host " + host+":"+port+" "+e.getMessage());
        }

    /*
     * If everything has been initialized then we want to write some data to the
     * socket we have opened a connection to on the port portNumber.
     */
        if (clientSocket != null && os != null && is != null) {
            try 
            {
                start();//reads and prints on client side
                
                System.out.println("\nHello.! Welcome to the chatroom.");
                System.out.println("Instructions:");
                // brodcast message, list of users, logout,send message to a user using their username or id
                System.out.println("1. To send a private message to a user,  @username message and press enter.");
                System.out.println("2. To send a message to all users, type the message and press enter.");
                System.out.println("3. To logout, type SIGNOUT and press enter.");
                System.out.println("4. To check who are present in chatroom, type GETUSERS and press enter.");
                while(true) {
                    System.out.print(">");
                    // read message from user
                    String msg = scan.nextLine();
                    // logout if message is SIGNOUT
                    if(msg.equalsIgnoreCase("SIGNOUT")) 
                    {
                        sendMessage(new Message(MessageType.SIGNOUT, ""));
                        Thread.sleep(2000);
                        break;
                    }
                    // message to check who are present in chatroom
                    else if(msg.equalsIgnoreCase("GETUSERS")) 
                    {
                        sendMessage(new Message(MessageType.GETUSERS, ""));               
                    }
                    // regular text message
                    else if(msg.contains("@")) 
                    {
                        sendMessage(new Message(MessageType.privateMessage, msg));
                        System.out.println("private message sent");
                    }
                    else 
                    {
                        sendMessage(new Message(MessageType.MESSAGE, msg));
                    }
                }
                /*
                 * Close the output stream, close the input stream, close the socket.
                 */
                CloseAll();
            } 
            catch (Exception e) 
            {
                System.err.println("IOException:  " + e);
            }
        }
    }

    /* To start the chat client
     */
    public static void start() {        
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be Message objects
        try
        {
            os.writeObject(username);
            boolean NameIsNotOk = true;
            while(NameIsNotOk)
            {
                // read the message form the input datastream
                String msg = (String) is.readObject();
                // print the message
                System.out.println(msg);
                System.out.print("> ");
                if(msg.equals("OK NAME"))
                {
                    NameIsNotOk = false;
                    username = msg;
                }
                else
                {
                    // read the new name from user
                    String newName = scan.nextLine();
                    os.writeObject(newName);
                }    
            }    
            
            
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            CloseAll();
        }
        catch(ClassNotFoundException e) {
        }
        // creates the Thread to listen from the server 
        new MessageListener(is).start();        
    }

    /*
     * To send a message to the console
     */
    private static void display(String msg) {
        System.out.println(msg);
    }
    
    /*
     * To send a message to the server
     */
    static void sendMessage(Message msg) {
        try 
        {
            os.writeObject(msg);
        }
        catch(IOException e) 
        {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect
     */
    static private void CloseAll() {
        try 
        { 
            if(is != null) is.close();
            if(os != null) os.close();
            if(scan != null) scan.close();
            if(clientSocket != null) clientSocket.close();
        }
        catch(Exception e) {}            
    }
}

/*
 * a class that waits for the message from the server
 */
class MessageListener extends Thread {
    private ObjectInputStream is;

    MessageListener(ObjectInputStream is)
    {
        this.is = is;
    }

    public void run() {
        while(true) {
            try {
                // read the message form the input datastream
                String msg = (String) is.readObject();
                // print the message
                System.out.println(msg);
                System.out.print("> ");
            }
            catch(IOException IOE) {
                System.out.println(" *** " + "Server has closed the connection: " + IOE + " *** ");
                break;
            }
            catch(ClassNotFoundException e) {
            }
        }
    }
}