/*
 * The MIT License
 * https://github.com/sonuauti/Android-Web-Server
 * Copyright 2018 Sonu Auti http://sonuauti.com twitter @SonuAuti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package race;

import ShimsJava.RaceLog;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 *
 * @author Sonu Auti @cis
 */
public class TinyWebServer extends Thread {

    /**
     * @param args the command line arguments
     */
    private static final String TAG = "TinyWebServer";

    private static ServerSocket serverSocket;
    private final Map<String, String> lowerCaseHeader = new HashMap<>();

    public static String CONTENT_TYPE = "text/html";
    private String CONTENT_DATE = "";
    private String CONN_TYPE = "";
    private String Content_Encoding = "";
    private String content_length = "";
    private String STATUS = "200";
    private boolean keepAlive = true;
    private String SERVER_NAME = "Firefly http server v0.1";
    private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";
    private static final String ASCII_ENCODING = "US-ASCII";
    private String REQUEST_TYPE = "GET";
    private String HTTP_VER = "HTTP/1.1";

    //all status
    public static String PAGE_NOT_FOUND = "404";
    public static String OKAY = "200";
    public static String CREATED = "201";
    public static String ACCEPTED = "202";
    public static String NO_CONTENT = "204";
    public static String PARTIAL_NO_CONTENT = "206";
    public static String MULTI_STATUS = "207";
    public static String MOVED_PERMANENTLY = "301";
    public static String SEE_OTHER = "303";
    public static String NOT_MODIFIED = "304";
    public static String TEMP_REDIRECT = "307";
    public static String BAD_REQUEST = "400";
    public static String UNAUTHORIZED_REQUEST = "401";
    public static String FORBIDDEN = "403";
    public static String NOT_FOUND = "404";
    public static String METHOD_NOT_ALLOWED = "405";
    public static String NOT_ACCEPTABLE = "406";
    public static String REQUEST_TIMEOUT = "408";
    public static String CONFLICT = "409";
    public static String GONE = "410";
    public static String LENGTH_REQUIRED = "411";
    public static String PRECONDITION_FAILED = "412";

    public static String PAYLOAD_TOO_LARGE = "413";
    public static String UNSUPPORTED_MEDIA_TYPE = "415";
    public static String RANGE_NOT_SATISFIABLE = "416";
    public static String EXPECTATION_FAILED = "417";
    public static String TOO_MANY_REQUESTS = "429";

    public static String INTERNAL_ERROR = "500";
    public static String NOT_IMPLEMENTED = "501";
    public static String SERVICE_UNAVAILABLE = "503";
    public static String UNSUPPORTED_HTTP_VERSION = "505";

    public static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";

    public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";

    public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";

    public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

