
import java.io.*;
import java.net.*;
import java.util.*;

public class myServer extends Thread {
	
	static final String HTML_START = 
			"<html>" +
			"<title>Server in java</title>" +
			"<body>";
			
    static final String HTML_END = 
			"</body>" +
			"</html>";
			
	Socket connectedClient = null;	
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	
			
	public myServer(Socket client) {
		connectedClient = client;
	}			
			
	public void run() {
		
	  String currentLine = null, postBoundary = null, contentength = null, filename = null, contentLength = null;
	  PrintWriter fout = null;
		
	  try {
		
		System.out.println( "The Client "+
        connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");
            
        inFromClient = new BufferedReader(new InputStreamReader (connectedClient.getInputStream()));                  
        outToClient = new DataOutputStream(connectedClient.getOutputStream());
        
		currentLine = inFromClient.readLine();
        String headerLine = currentLine;            	
        StringTokenizer tokenizer = new StringTokenizer(headerLine);
		String httpMethod = tokenizer.nextToken();
		String httpQueryString = tokenizer.nextToken();
		
		System.out.println(currentLine);
		StringBuffer responseBuffer = new StringBuffer();
		responseBuffer.append("<b> This is the HTTP Server Home Page.... </b><BR>");
        responseBuffer.append("The HTTP Client request is ....<BR>");
				
        if (httpMethod.equals("GET")) {    
        	System.out.println("GET request");    	
			if (httpQueryString.equals("/")) {
   				  // The default home page
				  sendResponse(200, responseBuffer.toString(), false);
				  //sendResponse(200, responseString , false);				  			  
				} else {
					//This is interpreted as a file name
					String fileName = httpQueryString.replaceFirst("/", "");
					fileName = URLDecoder.decode(fileName);
					if (new File(fileName).isFile()){								
					  sendResponse(200, fileName, true);
					}
					else {
				      sendResponse(404, "<b>The Requested resource not found ...." + 
					  "Usage: http://127.0.0.1:6060 or http://127.0.0.1:6060/<fileName></b>", false);	
					}	
                  
                  
                  
                  
                  
				}
		}
        else if (httpMethod.equals("Head")){
        	
        	
        	
        	sendResponse(200, "<html><title>HTTP Server in java</title><body><b>Welcome to Home Page!</b><BR>GET / HTTP/1.1<BR>Host: localhost:5101<BR>Connection: keep-alive<BR>Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8<BR>User-Agent: Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36<BR>Accept-Encoding: gzip, deflate, sdch<BR>Accept-Language: en-US,en;q=0.8<BR></body></html>", false);
        }
        
        
        
        
		else { //POST request
		    System.out.println("POST request"); 
			do {
				currentLine = inFromClient.readLine();
											
			    if (currentLine.indexOf("Content-Type: multipart/form-data") != -1) {
				  String boundary = currentLine.split("boundary=")[1];
				  // The POST boundary			  			 
				  
				  while (true) {
				  	currentLine = inFromClient.readLine();
				  	if (currentLine.indexOf("Content-Length:") != -1) {
				  		contentLength = currentLine.split(" ")[1];
				  		System.out.println("Content Length = " + contentLength);
				  		break;
				  	}				  	
				  }
				  
				  //Content length should be < 2MB
				  if (Long.valueOf(contentLength) > 2000000L) {
				  	sendResponse(200, "File size should be < 2MB", false);
				  }
				  
				  while (true) {
				  	currentLine = inFromClient.readLine();
				  	if (currentLine.indexOf("--" + boundary) != -1) {
				  		filename = inFromClient.readLine().split("filename=")[1].replaceAll("\"", ""); 				  			 		
				  		String [] filelist = filename.split("\\" + System.getProperty("file.separator"));
				  		filename = filelist[filelist.length - 1];				  		
				  		System.out.println("File to be uploaded = " + filename);
				  		break;
				  	}				  	
				  }
				  
				  String fileContentType = inFromClient.readLine().split(" ")[1];
				  System.out.println("File content type = " + fileContentType);
				  
				  inFromClient.readLine(); //assert(inFromClient.readLine().equals("")) : "Expected line in POST request is "" ";
				  
				  fout = new PrintWriter(filename);
				  String prevLine = inFromClient.readLine();
				  currentLine = inFromClient.readLine();			  
				  
				  //Here we upload the actual file contents
				  while (true) {
				  	if (currentLine.equals("--" + boundary + "--")) {
				  		fout.print(prevLine);
				  		break;
				  	}
				  	else {
				  		fout.println(prevLine);
				  	}	
				  	prevLine = currentLine;			  		
				  	currentLine = inFromClient.readLine();
			      }
			      
			      sendResponse(200, "File " + filename + " Uploaded..", false);
			      fout.close();				   
				} //if							  				
			}while (inFromClient.ready()); //End of do-while
	  	}//else
	  } catch (Exception e) {
			e.printStackTrace();
	  }	
	}
	
	public void sendResponse (int statusCode, String responseString, boolean isFile) throws Exception {
		
		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		String contentLengthLine = null;
		String fileName = null;		
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;
		
		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else if (statusCode == 502)
			statusLine = "HTTP/1.1 502 Bad Gateway" + "\r\n";
		else if (statusCode == 500)
			statusLine = "HTTP/1.1 500 Internal Server Error" + "\r\n";
		else if (statusCode == 401)
			statusLine = "HTTP/1.1 401 Unauthorized" + "\r\n";
		else if (statusCode == 301)
			statusLine = "HTTP/1.1 301 Moved Permanently" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";	
	    
			
		if (isFile) {
			fileName = responseString;			
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";	
		}						
		else {
			responseString = myServer.HTML_START + responseString + myServer.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";	
		}			
		 
		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		outToClient.writeBytes(contentLengthLine);
		outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");		
		
		if (isFile) sendFile(fin, outToClient);
		else outToClient.writeBytes(responseString);
		
		outToClient.close();
	}
	
	public void sendFile (FileInputStream fin, DataOutputStream out) throws Exception {
		byte[] buffer = new byte[1024] ;
		int bytesRead;
	
		while ((bytesRead = fin.read(buffer)) != -1 ) {
		out.write(buffer, 0, bytesRead);
	    }
	    fin.close();
	}
			
	public static void main (String args[]) throws Exception {
		
		ServerSocket Server = new ServerSocket (6060, 10, InetAddress.getByName("127.0.0.1"));         
		System.out.println ("HTTP Server Waiting for client on port 6060");
								
		while(true) {	                	   	      	
				Socket connected = Server.accept();
	            (new myServer(connected)).start();
        }      
	}
}
