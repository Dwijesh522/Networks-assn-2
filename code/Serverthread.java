import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
public class Serverthread implements Runnable
{
  Socket connectionSocket;
  BufferedReader inFromClient;
  DataOutputStream outToClient;
  Hashtable < String, HashTableData> table;
  int mode;
  public Serverthread(int mode, Hashtable < String, HashTableData> table, Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient)
  {
    this.table = table;
    this.connectionSocket = connectionSocket;
    this.inFromClient = inFromClient;
    this.outToClient = outToClient;
    this.mode = mode;
  }
  public void run() // public
  {
    if(mode <=3 && mode >= 1)
    {
      String reg_username = null, reg_pubkey = null;
      String line = null;
      String[] line_split;
      try
      {
        line = inFromClient.readLine();
        line_split = line.split(" ");
        if(line_split[0].equals("REGISTER"))
        {
          reg_username = line_split[2];
          if(!check_username(reg_username))
          {
            outToClient.writeBytes("ERROR 100 Malformed username\n\n");
            System.out.println("Register send message discarded - invalid username");
            connectionSocket.close();
            return;
          }
          if(mode != 1)
            reg_pubkey = (inFromClient.readLine());
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
                System.out.println("Register send message discarded - username already exists");
                connectionSocket.close();
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
            System.out.println(reg_username+" Registered for send socket");
            while(true)
            {
              line = inFromClient.readLine();
              line_split = line.split(" ");
              if(line_split[0].equals("GET_PUBLICKEY"))
              {
                String req_username = line_split[1];
                String encoded_pubkey = fetch_publickey(req_username);
                if(encoded_pubkey == null)
                {
                  outToClient.writeBytes("ERROR 104 recipient user not registered\n\n");
                  System.out.println("recepient not registered");
                  while(inFromClient.ready())
                  {
                    inFromClient.readLine();
                  }
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
              if(line_split[0].equals("SEND"))
              {
                String recepient_username;
                int message_length=0, signature_length=0;
                try
                {
                  recepient_username = line_split[1];
                  line = inFromClient.readLine();
                  line_split = line.split(" ");
                  if(line_split[0].equals("Content-length:"))
                    message_length = Integer.parseInt(line_split[1]);
                  else
                  {
                    outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                    System.out.println("Send Message discarded");
                    connectionSocket.close();
                    table.get(reg_username).put_send_socket(null);
                    return;
                  }
                  if(mode == 3)
                  {
                    line = inFromClient.readLine();
                    line_split = line.split(" ");
                    if(line_split[0].equals("Signature_length:"))
                      signature_length = Integer.parseInt(line_split[1]);
                    else
                    {
                      outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                      System.out.println("Send Message discarded");
                      connectionSocket.close();
                      table.get(reg_username).put_send_socket(null);
                      return;
                    }
                  }
                }
                catch(Exception e)
                {
                  outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                  System.out.println("Send Message discarded");
                  connectionSocket.close();
                  table.get(reg_username).put_send_socket(null);
                  return;
                }
                inFromClient.readLine(); //end of headers
                String message = read_string(inFromClient, message_length);
                inFromClient.readLine();
                inFromClient.readLine(); // extra \n character
                String signature = null;
                if(mode == 3)
                {
                  signature = read_string(inFromClient, signature_length);
                  inFromClient.readLine(); //end \n character
                  inFromClient.readLine();
                }
                if(table.get(recepient_username) == null)
                {
                  outToClient.writeBytes("ERROR 104 recipient user not registered\n\n");
                  System.out.println("Recpt user not registered");
                  while(inFromClient.ready())
                  {
                    inFromClient.readLine();
                  }
                  continue;
                }
                Socket comm_socket = table.get(recepient_username).get_recv_socket();
                if(comm_socket == null)
                {
                  outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                  System.out.println("Recpt user is not registered to recv");
                  while(inFromClient.ready())
                  {
                    inFromClient.readLine();
                  }
                  continue;
                }
                BufferedReader inFromRecpt = new BufferedReader(new InputStreamReader(comm_socket.getInputStream()));
                DataOutputStream outToRecpt = new DataOutputStream(comm_socket.getOutputStream());
                if(mode == 3)
                  outToRecpt.writeBytes("FORWARD "+reg_username+'\n'+"Content-length: "+message_length+'\n'+
                                        "Signature_length: "+signature_length + '\n' +'\n' + message + '\n' + '\n'
                                        +signature + '\n' + '\n');
                else
                outToRecpt.writeBytes("FORWARD "+reg_username+'\n'+"Content-length: "+message_length+'\n'+
                                      '\n' + message + '\n' + '\n');
                line = inFromRecpt.readLine();
                line_split = line.split(" ");
                if(line_split[0].equals("GET_PUBLICKEY"))
                {
                  String req_username = line_split[1];
                  String encoded_pubkey = fetch_publickey(req_username);
                  if(encoded_pubkey == null)
                  {
                    outToRecpt.writeBytes("ERROR 104 recipient user not registered\n\n");
                    outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                    System.out.println("Recpt not registered");
                    while(inFromClient.ready())
                    {
                      inFromClient.readLine();
                    }
                    continue;
                  }
                  inFromRecpt.readLine(); // end GET_PUBLICKEY message
                  outToRecpt.writeBytes("OK "+encoded_pubkey+'\n'+'\n');
                  line = inFromRecpt.readLine();
                  if(line.equals("RECVD_KEY"))
                  inFromRecpt.readLine(); // end RECVD_KEY msg
                  //read next message
                  line = inFromRecpt.readLine();
                  line_split = line.split(" ");
                }
                else if(line.equals("ERROR 103 Header incomplete"))
                {
                  outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                  System.out.println("Issue with recpt");
                  comm_socket.close(); // close recpt socket
                  table.get(recepient_username).put_recv_socket(null);
                  while(inFromClient.ready())
                  {
                    inFromClient.readLine();
                  }
                  continue;
                }
                if(line_split[0].equals("RECEIVED") && line_split[1].equals(reg_username))
                {
                  System.out.println("Message forwarded succesfully");
                  outToClient.writeBytes("SENT "+recepient_username+'\n'+'\n');
                }
                else
                {
                  outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                  System.out.println("No acknowledgement from recpt");
                  while(inFromClient.ready())
                  {
                    inFromClient.readLine();
                  }
                  continue;
                }
              }
              //UNREGISTER
              else if(line.equals("UNREGISTER"))
              {
                reg_username = line_split[1];
                inFromClient.readLine(); //end message
                table.remove(reg_username);
                System.out.println(reg_username+" had closed");
                return;
              }
              else
              {
                outToClient.writeBytes("generic error\n\n");
                System.out.println("Request discarded due to incorrect syntax");
                connectionSocket.close();
                table.get(reg_username).put_send_socket(null);
                return;
              }
            }
          }
          else if(line_split[1].equals("TORECV"))
          {
            //WRITING TO HASHTABLE
            if(table.containsKey(reg_username))
            {
              HashTableData temp_data = table.get(reg_username);
              if(temp_data.get_recv_socket() != null && temp_data.get_send_socket() != null)
              {
                outToClient.writeBytes("ERROR 105 username already used\n\n");
                System.out.println("Register recv message discarded - username already exists");
                connectionSocket.close();
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
            System.out.println(reg_username + " Registered for recv socket");
          }
          else
          {
            outToClient.writeBytes("ERROR 101 No user registered\n\n");
            System.out.println("Register message discarded - incorrect register message format");
            connectionSocket.close();
            return;
          }
        }
        else
        {
          outToClient.writeBytes("ERROR 101 No user registered\n\n");
          System.out.println("Register discarded - communication before registration");
          connectionSocket.close();
          return;
        }
      }
      catch(Exception e12)
      {
        //Assuming the error is caught only when client is closed
        try{connectionSocket.close();} catch(Exception e) {}
        if(reg_username != null)
        {
          if(table.containsKey(reg_username))
          {
            table.remove(reg_username);
          }
          System.out.println(reg_username+ " had closed");
          return;
        }
        System.out.println("Invalid username had closed");
        return;
      }
    }
  }

  String fetch_publickey(String req_username) // Base64
  {
    if(!table.containsKey(req_username)) return null;
    HashTableData temp_data = table.get(req_username);
    String publickey = (temp_data.get_public_key());
    return publickey;
  }
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

  String read_string(BufferedReader bf, int len)
  {
    try
    {
      char[] temp = new char[len];
      bf.read(temp,0,len);
      String res = new String(temp);
      return res;
    }
    catch(Exception e)
    {
      System.out.println(e);
      return null;
    }
  }
}
