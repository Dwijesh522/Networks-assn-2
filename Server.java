import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

class Serverthread extends Thread
{
  void run() // public
  {
  }
}

public Server
{
  public static void main(String argv[])
  {
    ServerSocket welcomeSocket_send = new ServerSocket(6789); // Send port on server
    ServerSocket welcomeSocket_recv = new ServerSocket(6790); // recieve port on server
    while(true)
    {
      Socket connectionSocket = welcomeSocket.accept();
      BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
      DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
      String line = inFromClient.readLine();
      String[] line_split = line.split(" ");
      if(line_split[0].equals("REGISTER"))
      {
        String username = line_split[2];
        inFromClient.readLine();
        String pubKey = inFromClient.readLine();
        if(line_split[1].equals("TOSEND"))
        {
        }
        else if(line_split[1].equals("TORECV")
        {

        }
        else
        {
          error101();
        }
      }
      else
      {
        error101();
      }
  }

  public void error100()
  {
    String error_message = "ERROR 101 No user registered\n\n";
    outToClient.writeBytes(error_message);
  }

  public void error101()
  {
    String error_message = "ERROR 100 Malformed username\n\n";
    outToClient.writeBytes(error_message);
  }

  public void error102()
  {
    String error_message = "ERROR 102 Unable to send\n\n";
    outToClient.writeBytes(error_message);
  }

  public void error103()
  {
    String error_message = "ERROR 103 Header incomplete\n\n";
    outToClient.write(error_message);
  }
  public bool check_username(String str)
  {
    int len = str.length();
    for(int i=0; i<len; i++)
    {
      bool temp_bool = false;
      int temp = (int)str[i];
      if(i<=57 && i>= 48)
        temp_bool = true;
      else if(i<= 90 && i>= 65)
        temp_bool = true;
      else if(i<= 97 && i>= 122)
        temp_bool = true;
      else
        temp_bool = false;
      if(!temp_bool) return false;
    }
    return true;
  }

}
