package edu.wpi.rbe2001.field;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BTConnector extends AsyncTask<Void, Void, Boolean>{

    private BluetoothSocket socket;
    private BluetoothDevice device;
    private List<BluetoothConnectionCallback> callers;
    public InputStream is;
    public OutputStream os;

    public BTConnector(BluetoothDevice device){
       this.device = device;
        callers = new ArrayList<>();
    }

    public void addListener(BluetoothConnectionCallback listener){
        callers.add(listener);
    }

    public void close(){
        callers.clear();
        try {
            socket.close();
            is.close();
            os.close();
        } catch (Exception e) {}
    }

    public Boolean doInBackground(Void... inputs) {
        try {
            // the hackiest of all hacks!
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket",
                    new Class[] {int.class}).invoke(device,1);
            socket.connect();
            is = socket.getInputStream();
            os = socket.getOutputStream();
            return true;
        } catch (Exception connectException) {
            try {
                socket.close();
            } catch (IOException e) {}
        }
        // nicely handle errors in onPostExecute
        return false;
    }

    public void onPostExecute(Boolean connected){
        for (BluetoothConnectionCallback cb : callers){
            if (connected){
                cb.successfulConnect();
            }
            else {
                cb.failedConnect();
            }
        }
    }
}