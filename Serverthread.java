import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
import hashtabledata.HashTableData;
public class Serverthread implements Runnable
{
  Socket connectionSocket;
  BufferedReader inFromClient;
  DataOutputStream outToClient;
  boolean send_thread;
  Hashtable < String, HashTableData> table;
  public Serverthread(Hashtable < String, HashTableData> table, Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient, boolean send_thread)
  {
    this.table = table;
    this.connectionSocket = connectionSocket;
    this.inFromClient = inFromClient;
    this.outToClient = outToClient;
    this.send_thread = send_thread;
  }
  public void run() // public
  {
    try
    {
      if(send_thread)
      {
        String line;
        String[] line_split;
        line = inFromClient.readLine();
        line_split = line.split(" ");
        if(line_split[0].equals("REGISTER"))
        {
          String reg_username = line_split[2];
          if(!check_username(reg_username))
          {
            outToClient.writeBytes("ERROR 100 Malformed username\n\n");
            connectionSocket.close();
            return;
          }
          byte[] reg_pubkey = Base64.getDecoder().decode(inFromClient.readLine());
          inFromClient.readLine(); //In order to parse the extra new line character added at the end of message
          //WRITING TO HASHTABLE INCOMPLETE
          if(line_split[1].equals("TOSEND"))
          {
            if(table.containsKey(reg_username))
            {
              HashTableData temp_data = table.get(reg_username);
              if(temp_data.get_send_socket() != null)
              {
                outToClient.writeBytes("ERROR 105 username already used\n\n");
                connectionSocket.close();
                table.remove(reg_username);
                return;
              }
              else
              {
                temp_data.put_send_socket(connectionSocket);
              }
            }
            else
            {
              HashTableData temp = new HashTableData(reg_pubkey, connectionSocket, true);
              table.put(reg_username, temp);
            }
            outToClient.writeBytes("REGISTERED TOSEND "+reg_username+'\n'+'\n');
            while(true)
            {
              line = inFromClient.readLine();
              while(line != null)
              {
                line = inFromClient.readLine();
              }
              line_split = line.split(" ");
              if(line_split[0].equals("GET_PUBLICKEY"))
              {
                String req_username = line_split[1];
                String encoded_pubkey = fetch_publickey(req_username);
                if(encoded_pubkey == null)
                {
                  outToClient.writeBytes("ERROR 104 recepient user not registered\n\n");
                  continue;
                }
                inFromClient.readLine(); // end GET_PUBLICKEY message
                outToClient.writeBytes("OK "+encoded_pubkey+'\n'+'\n');
                line = inFromClient.readLine();
                if(line.equals("RECVD_KEY"))
                inFromClient.readLine(); // end RECVD_KEY msg
                line = inFromClient.readLine(); // to read the next input line from client
                line_split = line.split(" ");
              }
              //NO ERROR MESSAGE IS SENT WHEN THE SENDER CLIENT DOESN'T ASK FOR PUBLIC KEY
              /*
              else
              {
                outToClient.writeBytes("generic error\n\n");
                connectionSocket.close();
                table.remove(reg_username);
                return;
              }
              */
              if(line_split[0].equals("SEND"))
              {
                String recepient_username;
                int message_length, signature_length;
                try
                {
                  recepient_username = line_split[1];
                  line = inFromClient.readLine();
                  line_split = line.split(" ");
                  if(line_split[0].equals("Message_length:"))
                    message_length = Integer.parseInt(line_split[1]);
                  else
                  {
                    outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                    System.out.println("Message discarded");
                    continue;
                  }
                  line = inFromClient.readLine();
                  line_split = line.split(" ");
                  if(line_split[0].equals("Signature_length:"))
                    signature_length = Integer.parseInt(line_split[1]);
                  else
                  {
                    outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                    System.out.println("Message discarded");
                    continue;
                  }
                }
                catch(NumberFormatException e)
                {
                  outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                  System.out.println("Message discarded");
                  continue;
                }
                inFromClient.readLine(); //end of headers
                String message = inFromClient.readLine().substring(0,message_length);
                inFromClient.readLine(); // extra \n character
                String signature = inFromClient.readLine().substring(0,signature_length);
                inFromClient.readLine(); //end \n character
                System.out.println("Message received at server. Beginning to forward");
                //forward(message, signature, recepient_username, message_length, signature_length, reg_username);
                Socket comm_socket = table.get(recepient_username).get_recv_socket();
                BufferedReader inFromRecpt = new BufferedReader(new InputStreamReader(comm_socket.getInputStream()));
                DataOutputStream outToRecpt = new DataOutputStream(comm_socket.getOutputStream());
                outToRecpt.writeBytes("FORWARD "+reg_username+'\n'+"Message_length: "+message_length+'\n'+
                                      "Signature_length: "+signature_length + '\n' +'\n' + message + '\n' + '\n'
                                      + signature + '\n' + '\n');
                line = inFromRecpt.readLine();
                line_split = line.split(" ");
                if(line_split[0].equals("GET_PUBLICKEY"))
                {
                  String req_username = line_split[1];
                  String encoded_pubkey = fetch_publickey(req_username);
                  if(encoded_pubkey == null)
                  {
                    outToRecpt.writeBytes("ERROR 104 recepient user not registered\n\n");
                    outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                    continue;
                  }
                  inFromRecpt.readLine(); // end GET_PUBLICKEY message
                  outToRecpt.writeBytes("OK "+encoded_pubkey+'\n'+'\n');
                  line = inFromRecpt.readLine();
                  if(line.equals("RECVD_KEY"))
                  inFromRecpt.readLine(); // end RECVD_KEY msg
                  System.out.println("Message forwarded succesfully");
                  //read next message
                  line = inFromRecpt.readLine();
                  line_split = line.split(" ");

                }
                else if(line.equals("ERROR 103 Header incomplete"))
                {
                  outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                  inFromRecpt.readLine(); // end error message
                  //read next message
                  line = inFromRecpt.readLine();
                  line_split = line.split(" ");
                  continue;
                }
                if(line_split[0].equals("RECEIVED") && line_split[1].equals(reg_username))
                {
                  outToClient.writeBytes("SENT "+recepient_username+'\n'+'\n');
                  System.out.println("Message sent succesfully");
                }
                else
                {
                  outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                  continue;
                }
              }
              else
              {
                outToClient.writeBytes("generic error\n\n");
                continue;
              }
            }
          }
          else outToClient.writeBytes("ERROR 101 No user registered\n\n");
        }
        else outToClient.writeBytes("ERROR 101 No user registered\n\n");
      }
      else
      {
        String line = inFromClient.readLine();
        String[] line_split = line.split(" ");
        if(line_split[0].equals("REGISTER"))
        {
          String reg_username = line_split[2];
          if(!check_username(reg_username)) outToClient.writeBytes("ERROR 100 Malformed username\n\n");
          byte[] reg_pubkey = Base64.getDecoder().decode(inFromClient.readLine());
          inFromClient.readLine(); //In order to parse the extra new line character added at the end of message
          if(line_split[1].equals("TORECV"))
          {
            //WRITING TO HASHTABLE
            if(table.containsKey(reg_username))
            {
              HashTableData temp_data = table.get(reg_username);
              if(temp_data.get_recv_socket() != null)
              {
                outToClient.writeBytes("ERROR 105 username already used\n\n");
                connectionSocket.close();
                table.remove(reg_username);
                return;
              }
              else
              {
                temp_data.put_recv_socket(connectionSocket);
              }
            }
            else
            {
              HashTableData temp = new HashTableData(reg_pubkey, connectionSocket, false);
              table.put(reg_username, temp);
            }
            outToClient.writeBytes("REGISTERED TORECV "+reg_username+'\n'+'\n');
          }
          else outToClient.writeBytes("ERROR 101 No user registered\n\n");
        }
        else outToClient.writeBytes("ERROR 101 No user registered\n\n");
      }
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  /*
  void forward(String message, String signature, String recepient_username,int message_length, int signature_length, String sender_username)
  {
    try
    {
      Socket comm_socket = table.get(recepient_username).get_recv_socket();
      BufferedReader inFromRecpt = new BufferedReader(new InputStreamReader(comm_socket.getInputStream()));
      DataOutputStream outToRecpt = new DataOutputStream(comm_socket.getOutputStream());
      outToRecpt.writeBytes("FORWARD "+recepient_username+'\n'+"Message_length: "+message_length+'\n'+
                            "Signature_length: "+signature_length + '\n' + message + '\n' + '\n'
                            + signature + '\n' + '\n');
      try
      {
        String line = inFromRecpt.readLine();
        String[] line_split = line.split(" ");
        if(line_split[0].equals("GET_PUBLICKEY"))
        {
          String req_username = line_split[2];
          String encoded_pubkey = fetch_publickey(req_username);
          if(encoded_pubkey == null)
          {
            outToClient.writeBytes("ERROR 104 recepient user not registered\n\n");
            connectionSocket.close();
            table.remove(reg_username);
            return;
          }
          outToRecpt.writeBytes("OK "+encoded_pubkey+'\n'+'\n');
          inFromRecpt.readLine(); // end GET_PUBLICKEY message
          line = inFromRecpt.readLine();
          if(line.equals("RECVD_KEY"))
          inFromRecpt.readLine(); // end RECVD_KEY msg
        }
        else
        {
          outToClient.writeBytes("generic error\n\n");
          comm_socket.close();
          return;
          //Additional handling in run function
        }
        line = inFromRecpt.readLine();
        line_split = line.split(" ");
        if(line_split[0].equals("RECEIVED") && line_split[1].equals(sender_username))
        {
          outToClient.writeBytes("SENT "+recepient_username+'\n'+'\n');
          return;
        }
      }
      catch(Exception e12)
      {
        System.out.println("CATCH_ERROR");
      }
    }
    catch(Exception e)
      {outToClient.writeBytes("ERROR 102 Unable to send\n\n");}
    //close socket?
    return;
  }
  */
  String fetch_publickey(String req_username) // Base64
  {
    if(!table.containsKey(req_username)) return null;
    HashTableData temp_data = table.get(req_username);
    String publickey = Base64.getEncoder().encodeToString(temp_data.get_public_key());
    return publickey;
  }
  /*
//ERROR messages
  void error101()
  {
    try
    {
      outToClient.writeBytes("ERROR 101 No user registered\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void error100()
  {
    try
    {
      outToClient.writeBytes("ERROR 100 Malformed username\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void error102()
  {
    try
    {
      outToClient.writeBytes("ERROR 102 Unable to send\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void error103()
  {
    try
    {
      outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void error104()
  {
    try
    {
      outToClient.writeBytes("ERROR 104 recepient user not registered\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void error105()
  {
    try
    {
      outToClient.writeBytes("ERROR 105 username already used\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  void gen_error()
  {
    try
    {
      outToClient.writeBytes("generic error\n\n");
    }
    catch(Exception e12)
    {
      System.out.println("CATCH_ERROR");
    }
  }
  */
  //checking USERNAME format
  boolean check_username(String str)
  {
    int len = str.length();
    for(int i=0; i<len; i++)
    {
      boolean temp_bool = false;
      int temp = (int)str.charAt(i);
      if(temp<=57 && temp>= 48)
        temp_bool = true;
      else if(temp<= 90 && temp>= 65)
        temp_bool = true;
      else if(temp<= 122 && temp >= 97)
        temp_bool = true;
      else
        temp_bool = false;
      if(!temp_bool) return false;
    }
    return true;
  }
/*
  public static void main(String[] argv)
  {
    Scanner in = new Scanner(System.in);
    while(true)
    {
      String s = in.nextLine();
      //if(check_username(s))
      System.out.println(check_username(s));
    }
  }
  */
}
