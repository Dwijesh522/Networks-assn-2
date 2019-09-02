import java.security.KeyPair;
import cryptography.cryptographyExample;
class send_receive_thread extends Thread
{
	public void run()
	{
		try
		{
			System.out.println("hello world");
		}
		catch(Exception e)
		{
			System.out.println("Exception cought\n");
		}
	}
}
public class client
{
	public static void main(String[] args) throws Exception
	{
		// Generate public private key pair
		cryptographyExample crypto_object = new cryptographyExample();
		KeyPair generateKeyPair = crypto_object.generateKeyPair();
		byte[] publicKey = generateKeyPair.getPublic().getEncoded();
		byte[] privateKey = generateKeyPair.getPrivate().getEncoded();
		
		// Reading commandline inputs: username server_ip_addr
		if(args.length < 2)
		{
			System.out.println("command line input error: expecting your userid and server ip address\n");
			return;
		}
		String myusername = args[0];
		String server_ip_addr = args[1];
		System.out.println(myusername + " " + server_ip_addr);
	}
}
