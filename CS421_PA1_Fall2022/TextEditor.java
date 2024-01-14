import java.net.*;
import java.io.*;


import java.util.*;

public class TextEditor {

	Socket clientSocket = null;
	DataOutputStream outToServer = null;
	BufferedReader inFromServer = null;
			
	public TextEditor() {
		
	}
	
	public static void main(String[] args) {
		
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		TextEditor t = new TextEditor();
		
		try {
			t.runClient(host, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void runClient(String host, int port) throws UnknownHostException, IOException {
		clientSocket = new Socket(host, port);
	
		
		System.out.println("$ Connected to the server at port " + port);
		
		boolean isAuthenticated = authenticateUser();
		
		if(isAuthenticated) {
			System.out.println("$ Authentication is completed.");
			
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			String commandArgument = null;
			
			System.out.println("$ Waiting for commands:");
			
			do  {
				String msg = inFromUser.readLine();
				commandArgument = msg.split(" ")[0];
				if(commandArgument.equals("WRTE")) {
					sendMsg(msg);
					
					String resp = retrieveMsg();
					String code = resp.split(" ")[0];
					
					if(code.equals("INVALID")) {
						if(resp.contains("No such line exists."))
						{
							System.out.println("$ Specified line number does not exist.");
						} else {
							System.out.println("$ Version Conflict! The current version: " + resp.split(" ")[1] + " please run UPDT command.");
						}
					} else if(code.equals("OK")) {
						System.out.println("$ Write operation is successful.");
					}
					
				}
				else if(commandArgument.equals("APND")) {
					
					sendMsg(msg);		
					String resp = retrieveMsg();
					String code = resp.split(" ")[0];
					
					if(code.equals("INVALID")) {
						System.out.println("$ Version Conflict! The current version: " + resp.split(" ")[1] + " please run UPDT command.");
					} else if(code.equals("OK")) {
						System.out.println("$ Append operation is successful.");
					}
					
				}
				else if(commandArgument.equals("UPDT")) {
					sendMsg(msg);		
					String resp = retrieveMsg();
					String code = resp.split(" ")[0];
					
					if(code.equals("INVALID")) {
						System.out.println("$ You are up to date.");
					} else if(code.equals("OK")) {
						System.out.println("$ The file is updated to " + resp.split(" ")[1] + " .");
					}
				} else {
					System.out.println("$ Command is not recognized!");
				}
			} while (!commandArgument.equals("EXIT"));
			
		}
		
		System.out.println("$ Terminating...");
	
		clientSocket.close();
        outToServer.close();
			
	}
	
	private boolean authenticateUser()  throws IOException {
		
		boolean authenticated = false;
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		
		while(!authenticated) {
			System.out.println("$ Enter your username:");
			String msg = inFromUser.readLine();
			
			String commandArgument = msg.split(" ")[0];
			
			while(!commandArgument.equals("USER")) {
			
				if(commandArgument.equals("EXIT")) {
					
					System.out.println("$ See you later alligator!");
					return false;
				}
				
				System.out.println("$ You should provide a username before moving on");
				msg = inFromUser.readLine();
				commandArgument = msg.split(" ")[0];
			}
			
			String username = msg.split(" ")[1];
			
			sendMsg(msg);
			String resp = retrieveMsg();
						
			if (!resp.split(" ")[0].equals("OK")) {
				System.out.println("$ Incorrect username!");
	            return false;
	        }
			
			System.out.println("$ Password for " + username + ":");
			
			msg = inFromUser.readLine();
			commandArgument = msg.split(" ")[0];
			
			
			while(!commandArgument.equals("PASS")) {
				
				if(commandArgument.equals("EXIT")) {
					
					System.out.println("$ See you later alligator!");
					return false;
				}
				
				System.out.println("$ You should provide a password to complete authentication!");
				msg = inFromUser.readLine();
				commandArgument = msg.split(" ")[0];
			}
			
			sendMsg(msg);
			
			resp = retrieveMsg();
			
			if (resp.split(" ")[0].equals("OK")) {
	            authenticated = true;
	        } else{
	            System.out.println("Incorrect credentials...");
	            return false;
	        }
		}
		
		return authenticated;
	}
	
	private void sendMsg(String msg) throws IOException {
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		outToServer.writeBytes(msg + "\r\n");
		outToServer.flush();
	}
	
	private String retrieveMsg() throws IOException {
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
		return inFromServer.readLine(); 
	}
}
