package httpserver;

import java.io.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketThread implements Runnable {
    SelectionKey key;
    Map<SocketChannel, byte[]> responseData;
    SocketChannel channel;
    String request;

    public SocketThread(SocketChannel channel, SelectionKey key,
                              Map<SocketChannel, byte[]> responseData, String request) throws Throwable {
        this.key = key;
        this.responseData = responseData;
        this.channel = channel;
        this.request = request;
    }

    public void run() {
        try {
            requestParser(request);
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.out.println(e.toString());
            sendResponse(null,errorResponse("404", "Not Found"));
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.toString());
            sendResponse(null, errorResponse("400", "Bad request"));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            sendResponse(null, errorResponse("500", "Internal Server Error"));
        }
    }

    private void sendResponse(byte[] bytes, String s) {
        try {
            byte[] combined;
            if(bytes != null) {
                byte[] one = s.getBytes();
                combined = new byte[one.length + bytes.length];

                System.arraycopy(one, 0, combined, 0, one.length);
                System.arraycopy(bytes, 0, combined, one.length, bytes.length);
            } else {
                combined = s.getBytes();
            }
            responseData.put(channel, combined);
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String httpResponse(String code, String error, String html, String contentType) {
        String header = "HTTP/1.1 " + code + " " + error +"\r\n";
        if(contentType != null)
            header += "Content-Type: " + contentType + "; charset=UTF-8\r\n";
        return header + "Connection: close\r\n\r\n" + html;
    }

    private String errorResponse(String code, String error) {
        String html = "<html><body><h1>" + code + " : " + error + "</h1></body></html>";
        return httpResponse(code, error, html, "text/html");
    }

    private void sendFile(String filename) throws Exception {
        String contentType = FileReader.getContentType(filename);
        if(contentType == null) {
            throw  new FileNotFoundException();
        }

        byte[] file = FileReader.getFile(filename, contentType);
        sendResponse(file, httpResponse("200", "OK", "", contentType));
    }

    private void requestParser(String response) throws Throwable {
        response = response.split("\r\n")[0];

        Pattern p = Pattern.compile("^GET");
        Matcher m = p.matcher(response);

        if(!m.find()) {
            sendResponse(null, errorResponse("405", "Method Not Allowed"));
            return;
        }

        p = Pattern.compile(" \\/\\S*");
        m = p.matcher(response);
        
        if(m.find()) {
            sendFile(m.group());
        } else
            throw new Exception();
    }

}
