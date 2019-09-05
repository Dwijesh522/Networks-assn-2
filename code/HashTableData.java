import java.io.*;
import java.net.*;
public class HashTableData
{
  String public_key;
  Socket recv_socket;
  Socket send_socket;
  public HashTableData(String public_key, Socket recv_socket, boolean send)
  {
    this.public_key = public_key;
    if(!send) this.recv_socket = recv_socket;
    else this.send_socket = send_socket;
  }
  public void put_send_socket(Socket send_socket)
  {
    this.send_socket = send_socket;
  }
  public void put_recv_socket(Socket recv_socket)
  {
    this.recv_socket = recv_socket;
  }
  public String get_public_key()
  {
    return public_key;
  }
  public Socket get_send_socket()
  {
    return send_socket;
  }
  public Socket get_recv_socket()
  {
    return recv_socket;
  }
}
