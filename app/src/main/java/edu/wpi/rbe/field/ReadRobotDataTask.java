package edu.wpi.rbe.field;


import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Created by peter on 10/24/15.
 */
public class ReadRobotDataTask extends AsyncTask<Void, byte[], Boolean> {

    InputStream is;
    List<BluetoothMessageCallback> listeners;
    boolean heartbeat = false;

    public ReadRobotDataTask(InputStream is, List<BluetoothMessageCallback> listeners) {
        this.listeners = listeners;
        this.is = is;
    }

    protected Boolean doInBackground(Void... params) {
        while (true) {
            try {
                if (is.read() == 0x5f) {
                    int size = is.read();
                    if (size>0) {
                        byte[] packet = new byte[size + 1];
                        packet[0] = (byte) 0x5f;
                        packet[1] = (byte) size;

                        try {
                            is.read(packet, 2, size - 1);
                            publishProgress(packet);
                        } catch (IOException e) {
                            Log.e(getClass().toString(), "failed to read the rest of a packet!");
                        }
                    }
                }
            } catch (NullPointerException ne) {
            } catch (IOException e) {
                // This means the robot was turned off
                Log.e("ROBOT OFF", "Couldn't receive.");
                return false;
            }
        }
    }

    @Override
    protected void onProgressUpdate(byte[]... packets) {
        byte packet[] = packets[0];
        int packetSize = packet[1];

        if (packetSize >= 5) {
            int dataSize = packetSize - 5;
            int type = (int) packet[2];

            BTProtocol.Type properType;
            byte[] data = new byte[dataSize];

            for (int i = 0; i < dataSize; i++) {
                data[i] = packet[i + 5];
            }

            if (type == BTProtocol.Type.HEARTBEAT.id()) {
                properType = BTProtocol.Type.HEARTBEAT;
            } else if (type == BTProtocol.Type.ALERT.id()) {
                properType = BTProtocol.Type.ALERT;
            } else if (type == BTProtocol.Type.STATUS.id()) {
                properType = BTProtocol.Type.STATUS;
            } else if (type == BTProtocol.Type.DEBUG.id()) {
                properType = BTProtocol.Type.DEBUG;
            } else {
                for (BluetoothMessageCallback listener : listeners) {
                    listener.invalidMessage();
                }
                return;
            }
            for (BluetoothMessageCallback listener : listeners) {
                listener.validMessage(properType, data);
            }
        }
    }

    protected void onPostExecute(Boolean success) {
        if (!success) {
            try {
                for (BluetoothMessageCallback listener : listeners) {
                    listener.robotDisconnected();
                }
            } catch (ConcurrentModificationException e) {}
        }
    }
}
