package com.cirlution.i386koba.droidrone;


/**
 * Created by admin on 2015/08/20.
 * http://toragi.cqpub.co.jp/Portals/0/support/junior/backnumber/2012/09/contents.html
 * トラ技ジュニア　2012年9･10月号 　俺のスマホでロボット操縦
 * TODO Bluetooth機能実装2 - 電子工作チュートリアル
 * http://lumenbolk.com/?p=1418
 * AsyncTask で Broadcast 受信
 * http://blog.zaq.ne.jp/oboe2uran/article/771/
 *Bluetoothで通信を行う(1)
 *http://techbooster.jpn.org/andriod/application/5191/
 *Android　Bluetooth でシリアル通信（SPP）する Fragment 書いた
 * http://y-anz-m.blogspot.jp/2012/09/androidbluetooth-spp-fragment.html
 */

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Bluetooth {
    private final String LOG_TITLE = "BLUETOOTH";
    private BluetoothSocket bluetoothSock = null;
    private InputStream bluetoothIn;
    private OutputStream bluetoothOut;
    private ReceiveThread bluetoothReceiver;
    private LineReceiveListener listener;

    public void setReceiveListener(LineReceiveListener l) {
        listener = l;
    }

    //* 接続
    public boolean connect(BluetoothSocket socket) {
        boolean result = false;
        bluetoothSock = socket;
        if (bluetoothSock != null) {
            InputStream tempIn;
            OutputStream tempOut;
            Log.e(LOG_TITLE, "connect Socket.");
            try {
                //bluetoothSock.connect();
                tempIn = bluetoothSock.getInputStream();
                tempOut = bluetoothSock.getOutputStream();
                bluetoothIn = tempIn;
                bluetoothOut = tempOut;
                bluetoothReceiver = new ReceiveThread();
                bluetoothReceiver.start();
                result = true;
            } catch (IOException e) {
                Log.e(LOG_TITLE, "connect Socket err:", e);
                bluetoothSock = null;
            }
        }
        return result;
    }

    public void close() {
        //受信停止
        try {
            if (bluetoothReceiver != null) {
                bluetoothReceiver.finish();
                //Thread クラスの join() というインスタンスメソッドを呼び出すと、そのインスタンスが表すスレッドが終了するまで (run メソッドを抜けるまで) 待機する。スレッドが終了したら join メソッドから帰ってきて、プログラムの続きを実行する。
                bluetoothReceiver.join();
                bluetoothReceiver = null;
                //ソケットクローズ
                if (bluetoothSock != null) {
                    Log.e(LOG_TITLE, "Socket Close request.");
                    try {
                        bluetoothSock.close();
                    } catch (IOException e) {
                        Log.e(LOG_TITLE, "Socket Close error");
                    }
                    bluetoothSock = null;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TITLE, "Receiver Thread finish error.");
        }
    }

    public boolean sendData(byte[] buf, int len) {
        boolean result = false;
        if ((bluetoothSock != null) && (bluetoothOut != null)) {
            try {
                bluetoothOut.write(buf, 0, len);
                result = true;
            } catch (IOException e) {
                Log.e(LOG_TITLE, "Write data error.");
            }
        }
        return result;
    }

    //* 受信スレッド
    private class ReceiveThread extends Thread {private
        byte[] buffer = new byte[1024];
        //int tempNum = 0;
        private boolean terminate = false;

        private void finish() {
            terminate = true;
        }

        @Override
        public void run() {
            terminate = false;
            Log.e(LOG_TITLE, "ReceiveThread started.");
            while (!terminate) {
                int numRead = readData();
                if (numRead > 0) {
                    //Log.e(LOG_TITLE, "ReceiveThread : READ data. num:" + numRead);
                    listener.lineReceived(new String(buffer, 0, numRead));
                } else {
                    Log.e(LOG_TITLE, "ReceiveThread : READ zero data");
                }
            }
            Log.e(LOG_TITLE, "ReceiveThread : Terminating.");
        }

        //* データの読み込み
        private int readData() {
            int numRead;
            try {
                numRead = bluetoothIn.read(buffer);
            } catch (IOException e) {
                numRead = 0;
                Log.e(LOG_TITLE, "Read Data Exception", e);
                try {
                    Thread.sleep(500, 0);
                } catch (InterruptedException e2) {
                    Log.e(LOG_TITLE, "sleep exception.", e);
                }
            }
            return numRead;
        }
    }
}
//Bluetoothで通信を行う(2) http://techbooster.jpn.org/andriod/device/5535/
//AndroidとPCのBluetooth接続のサンプル　http://www.kotemaru.org/2013/10/30/android-bluetooth-sample.html
//Bluetooth を使う http://www.limy.org/program/android/bluetooth.html
//    public BroadcastReceiver DeviceFoundReceiver = new BroadcastReceiver() {
//        //ACTION_DISCOVERY_STARTED	デバイス検出の開始時
//        //ACTION_FOUND	デバイス検出時
//        //ACTION_NAME_CHANGED	デバイス名の判明時(新規検出時)
//        //ACTION_DISCOVERY_FINISHED	デバイス検出終了時
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            String dName;
//            BluetoothDevice foundDevice;
//            //BluetoothAdapter - Constants  http://d.hatena.ne.jp/esmasui/20091114/1258218707
//            //BluetoothDevice.BOND_BONDING   デバイスは接続中
//            //BluetoothDevice.BOND_BONDED      デバイスは接続履歴あり
//            //BluetoothDevice.BOND_NONE         デバイスは接続履歴なし
//            if ( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action) ) {
//                // スキャン開始のプログレス表示などを行う
//                Log.e("DeviceFoundReceiver", "スキャン開始");
//            }
//            if ( BluetoothDevice.ACTION_FOUND.equals(action) ) {
//                //デバイスが検出された
//                   foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if ( (dName = foundDevice.getName()) != null ) {
//                    Log.e("DeviceFoundReceiver", "ACTION_FOUND: " + dName);
//                    if (foundDevice.getBondState() != BluetoothDevice.BOND_NONE) {
//                        Log.e("DeviceFoundReceiver", "!= BOND_NONE: " + dName);
//                    }
//                }
//            }
//            if ( BluetoothDevice.ACTION_NAME_CHANGED.equals(action) ) {
//                //デバイス名の判明時(新規検出時)
//                foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if ( (dName = foundDevice.getName()) != null ) {
//                    Log.e("DeviceFoundReceiver", "ACTION_NAME_CHANGED: " + dName);
//                    //if ( dName.equals(DEVICE_NAME[0]) ) { //選択したデバイスが接続可能 }
//                    if (foundDevice.getBondState() != BluetoothDevice.BOND_NONE) {
//                        Log.e("DeviceFoundReceiver", "!= BOND_NONE:" + dName);
//                    }
//                }
//            }
//            if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
//                // スキャン終了、表示していたプログレスなどは消す
//                Log.e("DeviceFoundReceiver", "スキャン終了");
//            }
//        }
//    };