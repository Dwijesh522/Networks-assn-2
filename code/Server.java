import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
public class Server
{
///mode
//1 -> unencrypted
//2 -> encrypted
//3 -> encrypted with signatures
  public static void main(String argv[]) //throws Exception
  {
    try
    {
      int mode = Integer.parseInt(argv[0]);
      Hashtable<String, HashTableData> table = new Hashtable<String, HashTableData>();
      ServerSocket welcomeSocket_recv = new ServerSocket(6789); // Send port on server
      while(true)
      {
        Socket connectionSocket_recv = welcomeSocket_recv.accept();
        BufferedReader inFromClient_recv = new BufferedReader(new InputStreamReader(connectionSocket_recv.getInputStream()));
        DataOutputStream outToClient_recv = new DataOutputStream(connectionSocket_recv.getOutputStream());
        Serverthread recv_thread = new Serverthread(mode, table, connectionSocket_recv, inFromClient_recv, outToClient_recv);
        Thread r_thread = new Thread(recv_thread);
        r_thread.start();
      }
    }
    catch(Exception e12)
    {
      System.out.println("CATCHE_ERROR");
    }
  }
}
