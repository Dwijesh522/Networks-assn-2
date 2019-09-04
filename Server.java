import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
import hashtabledata.HashTableData;
public class Server
{

  public static void main(String argv[]) //throws Exception
  {
    try
    {
      Hashtable<String, HashTableData> table = new Hashtable<String, HashTableData>();
      ServerSocket welcomeSocket_send = new ServerSocket(6789); // Send port on server
      ServerSocket welcomeSocket_recv = new ServerSocket(6790); // recieve port on server
      while(true)
      {
        Socket connectionSocket_send = welcomeSocket_send.accept();
        BufferedReader inFromClient_send = new BufferedReader(new InputStreamReader(connectionSocket_send.getInputStream()));
        DataOutputStream outToClient_send = new DataOutputStream(connectionSocket_send.getOutputStream());
        Serverthread send_thread = new Serverthread(table, connectionSocket_send, inFromClient_send, outToClient_send, true);
        Thread s_thread = new Thread(send_thread);
        s_thread.start();
        Socket connectionSocket_recv = welcomeSocket_recv.accept();
        BufferedReader inFromClient_recv = new BufferedReader(new InputStreamReader(connectionSocket_recv.getInputStream()));
        DataOutputStream outToClient_recv = new DataOutputStream(connectionSocket_recv.getOutputStream());
        Serverthread recv_thread = new Serverthread(table, connectionSocket_recv, inFromClient_recv, outToClient_recv, false);
        Thread r_thread = new Thread(recv_thread);
        r_thread.start();
      }
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
}
