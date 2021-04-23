package proxy;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class ProxyDaemon {

	public static void main(String args[]) throws Exception {
		
		ServerSocket welcomeSocket = new ServerSocket(8080);

		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			new Thread(new ServerHandler(connectionSocket)).start();
		}

	}

}

class ServerHandler implements Runnable {
	
	
	String[] fulllist = { "www.aaronsw.com", "cse.yeditepe.edu.tr", "www.instagram.com", "www.yandex.com"};
	ArrayList<String> list = new ArrayList<>();
	boolean flag = false;
	
	
		
	
	
	Socket clientSocket;
	DataInputStream inFromClient;
	DataOutputStream outToClient;

	String host;

	String path;

	String hd;

	public ServerHandler(Socket s) {
		clientSocket = s;
		
		
		for (int i=0;i<fulllist.length;i++) {
			list.add(fulllist[i]);
		}
		
		try {
			inFromClient = new DataInputStream(s.getInputStream());
			outToClient = new DataOutputStream(s.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		try {

			hd = getHeader(inFromClient);
			
			String method = hd.split(" ",2)[0];
			
			int sp1 = hd.indexOf(' ');
			int sp2 = hd.indexOf(' ', sp1 + 1);
			int eol = hd.indexOf('\r');
			
			String address = hd.substring(sp1 + 1, sp2);
			
			System.out.println(address);
			
			String pairs = hd.substring(eol + 2);
			MimeHeader inMH = new MimeHeader(pairs);

			host = inMH.get("Host");
			
			System.out.println("Requested a connection to " + host);

			
			
			
			
			String fouroonemssg = "<!DOCTYPE html>\r\n<body>\r\n<h1>\r\n401 Not Authorized\r\n</h1>\r\nError when fetching URL: "+address+"\r\n</body>\r\n</html>\r\n\r\n";
			String fourofivemssg = "<!DOCTYPE html>\r\n<body>\r\n<h1>\r\n405 Method Not Allowed\r\n</h1>\r\nError when fetching URL: "+address+"\r\n</body>\r\n</html>\r\n\r\n";
		
			
			if(!method.equals("GET")) {
				System.out.println("Requested method "+method+" is not allowed on proxy server\n");
				outToClient.writeBytes(fourofivemssg);
				outToClient.close();
				flag=true;
			
			}
			
			else {
				for(int i=0;i<list.size();i++) {
					if(list.get(i).equals(host)) {
						flag = true;
						System.out.println("Connection blocked to the host due to the proxy policy\n");
						outToClient.writeBytes(fouroonemssg);
						outToClient.close();
						
					}
				}
				
				if(!flag) {
						System.out.println("Initiating the server connection");
						handleProxy(address, inMH, outToClient);
				}
				
				
			}
		
			flag=false;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
public void handleProxy(String address, MimeHeader inMH, DataOutputStream outToClient) throws Exception {
		try {
		URL url = new URL(address);
		String hostName = url.getHost();
		String path = url.getPath().isEmpty() ? "/" : url.getPath();
		
		Socket sSocket = new Socket(hostName, 80);
		DataInputStream inFromServer = new DataInputStream(sSocket.getInputStream());
		DataOutputStream outToServer = new DataOutputStream(sSocket.getOutputStream());
		
		inMH.put("User-Agent", inMH.get("User-Agent") + " via CSE471 Proxy");
		
		String request = "GET " + path + " HTTP/1.1\r\n" + inMH + "\r\n";
		
		System.out.println(request);

		outToServer.writeBytes(request);

		System.out.println("HTTP request sent to: " + host + "\nWaiting response...");
		
		String responseHeader = getHeader(inFromServer);
		System.out.println("Response header:\n" + responseHeader);
		
		String str2 = responseHeader.split(" ",3)[1];
		
		if(!str2.equals("200")) {
			str2 = (responseHeader.split(" ",2)[1]).split("\r\n",2)[0];
			outToClient.writeBytes("<!DOCTYPE html>\r\n<body>\r\n<h1>\r\n"+str2+"\r\n</h1>\r\n"+responseHeader+"\r\n</body>\r\n</html>\r\n\r\n");
			outToClient.close();
			
		}
		else {
		
		int eol = responseHeader.indexOf('\r');
		
		String pairs = responseHeader.substring(eol + 2);
		MimeHeader responseMH = new MimeHeader(pairs);
		
		int length = Integer.parseInt(responseMH.get("Content-Length"));
				
		byte[] dataArr = new byte[length];
		
		inFromServer.readFully(dataArr);
		
		System.out.println("Data arrived\n." + length + " bytes");
		
		outToClient.writeBytes(responseHeader);
		outToClient.write(dataArr);
		
		System.out.println("Sent data back to client. Proxy for " + host + " is done.");
		
		sSocket.close();
		
		outToClient.close();
		}
		}catch(Exception e) {
			System.out.println("...");
		}
	}
	
	public String getHeader(DataInputStream in) throws Exception {
		
		byte[] header = new byte[1024];

		int data;
		int h = 0;

		while ((data = in.read()) != -1) {
			header[h++] = (byte) data;

			if (header[h - 1] == '\n' && header[h - 2] == '\r' && header[h - 3] == '\n'
					&& header[h - 4] == '\r') {
				break;
			}
		}
		return new String(header, 0, h);
		
	}

}