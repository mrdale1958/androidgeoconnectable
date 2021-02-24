package com.botherconsulting.geoconnectable;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackgroundUDPSocket extends AsyncTask<String, String, String> {

    boolean messageinQueue = false;
    static UDPSocketClient mUDPSocketClient;
    MapsActivity parentActivity;

    public BackgroundUDPSocket(MapsActivity mapsActivity) {

        parentActivity = mapsActivity;
        if (mUDPSocketClient != null && mUDPSocketClient.isOpen()) mUDPSocketClient.close();

    }

    private static class UDPSocketClient {

        java.net.InetAddress uri;
        org.java_websocket.drafts.Draft draft;
        int port;
        DatagramSocket theSocket;


        public UDPSocketClient(String host, int port) throws UnknownHostException, SocketException {
            this.uri = java.net.InetAddress.getByName(host);
            this.port = port;
            theSocket = new DatagramSocket(port, uri);
        }

        public void connect() {
            theSocket.connect(uri, port);
        }

        public void onOpen(ServerHandshake serverHandshake) throws IOException {
            Log.i("UDPsocket", "Opened");

            this.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
        }

        public void onMessage(String message) {

            //busHandleMessage(message);
            //publishProgress("message", message);

        }

        public boolean isOpen() {
            return (theSocket != null && theSocket.isConnected());
        }

        public void send(String message) throws IOException {
            byte buf[] = message.getBytes();
            DatagramPacket sendPacket =
                    new DatagramPacket(buf, buf.length, uri, port);
            theSocket.send(sendPacket);
        }

        public void onClose(int i, String s, boolean b) {
            Log.i("UDPsocket", "Closed " + s);
            //mWebSocketClient.connect();
        }

        public void onError(Exception e) {

            Log.e("UDPsocket", "Error " + e.getMessage());

        }

        public void close() {
            theSocket.close();
        }
    }


    public void publishProgress(String label, String message) {

    }


    private void busComplain(String... message) {
        Toast.makeText(parentActivity, message[0] + ":" + message[1], Toast.LENGTH_LONG).show();
        if (message[0].equals("Connection closed")) {
            Log.i("websocket closed", "restarting");
            parentActivity.asyncTaskHandler.postDelayed(parentActivity.sensorConnectionLauncher, 15000);
        }
    }

    //inputarg can contain array of values
    @Override
    protected String doInBackground(String... URIString) {
        URI uri;
        int port;
        String serverID;
        try {
            uri = new URI(URIString[0]);
            serverID = uri.getHost();
            port = uri.getPort();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "Invalid URI";
        }
        try {
            mUDPSocketClient = new UDPSocketClient(serverID, port) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) throws IOException {
                    Log.i("UDPsocket", "Opened");
                    mUDPSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
                }

                @Override
                public void onMessage(String message) {

                    busHandleMessage(message);
                    publishProgress("message", message);

                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i("UDPsocket", "Closed " + s);
                    publishProgress("Connection closed", s);
                    //mWebSocketClient.connect();
                }

                @Override
                public void onError(Exception e) {

                    Log.e("UDPsocket", "Error " + e.getMessage());
                    publishProgress("Connection Error", e.getMessage());

                }
            };
        } catch (Exception e) {
            Log.e("New UDP Socket", e.getMessage());
        }
        mUDPSocketClient.connect();
        return null;
    }

    protected void onProgressUpdate(String... progress) {

        //update the progress
        if (progress.length >= 2) {
            if (progress[0].equals("message")) {
                busHandleMessage(progress[1]);
            } else {
                //onMessage(progress[0]);
                parentActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        busComplain(progress);
                    }
                });
            }
        }

    }

    //this will call after finishing the doInBackground function
    protected void onPostExecute(String result) {

        // Update the ui elements
        //show some notification
        //showDialog("Task done " + result);

    }


    private void busHandleMessage(String messageString) {

        if (messageinQueue) {
            //Log.e("websocket message handler", "tossing message "+ messageString);
            return;
        }
        messageinQueue = true;
        JSONObject message;
        try {
            message = new JSONObject(messageString);
        } catch (org.json.JSONException e) {
            Log.i("odd JSON", messageString);
            messageinQueue = false;
            return;
        }
    /*        try {
    messageType = message.getString("type");
    } catch (org.json.JSONException e) {
        e.printStackTrace();
    }*/
        parentActivity.handleMessage(message);
        messageinQueue = false;

    }

    public boolean isOpen() {
        return (mUDPSocketClient != null && mUDPSocketClient.isOpen());
    }

    public void send(String message) throws IOException {
        if (mUDPSocketClient != null) {
            mUDPSocketClient.send(message);
        }

    }
}

