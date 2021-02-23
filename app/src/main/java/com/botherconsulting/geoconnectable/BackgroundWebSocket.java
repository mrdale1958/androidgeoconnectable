package com.botherconsulting.geoconnectable;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

        public class BackgroundWebSocket extends AsyncTask<String, String, String> {

    boolean messageinQueue = false;
    WebSocketClient mWebSocketClient;
    MapsActivity parentActivity;

    public BackgroundWebSocket(MapsActivity mapsActivity) {
        parentActivity = mapsActivity;
    }

    private void bwsComplain(String... message){
        Toast.makeText(parentActivity,message[0]+":"+message[1],Toast.LENGTH_LONG).show();
        if(message[0].equals("Connection closed")){
            Log.i("websocket closed","restarting");
            parentActivity.asyncTaskHandler.postDelayed(parentActivity.sensorConnectionLauncher,15000);
        }
    }

            //inputarg can contain array of values
    @Override
    protected String doInBackground(String... URIString) {
        URI uri;
        try {
            uri = new URI(URIString[0]);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "Invalid URI";
        }
        mWebSocketClient = new WebSocketClient(uri, new org.java_websocket.drafts.Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String message) {

                bwsHandleMessage(message);
                publishProgress("message", message);

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
                publishProgress("Connection closed", s);
                //mWebSocketClient.connect();
            }

            @Override
            public void onError(Exception e) {

                Log.e("Websocket", "Error " + e.getMessage());
                publishProgress("Connection Error", e.getMessage());

            }
        };
        mWebSocketClient.connect();
        return null;
    }

    protected void onProgressUpdate(String... progress) {

        //update the progress
        if (progress.length >= 2) {
            if (progress[0].equals("message")) {
                bwsHandleMessage(progress[1]);
            } else {
                //onMessage(progress[0]);
                parentActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        bwsComplain(progress);
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


    private void bwsHandleMessage(String messageString) {

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
}

