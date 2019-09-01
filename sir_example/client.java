import  java.io.*;
import java.net.*;
class client
{
	public static void main(String[] args) throws Exception
	{
		String sentence;
		String modifiedSentence;
		Socket clientSocket = new Socket("localhost", 6789);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		while(true)
		{
			// send thread
			sentence = inFromUser.readLine();
			outToServer.writeBytes(sentence + '\n');
			if(sentence == "close socket") break;
			//receive thread
			modifiedSentence = inFromServer.readLine();
			System.out.println("Server says: " + modifiedSentence);
		}
		clientSocket.close();
	}
}
