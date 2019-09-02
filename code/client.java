import java.security.KeyPair;
import cryptography.cryptographyExample;
import java.util.*;
import java.io.*;
import java.net.*;
class communication_thread implements Runnable
{
	communication_thread()
	{}
	
	public void run()
	{
		try
		{
			// even id: send thread
			// odd id : receiving thread
			if(Thread.currentThread().getId() % 2 == 0)
			{
				//reading and parsing the user input
				boolean correctly_parsed = false;
				do
				{
					// reading input from the user
					Scanner input_scanner = new Scanner(System.in);
					System.out.println("type: @recipien_user_id message");
					String user_input = input_scanner.nextLine();
					
					//parsing the user input
					int index=0, user_input_length = user_input.length();
					String message="", recipient="";
					if(user_input.charAt(index) == '@')
					{
						for(; user_input.charAt(index)!=' '; index++)	recipient += user_input.charAt(index);
						index++;
						for(; index < user_input_length; index++)	message+=user_input.charAt(index);
					}
				}
				while(!correctly_parsed);
			}
			else
			{

			}
		}
		catch(Exception e)
		{
			System.out.println("Exception cought\n");
		}
	}
}
public class client
{
	private static int send_port = 6789, receive_port = 6790;
	public static void main(String[] args) throws Exception
	{
		// Generate public private key pair
		cryptographyExample crypto_object = new cryptographyExample();
		KeyPair generateKeyPair = crypto_object.generateKeyPair();
		byte[] public_key = generateKeyPair.getPublic().getEncoded();
		byte[] private_key = generateKeyPair.getPrivate().getEncoded();
		
		// Reading commandline inputs: username server_ip_addr
		if(args.length < 2)
		{
			System.out.println("command line input error: expecting your userid and server ip address\n");
			return;
		}
		String myusername = args[0], server_ip_addr = args[1];
		System.out.println(myusername + " " + server_ip_addr);
		
		//creating two sockets, one for sending msg to users, and another for receiving msgs from clients
		Socket send_socket = new Socket(server_ip_addr, send_port);
		Socket receive_socket = new Socket(server_ip_addr, receive_port);
		DataOutputStream out_to_server = new DataOutputStream(send_socket.getOutputStream());
		BufferedReader in_from_server = new BufferedReader(new InputStreamReader(receive_socket.getInputStream()));
		
		//sending tosend message to server
		String public_key_string = new String(public_key);
		String msg_to_server = "REGISTER TOSEND " + myusername + '\n' + public_key_string + '\n' + '\n';
		out_to_server.writeBytes(msg_to_server);

		//waiting for server ack
		String reg_response_from_server = in_from_server.readLine();
		if(reg_response_from_server == "ERROR 100 Malformed username")
		{
			System.out.println("ERROR 100 Malformed username: " + "Try again with correct username(alphabets and numerals only)");
			return;
		}

		//sending torcv msg to server
		msg_to_server = "REGISTER TORECV " + myusername + '\n' + public_key_string + '\n' + '\n';
		out_to_server.writeBytes(msg_to_server);

		//waiting for server ack
		reg_response_from_server = in_from_server.readLine();
		if(reg_response_from_server == "ERROR 100 Malformed username")
		{
			System.out.println("ERROR 100 Malformed username: " + "Try again with correct username(alphabets and numerals only)");
			return;
		}
		
		System.out.println(myusername + " registered successfully.");

		//creating two threads
		for(int i=0; i<2; i++)
		{
			communication_thread communication = new communication_thread();
			Thread thread = new Thread(communication);
			thread.start();
		}
	}
}
