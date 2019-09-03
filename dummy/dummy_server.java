import java.io.*;
import java.net.*;
class dummy_server
{
	public static void main(String[] args) throws Exception
	{
		String clientSentence, clientSentence2;
		String serverSentence;
		ServerSocket welcomeSocket = new ServerSocket(6789);
		ServerSocket welcomeSocket_recv = new ServerSocket(6790);
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		Socket connectionSocket = welcomeSocket.accept();
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		while(true)
		{
			clientSentence = inFromClient.readLine();
			clientSentence2 = inFromClient.readLine();
			clientSentence2 = inFromClient.readLine();
			System.out.println("client says: " + clientSentence);
			System.out.println("client2 says: " + clientSentence2);
			if(clientSentence == "close socket")	break;
			serverSentence = userInput.readLine();
			outToClient.writeBytes(serverSentence + '\n');
		}
	}
}
