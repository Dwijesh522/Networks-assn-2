import java.security.KeyPair;
import java.security.MessageDigest;
import cryptography.cryptographyExample;
import java.util.*;
import java.io.*;
import java.net.*;
class communication_thread implements Runnable
{
	private DataOutputStream out_send_socket;
	private DataOutputStream out_receive_socket;
	private BufferedReader in_send_socket;
	private BufferedReader in_receive_socket;
	private cryptographyExample crypto_object;
	private byte[] private_key_self;
	communication_thread(	DataOutputStream out_send_socket , BufferedReader in_send_socket, 
				DataOutputStream out_receive_socket, BufferedReader in_receive_socket,
				cryptographyExample crypto_object, byte[] private_key_self)
	{
		this.out_send_socket = out_send_socket;
		this.out_receive_socket = out_receive_socket;
		this.in_send_socket = in_send_socket;
		this.in_receive_socket = in_receive_socket;
		this.crypto_object = crypto_object;
		this.private_key_self = private_key_self;
	}
	
	public void run()
	{
		try
		{
			// even id: send thread
			// odd id : receiving thread
			if(Thread.currentThread().getId() % 2 == 0)
			{
				while(true)
				{
					String str = null;
					while((str = in_send_socket.readLine()) != null)
					{
						if (str.isEmpty()) break;
					}
					//reading and parsing the user input
					boolean correctly_parsed = false;
					String recipient="", message="", public_key_recipient="", msg_to_server="", response_from_server="";
					System.out.println("type: @recipient_user_id message");
					do
					{
						// reading input from the user
						Scanner input_scanner = new Scanner(System.in);
						String user_input = input_scanner.nextLine();
						
						//parsing the user input
						int index=0, user_input_length = user_input.length();
						recipient = "";
						message = "";	
						if(user_input.charAt(index) == '@')
						{
							for(; index < user_input_length && user_input.charAt(index)!=' '; index++)	recipient += user_input.charAt(index);
							if(index != 1 && index != user_input_length)
							{
								index++;
								for(; index < user_input_length; index++)	message+=user_input.charAt(index);
							}
							else	{ System.out.println("invalid syntax, try again"); continue;}
						}
						else	{ System.out.println("invalid syntax, try again"); continue;}
						System.out.println("recipient_id: " + recipient + '\n' + "message: " + message);

						// fetching recipient public key
						System.out.println("fetching recipient public key from send socket...");
						msg_to_server = "GET_PUBLICKEY " + recipient + '\n' + '\n';
						System.out.println("msg_to_server: " + msg_to_server);
						out_send_socket.writeBytes(msg_to_server);
						System.out.println("waiting for response...");
						response_from_server = in_send_socket.readLine();
						System.out.println("got the response...\nresponse is: " + response_from_server);
						if(response_from_server != "ERROR 104 recipient user not registered")	public_key_recipient = response_from_server.substring(3);
						else	{ System.out.println("Recipient not registered, try again later or with diff username."); continue;}
						in_send_socket.readLine();
						//sending ack to server
						msg_to_server = "RECVD_KEY" + '\n' + '\n';
						System.out.println("sending ack to server...\nmessage is: " + msg_to_server);
						out_send_socket.writeBytes(msg_to_server);
						
						correctly_parsed = true;
					}
					while(!correctly_parsed);
					
					// encrypt the message and create a signature
					byte[] encrypted_msg = crypto_object.encrypt(java.util.Base64.getDecoder().decode(public_key_recipient), message.getBytes());
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte[] hash_msg = md.digest(encrypted_msg);
					byte[] signature = crypto_object.encrypt(private_key_self, hash_msg);

					// dispatch message and signature through send packet
					String encrypted_msg_string = java.util.Base64.getEncoder().encodeToString(encrypted_msg);
					String signature_string = java.util.Base64.getEncoder().encodeToString(signature);
					int msg_length = encrypted_msg_string.getBytes().length, signature_length = signature_string.getBytes().length;// byte length and string length are different
					msg_to_server = "SEND " + recipient + '\n' + "Message_length: " + msg_length + '\n' 
							+ "Signature_length: " + signature_length + '\n' + '\n'
							+ encrypted_msg_string + '\n' + '\n'
							+ signature_string + '\n' + '\n';
					out_send_socket.writeBytes(msg_to_server);

					// wait for the server response
					response_from_server = in_send_socket.readLine();
					if(response_from_server == "ERROR 102 Unable to send")
						System.out.println("ERROR 102 Unable to send message to recipient " + recipient);
					else if(response_from_server == "ERROR 103 Header incomplete")
						System.out.println("ERROR 103 Header incomplete");
					else
						System.out.println("Message sent to recipient " + recipient);
					in_send_socket.readLine();
				}
			}
			else
			{
				while(true)
				{
					String str = null;
					System.out.println("hello");
					while((str = in_receive_socket.readLine()) != null){}
					// waiting for FORWARD message from server
					String response_from_server = "", msg_to_server = "";
					response_from_server = in_receive_socket.readLine();

					// parse the header
					// checking the first line
					String sender_username = "";
					int response_from_server_length = response_from_server.length();
					if(response_from_server.substring(0, 7) == "FORWARD" && response_from_server_length >= 8)
						sender_username = response_from_server.substring(8);
					else
					{
						msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						continue;
					}
					// checking the second line
					response_from_server = in_receive_socket.readLine();
					response_from_server_length = response_from_server.length();
					int message_length;
					if(response_from_server.substring(0, 16) == "Message_length: " && response_from_server_length >= 16)
						message_length = Integer.parseInt(response_from_server.substring(16));
					else
					{
						msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						continue;
					}
					// checking the third line
					response_from_server = in_receive_socket.readLine();
					response_from_server_length = response_from_server.length();
					int signature_length;
					if(response_from_server.substring(0,18) == "Signature_length: " && response_from_server_length >= 18)
						signature_length = Integer.parseInt(response_from_server.substring(18));
					else
					{
						msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						continue;
					}
					// getting the message and signature
					response_from_server = in_receive_socket.readLine();
					String encrypted_msg_string = in_receive_socket.readLine();	// in base64
					response_from_server = in_receive_socket.readLine();
					String signature_string = in_receive_socket.readLine();		// in base64
					in_receive_socket.readLine();

					// fetching the sender's public key
					String public_key_sender_string = "";
					msg_to_server = "GET_PUBLICKEY " + sender_username + '\n' + '\n';
					out_receive_socket.writeBytes(msg_to_server);
					response_from_server = in_receive_socket.readLine();
					if(response_from_server != "ERROR 104 recipient user not registered")	public_key_sender_string = response_from_server.substring(3);
					else	{ System.out.println("sender not registered, ignoring this packet."); continue;}
					//sending ack to server
					msg_to_server = "RECVD_KEY" + '\n' + '\n';
					out_receive_socket.writeBytes(msg_to_server);
					in_receive_socket.readLine();

					// signature check
					// hash of ecncrypted messageString str = null;
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte[] hash_msg1 = md.digest(java.util.Base64.getDecoder().decode(encrypted_msg_string));
					// decrypt the signature with public key of the sender to get hash of encrypted message
					byte[] hash_msg2 = crypto_object.decrypt(java.util.Base64.getDecoder().decode(public_key_sender_string), java.util.Base64.getDecoder().decode(signature_string));
					// checking for equality
					if(Arrays.equals(hash_msg1, hash_msg2))
					{
						//acknowledging server for received message
						msg_to_server = "RECEIVED " + sender_username + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						// decrypting the message
						byte[] decrypted_message = crypto_object.decrypt(private_key_self, java.util.Base64.getDecoder().decode(encrypted_msg_string));
						String decrypted_message_string = java.util.Base64.getEncoder().encodeToString(decrypted_message);
						//show it to user
						System.out.println(sender_username + ": " + decrypted_message);
					}
					else
					{
						msg_to_server = "Message corrupted." + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						System.out.println("Message from " + sender_username + " seems to be corrupted, failed in signature check. Ignoring the packet.");
						// figure out what to do
					}
				}
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
		DataOutputStream out_send_socket = new DataOutputStream(send_socket.getOutputStream());
		DataOutputStream out_receive_socket = new DataOutputStream(receive_socket.getOutputStream());
		BufferedReader in_send_socket = new BufferedReader(new InputStreamReader(send_socket.getInputStream()));
		BufferedReader in_receive_socket = new BufferedReader(new InputStreamReader(receive_socket.getInputStream()));

		String msg_to_server="";
		String public_key_string = java.util.Base64.getEncoder().encodeToString(public_key);
		//sending torcv msg to server
		System.out.println("sending torcv msg to server...");
		msg_to_server = "REGISTER TORECV " + myusername + '\n' + public_key_string + '\n' + '\n';
		System.out.println("msg is: " + msg_to_server);
		out_receive_socket.writeBytes(msg_to_server);

		//waiting for server ack
		System.out.println("waiting for server ack...");
		String reg_response_from_server = in_receive_socket.readLine();
		System.out.println("got the response for rcv packet...");
		System.out.println("response is: " + reg_response_from_server);
		if(reg_response_from_server == "ERROR 100 Malformed username" || reg_response_from_server == "ERROR 105 username already used")
		{
			System.out.println("ERROR 100 Malformed username: " + "Try again with correct username(alphabets and numerals only)");
			return;
		}
		in_receive_socket.readLine();	

		//sending tosend message to server
		System.out.println("sending tosend message to server...");
		msg_to_server = "REGISTER TOSEND " + myusername + '\n' + public_key_string + '\n' + '\n';
		out_send_socket.writeBytes(msg_to_server);

		//waiting for server ack
		System.out.println("waiting for server ack...");
		reg_response_from_server = in_send_socket.readLine();
		System.out.println("Got the response for tosend packet...");
		System.out.println("response is: " + reg_response_from_server);
		if(reg_response_from_server == "ERROR 100 Malformed username" || reg_response_from_server == "ERROR 105 username already used")
		{
			System.out.println( reg_response_from_server + ": Try again with unique correct username(alphabets and numerals only)");
			return;
		}
		in_send_socket.readLine();		// because of the '\n' used to end the message


		System.out.println(myusername + " registered successfully.");

		//creating two threads
		for(int i=0; i<2; i++)
		{
			communication_thread communication = new communication_thread(out_send_socket, in_send_socket, out_receive_socket, in_receive_socket, crypto_object, private_key);
			Thread thread = new Thread(communication);
			thread.start();
		}
	}
}
