import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * A chat server that delivers public and private messages.
*/
public class ChatServer {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;
    //An ArrayList to keep list of clients
    private static ArrayList<ClientThread> clients;
    // to display time
    private static SimpleDateFormat sdf;

    public static void main(String args[]) {
        clients = new ArrayList<>();
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // The default port number.
        int portNumber = 5000 ;
        
        /*
         * Open a server socket on the portNumber (default 5000). Note that we can
         * not choose a port less than 1023 if we are not privileged users (root).
         */
        try 
        {
            serverSocket = new ServerSocket(portNumber);
        } 
        catch (IOException e) 
        {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         * thread.
         */
        boolean ContinueRunningServer = true;
        while (ContinueRunningServer)
        {
            try 
            {
                display("Server waiting for Clients on port " + portNumber + ".");
                clientSocket = serverSocket.accept();
                // break if server stoped
                if(!ContinueRunningServer)
                    break;
                
                // if client is connected, create its thread
                ClientThread t = new ClientThread(clientSocket, clients);
                
                //add this client to arraylist
                clients.add(t);
                
                t.start(); 
            } 
            catch (IOException e) 
            {
                String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
                display(msg);
            }
        }

        // try to stop the server
        try 
        {
            serverSocket.close();
            for(ClientThread tc : clients) 
            {
                try 
                {
                    tc.CloseAll();
                }
                catch(Exception e)
                {
                    display("Exception closing " + e);          
                }
            }
        }
        catch(Exception e)
        {
            display("Exception closing the server and clients: " + e);
        }
    }

    // Display a message on console
    private static void display(String msg){
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class ClientThread extends Thread {

    private String clientName = null;
    private ObjectInputStream  is = null;
    private ObjectOutputStream  os = null;
    private Socket clientSocket = null;
    private final ArrayList<ClientThread> threads;
    // timestamp
    String date;
    // message object to recieve message and its type
    Message msg;
    // to display time
    private SimpleDateFormat sdf;

    public ClientThread(Socket clientSocket,ArrayList<ClientThread> threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        sdf = new SimpleDateFormat("HH:mm:ss");
        /*
         * Create input and output streams for this client.
         */
        try
        {
            is = new ObjectInputStream(clientSocket.getInputStream());
            os = new ObjectOutputStream(clientSocket.getOutputStream());
        }
        catch (IOException e) 
        {
            display("Exception creating new Input/output Streams: " + e);
            return;
        }

        date = new Date().toString();            
    }

    // Display a message on console
    private void display(String msg){
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    //Close everything
    public void CloseAll()
    {
        try 
        {
            if(is != null) is.close();
            if(os != null) os.close();
            if(clientSocket != null) clientSocket.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public String GetClientName()
    {
        return clientName;
    }

    public void run() {
        ArrayList<ClientThread> threads = this.threads;
        try 
        {
            String name = "";
            boolean nameCheckFailed=true;
            while (nameCheckFailed) 
            {
                try
                {
                    name = (String) is.readObject();
                }
                catch (ClassNotFoundException e)
                {
                }
                if (name.indexOf('@') == -1 && name.indexOf(' ') == -1) 
                {
                    boolean ClientsWithSameNameExists=false;
                    for(ClientThread ct: threads)
                    {
                        if(ct != this)
                        {
                            if(name.equals(ct.GetClientName()))
                            {
                                os.writeObject("The name " + name + " is already taken. Please enter a different username.\n");
                                ClientsWithSameNameExists = true;
                                break;              
                            }   
                        }   
                    }
                    nameCheckFailed = ClientsWithSameNameExists;
                } 
                else 
                {
                    os.writeObject("The name should not contain '@' character or space. Please enter a different username.\n");
                }
            }
            os.writeObject("OK NAME");
            clientName=name;

        /* Welcome the new the client. */
        os.writeObject("Welcome " + clientName
                    + " to our chat room.\nTo leave enter \"signout\" in a new line. To send private method use @user message");
        broadcast(" *** A new user " + clientName + " has joined the chat room." + " *** ");    
        
      /* Start the conversation. */
      boolean ContinueConversation=true;
            while (ContinueConversation) 
            {
                try
                {
                    msg = (Message) is.readObject();
                }
                catch (IOException e) 
                {
                    display(clientName + " Exception reading Streams: " + e);
                    break;              
                }
                catch (ClassNotFoundException e)
                {
                }
                // get the message from the Message object received
                String message = msg.getMessage();

                // different actions based on type message
                // different actions based on type message
                switch(msg.getType()) {

                case MESSAGE:
                    // broadcast message
                    boolean confirmation =  broadcast(clientName + " : " + message);
                    if(confirmation==false){
                        String msg = sdf.format(new Date()) + " *** " + "Sorry. No such user exists." + " *** " + "\n";
                        writeMsg(msg);
                    }
                    break;
                    
                case SIGNOUT:
                    display(clientName + " disconnected with a SIGNOUT message.");
                    ContinueConversation = false;
                    os.writeObject("*** Bye " + clientName + " ***");
                    break;
                case GETUSERS:
                    os.writeObject("List of the users connected at " + sdf.format(new Date()) + "\n");
                    // send list of active clients
                    for(ClientThread ct : threads) 
                    {
                        if((ct != this) && (ct.GetClientName() != null))
                        {
                            os.writeObject(" @@@ " + ct.GetClientName() + " since " + ct.date + "\n");  
                        }   
                    }
                    break;
                case privateMessage:
                    // send private message to mentioned username
                    String[] w = message.split(" ",2);
                    boolean privateConfirmation = false;
                    for(ClientThread ct : threads) 
                    {
                        if(ct.GetClientName().equals(w[0]))
                        {
                            privateConfirmation = true;
                            if(ct != this && ct.GetClientName() != null) 
                            {
                                ct.os.writeObject(clientName + " : " + w[1]);
                                break;
                            }
                        }
                    }
                    if(privateConfirmation==false)
                    {
                        os.writeObject(" *** " + "Sorry. No such user exists." + " *** ");
                    }
                    break;

                }   
            }
            broadcast(" *** User " + clientName + " is leaving the chat room." + " *** ");

      /*
       * Clean up. Set the current thread variable to null so that a new client
       * could be accepted by the server.
       */
            threads.remove(this);
      /*
       * Close the output stream, close the input stream, close the socket.
       */
        CloseAll();
        } catch (IOException e) {
        }
    }

    // to broadcast a message to all Clients
    private synchronized boolean broadcast(String message) {
        // add timestamp to the message
        String time = sdf.format(new Date());
        
        // to check if message is private i.e. client to client message
        String[] w = message.split(" ",3);
        
        boolean isPrivate = false;
        if(w[1].charAt(0)=='@') 
            isPrivate=true;
        
        
        // if private message, send message to mentioned username only
        if(isPrivate==true)
        {
            String tocheck=w[1].substring(1, w[1].length());
            
            message=w[0]+w[2];
            String messageLf = time + " (Private Message) " + message + "\n";
            boolean found=false;
            // we loop in reverse order to find the mentioned username
            for(int y=threads.size(); --y>=0;)
            {
                ClientThread ct1=threads.get(y);
                String check=ct1.GetClientName();
                if(check != null && check.equals(tocheck))
                {
                    // try to write to the Client if it fails remove it from the list
                    if(!ct1.writeMsg(messageLf)) {
                        threads.remove(y);
                        display("Disconnected Client " + ct1.GetClientName() + " removed from list.");
                    }
                    // username found and delivered the message
                    found=true;
                    // display message
                    System.out.print(messageLf);
                    break;
                }
                
                
                
            }
            // mentioned user not found, return false
            if(found!=true)
            {
                return false; 
            }
        }
        // if message is a broadcast message
        else
        {
            String messageLf = time + " " + message + "\n";
            // display message
            System.out.print(messageLf);
            
            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for(int i = threads.size(); --i >= 0;) {
                ClientThread ct = threads.get(i);
                // try to write to the Client if it fails remove it from the list
                if(ct.GetClientName() != null)
                {
                    if(!ct.writeMsg(messageLf)) 
                    {
                        threads.remove(i);
                        display("Disconnected Client " + ct.GetClientName() + " removed from list.");
                    }   
                }                   
            }
        }
        return true;
        
        
    }

    // write a String to the Client output stream
    private boolean writeMsg(String msg) {
        // if Client is still connected send the message to it
        if(!clientSocket.isConnected()) {
            CloseAll();
            return false;
        }
        // write the message to the stream
        try {
            os.writeObject(msg);
        }
        // if an error occurs, do not abort just inform the user
        catch(IOException e) {
            display(" *** " + "Error sending message to " + clientName + " *** ");
            display(e.toString());
        }
        return true;
    }

    // if client sent SIGNOUT message to exit
    synchronized void remove(int id) {
        
        String disconnectedClient = "";
        // scan the array list until we found the Id
        for(int i = 0; i < threads.size(); ++i) {
            ClientThread ct = threads.get(i);
            // if found remove it
            if(ct.GetClientName().equals(GetClientName())) {
                disconnectedClient = ct.GetClientName();
                threads.remove(i);
                break;
            }
        }
        broadcast(" *** " + disconnectedClient + " has left the chat room." + " *** ");
    }
}
