import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;
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
	private PublicKey ref_public_key;
	private PrivateKey ref_private_key;
	private byte[] private_key_self;
	int mode_of_communication;
	String myusername;
	communication_thread(	DataOutputStream out_send_socket , BufferedReader in_send_socket, 
				DataOutputStream out_receive_socket, BufferedReader in_receive_socket,
				cryptographyExample crypto_object, byte[] private_key_self, 
				PublicKey ref_public_key, PrivateKey ref_private_key,
				int mode_of_communication, String myusername)
	{
		this.out_send_socket = out_send_socket;
		this.out_receive_socket = out_receive_socket;
		this.in_send_socket = in_send_socket;
		this.in_receive_socket = in_receive_socket;
		this.crypto_object = crypto_object;
		this.private_key_self = private_key_self;
		this.ref_public_key = ref_public_key;
		this.ref_private_key = ref_private_key;
		this.mode_of_communication = mode_of_communication;
		this.myusername = myusername;
	}
	// read the message upto certain bytes
	// only fro un-encrypted message
	public static String read_message(BufferedReader bf, int message_length)
	{
		try
		{
			String message="";
			int byte_counter=0;
			while(byte_counter < message_length)
			{
				String temp_string = bf.readLine();
				message += temp_string + '\n';
				byte_counter += temp_string.getBytes().length+1;
			}
			message = message.substring(0, message.length()-1);
			return message;
		}
		catch(Exception e){System.out.println(e);}
		return "";
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
					while(in_send_socket.ready()){	in_send_socket.readLine();}
					//reading and parsing the user input
					boolean correctly_parsed = false;
					String recipient="", message="", public_key_recipient="", msg_to_server="", response_from_server="";
					System.out.println("type: @recipient_user_id message");
					do
					{
						// reading input from the user
						Scanner input_scanner = new Scanner(System.in);
						String user_input = "", user_line="";
						while((user_line=input_scanner.nextLine()).length() != 0)	user_input += user_line + '\n';
//						System.out.println("user input is: " + user_input);
						//parsing the user input
						int index=0, user_input_length = user_input.length();
						recipient = "";
						message = "";
						if(user_input.charAt(index) == '@')
						{
							index++;
							for(; index < user_input_length && user_input.charAt(index)!=' '; index++)	recipient += user_input.charAt(index);
							if(index != 1 && index != user_input_length)
							{
								index++;
//								for(; index < user_input_length; index++)	message+=user_input.charAt(index);
								message = user_input.substring(index);
							}
							else	{ System.out.println("invalid syntax, try again"); continue;}
						}
						else if(user_input.split("\\s")[0].equals("unregister"))
						{
							msg_to_server = "UNREGISTER " + myusername + '\n' + '\n';
							out_send_socket.writeBytes(msg_to_server);
							System.out.println("Successfully unregistered. You can terminate the program...");
							return;
						}
						else { System.out.println("invalid syntax, try again"); continue;}
//						System.out.println("recipient_id: " + recipient + '\n' + "message: " + message);

						// only fetch if mode is 2, 3
						if(mode_of_communication != 1)
						{
							// fetching recipient public key
							msg_to_server = "GET_PUBLICKEY " + recipient + '\n' + '\n';
//							System.out.println("msg_to_server: " + msg_to_server);
							out_send_socket.writeBytes(msg_to_server);
							response_from_server = in_send_socket.readLine();
							if(! response_from_server.equals("ERROR 104 recipient user not registered"))	public_key_recipient = response_from_server.substring(3);
							else	{ System.out.println("Recipient not registered, try again later or with diff username."); continue;}
							in_send_socket.readLine();
							//sending ack to server
							msg_to_server = "RECVD_KEY" + '\n' + '\n';
							out_send_socket.writeBytes(msg_to_server);
						}
						correctly_parsed = true;
					}
					while(!correctly_parsed);
					

					// encrypt the message
					byte[] encrypted_msg="".getBytes(), hash_msg="".getBytes(), signature="".getBytes();
					MessageDigest md;
					if(mode_of_communication != 1)
					{
						encrypted_msg = crypto_object.encrypt(java.util.Base64.getDecoder().decode(public_key_recipient), message.getBytes());
					}
					if(mode_of_communication == 3)
					{
						md = MessageDigest.getInstance("SHA-256");
						hash_msg = md.digest(encrypted_msg);
						signature = crypto_object.get_signature(ref_private_key, hash_msg);
					}

					String encrypted_msg_string="", signature_string="";
					int msg_length=0, signature_length=0;
					
					// make the msg ready
					if(mode_of_communication == 1)
					{
						msg_length = message.getBytes().length;
						msg_to_server = "SEND " + recipient + '\n' + 
								"Content-length: " + msg_length + 
								'\n' + '\n'
								+ message + '\n' + '\n';
					}
					else if(mode_of_communication == 2)
					{
						// dispatch message and signature through send packet
						encrypted_msg_string = java.util.Base64.getEncoder().encodeToString(encrypted_msg);
//						System.out.println("encrypted message in base64 is: " + encrypted_msg_string);
						msg_length = encrypted_msg_string.getBytes().length;
						msg_to_server = "SEND " + recipient + '\n' + "Content-length: " + msg_length + '\n' + '\n'
								+ encrypted_msg_string + '\n' + '\n';
					}
					else
					{
						// dispatch message and signature through send packet
						encrypted_msg_string = java.util.Base64.getEncoder().encodeToString(encrypted_msg);
//						System.out.println("encrypted message in base64 is: " + encrypted_msg_string);
						signature_string = java.util.Base64.getEncoder().encodeToString(signature);
//						System.out.println("signature in base64 is: " + signature_string);
						msg_length = encrypted_msg_string.getBytes().length;
						signature_length = signature_string.getBytes().length;// byte length and string length are different
						msg_to_server = "SEND " + recipient + '\n' + "Content-length: " + msg_length + '\n' 
								+ "Signature_length: " + signature_length + '\n' + '\n'
								+ encrypted_msg_string + '\n' + '\n'
								+ signature_string + '\n' + '\n';
					}	
					out_send_socket.writeBytes(msg_to_server);


					// wait for the server response
					response_from_server = in_send_socket.readLine();
					if(response_from_server.equals("ERROR 102 Unable to send"))
						System.out.println("ERROR 102 Unable to send message to recipient " + recipient);
					else if(response_from_server.equals("ERROR 103 Header incomplete"))
					{
						System.out.println("ERROR 103 Header incomplete");
						return; // no continue
					}
					else if(response_from_server.equals("ERROR 104 recipient user not registered"))
						System.out.println("recipient user not registered. Either username is malformed or recipient is offline.");
					else
						System.out.println("Message sent to recipient " + recipient);
					in_send_socket.readLine();
				}
			}
			else
			{
				while(true)
				{
					while(in_receive_socket.ready()) { in_receive_socket.readLine();}
					// waiting for FORWARD message from server
					String response_from_server = "", msg_to_server = "";
					response_from_server = in_receive_socket.readLine();
					// parse the header
					
					// checking the first line
					String sender_username = "";
					int response_from_server_length = response_from_server.length();
					if(response_from_server_length >=  8 && response_from_server.substring(0, 7).equals("FORWARD"))
						sender_username = response_from_server.substring(8);
					else
					{
						msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						return;
					}
					int message_length, signature_length;
					try
					{
						// checking the second line
						response_from_server = in_receive_socket.readLine();
						response_from_server_length = response_from_server.length();
						if(response_from_server_length >= 16 && response_from_server.substring(0, 16).equals("Content-length: ") )
							message_length = Integer.parseInt(response_from_server.substring(16));
						else
						{
							msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
							out_receive_socket.writeBytes(msg_to_server);
							return;
						}
						if(mode_of_communication == 3)
						{
							// checking the third line
							response_from_server = in_receive_socket.readLine();
							response_from_server_length = response_from_server.length();
							if(response_from_server_length >= 18 && response_from_server.substring(0,18).equals("Signature_length: ") )
								signature_length = Integer.parseInt(response_from_server.substring(18));
							else
							{
								msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
								out_receive_socket.writeBytes(msg_to_server);
								return;
							}
						}
					}
					catch(Exception e)
					{
						msg_to_server = "ERROR 103 Header incomplete" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						return;
					}
					// getting the message
					response_from_server = in_receive_socket.readLine();		// empty line
					String 	encrypted_msg_string = read_message(in_receive_socket, message_length), signature_string = "",
						public_key_sender_string="";
					response_from_server = in_receive_socket.readLine();		// empty line

					if(mode_of_communication == 3)
					{
						signature_string = in_receive_socket.readLine();	// in base64
						in_receive_socket.readLine();				// empty line
					}

					if(mode_of_communication != 1)
					{
						// fetching the sender's public key
						msg_to_server = "GET_PUBLICKEY " + sender_username + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						response_from_server = in_receive_socket.readLine();
						if(!response_from_server.equals("ERROR 104 recipient user not registered"))	public_key_sender_string = response_from_server.substring(3);
						else	{ System.out.println("sender not registered, ignoring this packet."); continue;}
						//sending ack to server
						msg_to_server = "RECVD_KEY" + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						in_receive_socket.readLine();
					}
					boolean verify = true;	// for 1, 2 mode of communication
					if(mode_of_communication == 3)
					{
						// signature check
						// hash of ecncrypted messageString str = null;
						MessageDigest md = MessageDigest.getInstance("SHA-256");
						byte[] hash_msg1 = md.digest(java.util.Base64.getDecoder().decode(encrypted_msg_string));
						// decrypt the signature with public key of the sender to get hash of encrypted message
						verify = crypto_object.verify_signature(java.util.Base64.getDecoder().decode(signature_string), 
											java.util.Base64.getDecoder().decode(public_key_sender_string), 
											hash_msg1);
					}
					// checking for equality
					if(verify)
					{
						//acknowledging server for received message
						msg_to_server = "RECEIVED " + sender_username + '\n' + '\n';
						out_receive_socket.writeBytes(msg_to_server);
						String decrypted_message_string = encrypted_msg_string;	
						if(mode_of_communication != 1)
						{
							// decrypting the message
							byte[] decrypted_message = crypto_object.decrypt(private_key_self, java.util.Base64.getDecoder().decode(encrypted_msg_string));
							decrypted_message_string = new String(decrypted_message);
						}
						//show it to user
						System.out.println(sender_username + ": " + decrypted_message_string);
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
			System.out.println("error: " + e);
			return;
		}
	}
}
public class client
{
	private static int send_port = 6789, receive_port = 6789;
	public static void main(String[] args) throws Exception
	{
		// Generate public private key pair
		cryptographyExample crypto_object = new cryptographyExample();
		KeyPair generateKeyPair = crypto_object.generateKeyPair();
		PublicKey ref_public_key = generateKeyPair.getPublic();
		PrivateKey ref_private_key = generateKeyPair.getPrivate();
		byte[] public_key = ref_public_key.getEncoded();
		byte[] private_key = ref_private_key.getEncoded();
		
		// Reading commandline inputs: username server_ip_addr
		if(args.length < 3)
		{
			System.out.println("command line input error: expecting your_userid server_ip_address mode_of_communication\n");
			return;
		}
		String myusername = args[0], server_ip_addr = args[1];
		int mode_of_communication = Integer.parseInt(args[2]);
		
		//creating two sockets, one for sending msg to users, and another for receiving msgs from clients
		Socket send_socket = new Socket(server_ip_addr, send_port);
		Socket receive_socket = new Socket(server_ip_addr, receive_port);	
		DataOutputStream out_send_socket = new DataOutputStream(send_socket.getOutputStream());
		DataOutputStream out_receive_socket = new DataOutputStream(receive_socket.getOutputStream());
		BufferedReader in_send_socket = new BufferedReader(new InputStreamReader(send_socket.getInputStream()));
		BufferedReader in_receive_socket = new BufferedReader(new InputStreamReader(receive_socket.getInputStream()));

		String msg_to_server="", public_key_string="", reg_response_from_server="";
		if(mode_of_communication == 1)
		{
			//sending torcv msg to server
			msg_to_server = "REGISTER TORECV " + myusername + '\n' + '\n';
			out_receive_socket.writeBytes(msg_to_server);

			//waiting for server ack
			reg_response_from_server = in_receive_socket.readLine();
			if(reg_response_from_server.equals("ERROR 100 Malformed username") || reg_response_from_server.equals("ERROR 105 username already used"))
			{
				System.out.println( reg_response_from_server + ": " + "Try again with other username(alphabets and numerals only)");
				return;
			}
			in_receive_socket.readLine();	

			//sending tosend message to server
			msg_to_server = "REGISTER TOSEND " + myusername + '\n' + '\n';
			out_send_socket.writeBytes(msg_to_server);

			//waiting for server ack
			reg_response_from_server = in_send_socket.readLine();
			if(reg_response_from_server.equals("ERROR 100 Malformed username") || reg_response_from_server.equals("ERROR 105 username already used"))
			{
				System.out.println( reg_response_from_server + ": Try again with other username(alphabets and numerals only)");
				return;
			}
			in_send_socket.readLine();		// because of the '\n' used to end the message


			System.out.println(myusername + " registered successfully.");
		}
		else
		{
			public_key_string = java.util.Base64.getEncoder().encodeToString(public_key);
			//sending torcv msg to server
			msg_to_server = "REGISTER TORECV " + myusername + '\n' + public_key_string + '\n' + '\n';
			out_receive_socket.writeBytes(msg_to_server);

			//waiting for server ack
			reg_response_from_server = in_receive_socket.readLine();
			if(reg_response_from_server.equals("ERROR 100 Malformed username") || reg_response_from_server.equals("ERROR 105 username already used"))
			{
				System.out.println(reg_response_from_server + ": " + "Try again with other username(alphabets and numerals only)");
				return;
			}
			in_receive_socket.readLine();	

			//sending tosend message to server
			msg_to_server = "REGISTER TOSEND " + myusername + '\n' + public_key_string + '\n' + '\n';
			out_send_socket.writeBytes(msg_to_server);

			//waiting for server ack
			reg_response_from_server = in_send_socket.readLine();
			if(reg_response_from_server.equals("ERROR 100 Malformed username") || reg_response_from_server.equals("ERROR 105 username already used"))
			{
				System.out.println( reg_response_from_server + ": Try again with other username(alphabets and numerals only)");
				return;
			}
			in_send_socket.readLine();		// because of the '\n' used to end the message


		}

		//creating two threads
		for(int i=0; i<2; i++)
		{
			communication_thread communication = new communication_thread(out_send_socket, in_send_socket, out_receive_socket, in_receive_socket, crypto_object, private_key, ref_public_key, ref_private_key, mode_of_communication, myusername);
			Thread thread = new Thread(communication);
			thread.start();
		}	
	}
}