    public static final String CONTENT_LENGTH_REGEX = "Content-Length:";
    public static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile(CONTENT_LENGTH_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String USER_AGENT = "User-Agent:";
    public static final Pattern USER_AGENT_PATTERN = Pattern.compile(USER_AGENT, Pattern.CASE_INSENSITIVE);

    public static final String HOST_REGEX = "Host:";
    public static final Pattern CLIENT_HOST_PATTERN = Pattern.compile(HOST_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONNECTION_TYPE_REGEX = "Connection:";
    public static final Pattern CONNECTION_TYPE_PATTERN = Pattern.compile(CONNECTION_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String ACCEPT_ENCODING_REGEX = "Accept-Encoding:";
    public static final Pattern ACCEPT_ENCODING_PATTERN = Pattern.compile(ACCEPT_ENCODING_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";

    private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

    private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);


    public static String APK_PATH="/";
    public static String SERVER_IP="localhost";
    public static int SERVER_PORT=9000;
    private String passphrase;
    public static boolean isStart=false;


    public TinyWebServer(final String ip, final int port, final String password) throws IOException {

        InetAddress addr = InetAddress.getByName(ip); ////"172.31.0.186");
        serverSocket = new ServerSocket(port, 100, addr);
        serverSocket.setSoTimeout(5000);  //set timeout for listner
        passphrase = password;

    }

    @Override
    public void run() {

        while (isStart) {
            try {
                //wait for new connection on port
                Socket newSocket = serverSocket.accept();
                Thread newClient = new EchoThread(newSocket);
                newClient.start();
            } catch (SocketTimeoutException s) {
            } catch (IOException e) {
            }
        }//endof Never Ending while loop
    }

    public class EchoThread extends Thread {

        protected Socket socket;
        protected boolean nb_open;

        public EchoThread(Socket clientSocket) {
            this.socket = clientSocket;
            this.nb_open = true;
        }

        @Override
        public void run() {

            try {
                DataInputStream in = null;
                DataOutputStream out = null;

                if (socket.isConnected()) {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                }

                byte[] data = new byte[1500];
                //socket.setSoTimeout(60 * 1000 * 5);

                while (in.read(data) != -1) {
                    String recData = new String(data).trim();
                    //Log.d(TAG, "received data: \n" + recData);
                    //Log.d(TAG, "------------------------------");
                    String[] header = recData.split("\\r?\\n");

                    String contentLen = "0";
                    String contentType = "text/html";
                    String connectionType = "keep-alive";
                    String hostname = "";
                    String userAgent = "";
                    String encoding = "";

                    String[] h1 = header[0].split(" ");
                    if (h1.length == 3) {
                        setRequestType(h1[0]);
                        setHttpVer(h1[2]);
                    }

                    for (int h = 0; h < header.length; h++) {
                        String value = header[h].trim();

                        //Log.d(TAG, header[h]+" -> "+CONTENT_LENGTH_PATTERN.matcher(header[h]).find());
                        if (CONTENT_LENGTH_PATTERN.matcher(value).find()) {
                            contentLen = value.split(":")[1].trim();
                        } else if (CONTENT_TYPE_PATTERN.matcher(value).find()) {
                            contentType = value.split(":")[1].trim();
                        } else if (CONNECTION_TYPE_PATTERN.matcher(value).find()) {
                            connectionType = value.split(":")[1].trim();
                        } else if (CLIENT_HOST_PATTERN.matcher(value).find()) {
                            hostname = value.split(":")[1].trim();
                        } else if (USER_AGENT_PATTERN.matcher(value).find()) {
                            for (String ua : value.split(":")) {
                                if (!ua.equalsIgnoreCase("User-Agent:")) {
                                    userAgent += ua.trim();
                                }
                            }
                        } else if (ACCEPT_ENCODING_PATTERN.matcher(value).find()) {
                            encoding = value.split(":")[1].trim();
                        }

                    }

                    if (!REQUEST_TYPE.equals("")) {
                        String postData = "";
                        if (REQUEST_TYPE.equalsIgnoreCase("POST") && !contentLen.equals("0")) {
                            postData = header[header.length - 1];
                            if (postData.length() > 0 && contentLen.length() > 0) {
                                int len = Integer.valueOf(contentLen);
                                postData = postData.substring(0, len);
                                // Log.d(TAG, "Post data -> " + contentLen + " ->" + postData);
                            }
                        }

                        // Log.d(TAG, "contentLen ->" + contentLen + "\ncontentType ->" + contentType + "\nhostname ->" + hostname + "\nconnectionType-> " + connectionType + "\nhostname ->" + hostname + "\nuserAgent -> " + userAgent);
                        final String requestLocation = h1[1];
                        if (requestLocation != null) {
                            processLocation(out, requestLocation, postData);
                        }
                        //Log.d(TAG, "requestLocation "+requestLocation);
                    }
                }
            } catch (Exception er) {
                er.printStackTrace();
            }
            Log.d(TAG, "Sending finishQrChallengeProtocolActivity intent");
            RaceLog.logDebug(TAG, "Sending finishQrChallengeProtocolActivity intent", "");

            try {
                 Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
                 Context context = app.getApplicationContext();
                 context.sendBroadcast(new Intent("finishQrChallengeProtocolActivity"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void processLocation(DataOutputStream out, String location, String postData) {
        String data = "";
        // Log.d(TAG, "Received request for location: " + location);
        RaceLog.logDebug(TAG, "Received request for location: " + location, "");

        if (location.equals("/" + passphrase) || location.equals(passphrase)) {
            //root location, server index file
            CONTENT_TYPE = getContentType(APK_PATH);
            RaceLog.logDebug(TAG, "CONTENT_TYPE of file: " + CONTENT_TYPE, "");
            if (sendFile(out, APK_PATH, CONTENT_TYPE) != 0) {
                pageNotFound();
            }
        } else {
            pageNotFound();
        }
    }

    public void setRequestType(String type) {
        // Log.d(TAG, "REQUEST TYPE " + type);
        this.REQUEST_TYPE = type;
    }

    public void setHttpVer(String httpver) {
        // Log.d(TAG, "REQUEST ver " + httpver);
        this.HTTP_VER = httpver;
    }

    public String pageNotFound() {
        STATUS = NOT_FOUND;
        CONTENT_TYPE = "text/html";
        //customize your page here
        return "<!DOCTYPE html>"
                + "<html><head><title>Page not found</title>"
                + "</head><body><h3>Requested page not found</h3></body></html>";
    }

    //hashtable initilization for content types
    static Hashtable<String, String> mContentTypes = new Hashtable();
    {
//        mContentTypes.put("apk", "application/zip");
        mContentTypes.put("apk", "application/vnd.android.package-archive");
        mContentTypes.put("tar", "application/x-tar");
        mContentTypes.put("tar.gz", "application/x-gtar");
        mContentTypes.put("tgz", "application/x-gtar");
        mContentTypes.put("rar", "application/x-rar-compressed");
        mContentTypes.put("7z", "application/x-7z-compressed");
        mContentTypes.put("zip", "application/zip");
        mContentTypes.put("js", "application/javascript");
        mContentTypes.put("php", "text/html");
        mContentTypes.put("java", "text/html");
        mContentTypes.put("json", "application/json");
        mContentTypes.put("png", "image/png");
        mContentTypes.put("jpg", "image/jpeg");
        mContentTypes.put("html", "text/html");
        mContentTypes.put("css", "text/css");
        mContentTypes.put("mp4", "video/mp4");
        mContentTypes.put("mov", "video/quicktime");
        mContentTypes.put("wmv", "video/x-ms-wmv");
    }

    //get request content type
    public static String getContentType(String path) {
        String type = tryGetContentType(path);
        if (type != null) {
            return type;
        }
        return "text/plain";
    }

    //get request content type from path
    public static String tryGetContentType(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String e = path.substring(index + 1);
            String ct = mContentTypes.get(e);
            // Log.d(TAG, "content type: " + ct);
            if (ct != null) {
                return ct;
            }
        }
        return null;
    }

    private void constructHeaderText(DataOutputStream output, String size, String data) {
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
        pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
        if (this.CONTENT_TYPE != null) {
            printHeader(pw, "Content-Type", this.CONTENT_TYPE);
        }
        printHeader(pw, "Date", gmtFrmt.format(new Date()));
        printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
        printHeader(pw, "Content-Length", size);
        printHeader(pw, "Server", SERVER_NAME);
        pw.append("\r\n");
        pw.append(data);
        pw.flush();
        //pw.close();
    }

    private void constructHeaderBinary(DataOutputStream output, String size, byte[] data) {
        try{

            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
            pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
            if (this.CONTENT_TYPE != null) {
                printHeader(pw, "Content-Type", this.CONTENT_TYPE);
            }
            printHeader(pw, "Date", gmtFrmt.format(new Date()));
            printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
            printHeader(pw, "Content-Length", size);
            printHeader(pw, "Server", SERVER_NAME);

            // printHeader(pw, "Content-Disposition", "attachment; filename=\"RACE.apk\"");

            pw.append("\r\n");
            pw.flush();
            output.write(data);
            output.flush();
            //Log.d(TAG, "data sent success");

            //pw.close();
        }catch(Exception er){er.printStackTrace();}

    }

    private int sendFile(DataOutputStream output, String fileName, String filetype) {
        try{
            File ifile=new File(fileName);
            String filenameSuggestion = ifile.getName();
            if(!ifile.exists()) {
                // Log.e(TAG, "File does not exist: " + fileName);
                RaceLog.logError(TAG, "File does not exist: " + fileName, "");
                return -1;
            }

            FileInputStream fis = new FileInputStream(fileName);
            int size = fis.available();

            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
            pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
            if (filetype != null) {
                printHeader(pw, "Content-Type", filetype);
            }
            printHeader(pw, "Date", gmtFrmt.format(new Date()));
            printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
            printHeader(pw, "Content-Length", size+"");
            printHeader(pw, "Server", SERVER_NAME);
            printHeader(pw, "Content-Disposition", "attachment; filename=\"" + filenameSuggestion + "\"");

            pw.append("\r\n");
            pw.flush();

            byte[] buffer = new byte[4096];
            while (fis.read(buffer) != -1) {
                output.write(buffer);
                output.flush();
            }
            fis.close();

            //Log.d(TAG, "data sent success");
            //pw.close();
        } catch(Exception er) {
            er.printStackTrace();
            return -1;
        }
        return 0;
    }

    @SuppressWarnings("static-method")
    protected void printHeader(PrintWriter pw, String key, String value) {
        pw.append(key).append(": ").append(value).append("\r\n");
    }

    public byte[] readBinaryFiles(String fileName, String filetype){
        try{
            File ifile=new File(fileName);
            if(ifile.exists()){
                if(filetype.equalsIgnoreCase("image/png") || filetype.equalsIgnoreCase("image/jpeg") || filetype.equalsIgnoreCase("image/gif") || filetype.equalsIgnoreCase("image/jpg") || filetype.equalsIgnoreCase("application/vnd.android.package-archive")){
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buffer = new byte[fis.available()];
                    while (fis.read(buffer) != -1) {}
                    fis.close();
                    return buffer;
                }
            }else{

            }
        }catch(Exception er){}
        return null;
    }
    public String readFile(String fileName){
        String content="";
        try{
            File ifile=new File(fileName);
            if(ifile.exists()){
                FileInputStream fis = new FileInputStream(fileName);
                byte[] buffer = new byte[10];
                StringBuilder sb = new StringBuilder();
                while (fis.read(buffer) != -1) {
                    sb.append(new String(buffer));
                    buffer = new byte[10];
                }
                fis.close();
                content = sb.toString();
            }else{
                pageNotFound();
                return content;
            }
        }catch(Exception er){
            pageNotFound();
            return "";
        }
        return content;
    }


    public static void init(String ip,int port,String apk_path, String passphrase){
        SERVER_IP=ip;
        SERVER_PORT=port;
        APK_PATH=apk_path;
        findApkFile();
    }

    public static void startServer(String ip, int port, String file_to_serve, String passphrase){
        try {
            isStart = true;
            init(ip, port, file_to_serve, passphrase);
            Thread t = new TinyWebServer(SERVER_IP, SERVER_PORT, passphrase);
            t.start();
            // Log.d(TAG, "Server Started! Serving file: " + file_to_serve);
            RaceLog.logDebug(TAG, "Server Started! Serving file: " + file_to_serve, "");
            RaceLog.logDebug(TAG, "\tAt location: " + ip + ":" + Integer.toString(SERVER_PORT) + "/" + passphrase, "");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }

    public static void stopServer(){
        if(isStart){
            try{
                isStart = false;
                serverSocket.close();
                // Log.d(TAG, "Server stopped running!");
                RaceLog.logDebug(TAG, "Server stopped running!", "");
            }catch(IOException er){
                er.printStackTrace();
            }
        }
    }

    //scan for APK file
    public static void findApkFile(){
        boolean isIndexFound=false;
        try {
            File file=new File(APK_PATH);
            if (file.isFile()) {
                isIndexFound = true;
            }
        } catch(Exception er){
            er.printStackTrace();
        }

        if(!isIndexFound){
            // Log.e(TAG, "File to serve not found: " + APK_PATH);
            RaceLog.logError(TAG, "File to serve not found: " + APK_PATH, "");
        }
    }

   /* //use for testing
    public static void main(String[] args) {
        try {

            Thread t = new TinyWebServer(SERVER_IP, SERVER_PORT);
            t.start();
            Log.d(TAG, "Server Started!");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }*/

}