import java.io.*;
import java.net.*;
class server
{
	public static void main(String[] args) throws Exception
	{
		String clientSentence;
		String serverSentence;
		ServerSocket welcomeSocket = new ServerSocket(6789);
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		Socket connectionSocket = welcomeSocket.accept();
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		while(true)
		{

			clientSentence = inFromClient.readLine();
			System.out.println("client says: " + clientSentence);
			if(clientSentence == "close socket")	break;
			serverSentence = userInput.readLine();
			outToClient.writeBytes(serverSentence + '\n');
		}
	}
}
