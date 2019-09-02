import java.io.*; 
import java.net.*; 

class TCPServer { 


  public static void main(String argv[]) throws Exception 
    { 

      ServerSocket welcomeSocket = new ServerSocket(6789); 
  
      while(true) { 
 
          Socket connectionSocket = welcomeSocket.accept(); 

          BufferedReader inFromClient = 
           new BufferedReader(new
           InputStreamReader(connectionSocket.getInputStream())); 


          DataOutputStream outToClient = 
           new DataOutputStream(connectionSocket.getOutputStream()); 

	  SocketThread socketThread = new SocketThread(connectionSocket, inFromClient, outToClient);
          Thread thread = new Thread(socketThread);
          thread.start();  

      }

    } 
} 
 

class SocketThread implements Runnable {
     String clientSentence; 
     String capitalizedSentence; 
     Socket connectionSocket;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
   
     SocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocket = connectionSocket;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     } 

     public void run() {
       while(true) { 
	   try {

	           clientSentence = inFromClient.readLine(); 

		   System.out.println(clientSentence);

  	         capitalizedSentence = clientSentence.toUpperCase() + '\n'; 

        	   outToClient.writeBytes(capitalizedSentence); 
	   } catch(Exception e) {
		try {
			connectionSocket.close();
		} catch(Exception ee) { }
		break;
	   }
        } 
    }
}

