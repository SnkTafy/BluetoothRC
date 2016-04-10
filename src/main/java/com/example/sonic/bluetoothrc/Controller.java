package com.example.sonic.bluetoothrc;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Controller extends Activity{
    private ViewGroup lrLayout, udLayout;
    private ImageView lrImage, udImage, udImageBackgound, lrImageBackground;
    private TextView forwardText, backwardText, leftText, rightText, versionText, connectedToText;
    private Button backButton;
    private final static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Text font
    private Typeface customFont;
    private int finalPositionFlag=0, connectionStatusFlag = 0;
    private int dx, dy, xStartPos, yStartPos;
    private String deviceName, upDownData, leftRightData;
    private BluetoothAdapter mBluetoothAdapter;
    RelativeLayout.LayoutParams lrParms,udParms;
    private ConnectingThread t;
    private ConnectedThread connectedTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        getMacAddressAndStartConnection();
        // Setting the application to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Getting the left right relative layouts, image and background
        lrLayout = (RelativeLayout)findViewById(R.id.leftrightlayout);
        lrImage = (ImageView)findViewById(R.id.lr_image);
        lrImageBackground = (ImageView)findViewById(R.id.lrButtonPlace);
        //Getting up down relative layout, image and background
        udLayout = (RelativeLayout)findViewById(R.id.udlayout);
        udImage = (ImageView)findViewById(R.id.ud_image);
        udImageBackgound = (ImageView)findViewById(R.id.udButtonPlace);

        getTextViewsAndSetFonts();
        connectedToText.setText("Connected to:\n" + deviceName);

        t.connectionStatus();

        // Back button listener
        backButton.setOnTouchListener(backButtonListener());
        // Setting listener to left right button
        lrImage.setOnTouchListener(leftRightTouchListener());
        // Setting listener to up down button
        udImage.setOnTouchListener(upDownTouchListener());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //connectedTo.write("U0\n");
        //connectedTo.write("L0\n");
        try{
            t.getBluetoothSocket().close();
        }catch (IOException e) {}
    }


    private void customToastFont(Toast toast){
        LinearLayout toastLayout = (LinearLayout) toast.getView();
        TextView toastTV = (TextView) toastLayout.getChildAt(0);
        toastTV.setTypeface(customFont);
        toast.show();
    }

    private View.OnTouchListener backButtonListener(){
        return new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        backButton.setVisibility(View.INVISIBLE);
                        ((Button)findViewById(R.id.backButtonAfterClick)).setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        backButton.setVisibility(View.VISIBLE);
                        ((Button)findViewById(R.id.backButtonAfterClick)).setVisibility(View.INVISIBLE);
                        // Try to close the bluetooth connection
                        try{
                            (t.getBluetoothSocket()).close();
                            Toast disconnected = Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT);
                            customToastFont(disconnected);
                        }catch (IOException e) { }
                        finish();
                        break;
                }
                udLayout.invalidate();
                return true;
            }
        };
    }

    private void getMacAddressAndStartConnection(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String mac = ((Bundle)getIntent().getExtras()).getString("MAC");

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);
        deviceName = bluetoothDevice.getName();

        t = new ConnectingThread(bluetoothDevice);
        t.start();
    }

    // Getting textviews and setting my own font
    private void getTextViewsAndSetFonts(){
        customFont = Typeface.createFromAsset(getAssets(), "fonts/coolvetica.ttf");
        forwardText = (TextView)findViewById(R.id.udForwardText);
        forwardText.setTypeface(customFont);
        backwardText = (TextView)findViewById(R.id.udBackwardText);
        backwardText.setTypeface(customFont);
        leftText = (TextView)findViewById(R.id.lrLeftText);
        leftText.setTypeface(customFont);
        rightText = (TextView)findViewById(R.id.lrRightText);
        rightText.setTypeface(customFont);
        versionText = (TextView)findViewById(R.id.version);
        versionText.setTypeface(customFont);
        connectedToText = (TextView)findViewById(R.id.connectToText);
        connectedToText.setTypeface(customFont);
        backButton = (Button)findViewById(R.id.backButtonBeforeClick);
        // All text invisible
        forwardText.setVisibility(View.INVISIBLE);
        backwardText.setVisibility(View.INVISIBLE);
        leftText.setVisibility(View.INVISIBLE);
        rightText.setVisibility(View.INVISIBLE);
    }

    private class ConnectingThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectingThread(BluetoothDevice device) {

            BluetoothSocket temp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
        }

        public BluetoothSocket getBluetoothSocket(){
            return bluetoothSocket;
        }

        public void run() {
            // Cancel discovery as it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
            // This will block until it succeeds in connecting to the device
            // through the bluetoothSocket or throws an exception
                bluetoothSocket.connect();
                connectionStatusFlag = 1;
            } catch (IOException connectException) {
                connectionStatusFlag = 2;
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            connectedTo = new ConnectedThread(bluetoothSocket);
            connectedTo.start();
        }

        public void connectionStatus(){
            while(true)
            {
                if(connectionStatusFlag == 1){
                    Toast connected = Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT);
                    customToastFont(connected);
                    break;
                } else if(connectionStatusFlag == 2){
                    Toast noConnect = Toast.makeText(getApplicationContext(), "Can not connect to selected device", Toast.LENGTH_SHORT);
                    customToastFont(noConnect);
                    finish();
                    break;
                }
            }
        }

        // Cancel an open connection and terminate the thread
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ConnectedThread extends Thread{
        private final BluetoothSocket blueSocket;
        private final InputStream blueInStream;
        private final OutputStream blueOutStream;
        private static final String TAG = "ConnectedThread";
        private static final boolean D = true;

        public ConnectedThread(BluetoothSocket socket){
            blueSocket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e){  }

            blueInStream = tmpIn;
            blueOutStream = tmpOut;
        }

        public void run(){
            if(D) Log.e(TAG, "-- ConnectedThread --");

            byte[] buffer = new byte[1024]; //buffer store for the stream
            int bytes; // bytes returned by read
            try {
                while ((bytes = blueInStream.read(buffer)) != -1) {

                    if (D) Log.e(TAG, "ConnectedThread while(true)");
                    blueOutStream.write(bytes);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(String msg) {
            byte[] msgBuffer = msg.getBytes();

            try {
                blueOutStream.write(msgBuffer);
            } catch (IOException e) { }
            try{
                blueOutStream.flush();
            }catch (IOException e){}
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                blueSocket.close();
            } catch (IOException e) { }
        }
    }

    private View.OnTouchListener leftRightTouchListener(){
        return new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        //This part of code will be executed only once when the button is touched
                        lrParms = (RelativeLayout.LayoutParams) lrImage.getLayoutParams();
                        dx = (int)(event.getRawX() - lrParms.leftMargin);
                        // Getting the start position of the button
                        xStartPos = (int)event.getRawX() - dx;
                        break;
                    case MotionEvent.ACTION_UP:
                        // This part of code will be executed only once when the finger is removed from the button
                        // Set the button to the start position
                        lrParms.leftMargin = xStartPos;
                        lrImage.setLayoutParams(lrParms);
                        // When remove the button from the button set all the texts to invisible
                        leftText.setVisibility(View.INVISIBLE);
                        rightText.setVisibility(View.INVISIBLE);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // This part of code will be executed while the button is touched
                        // Checking if the current button position is bigger than background image width / 2
                        // e.g.: Current position: 230, background image width: 400.  So if 230 > (400/2)
                        if(Math.abs(xStartPos - (event.getRawX() - dx)) > ((lrImageBackground.getWidth()/ 2) - (lrImage.getWidth()/2))){
                            // If the above statement is true do not get new position.
                            // Checking if button pushed left or right
                            // If the button pushed left
                            if((event.getRawX() - dx) < xStartPos){
                                // This is the final left position
                                lrParms.leftMargin = (xStartPos - ((lrImageBackground.getWidth()/ 2) - (lrImage.getWidth()/2)));
                                // Sending to bluetooth the final poslition
                                connectedTo.write("L" + ((lrImageBackground.getWidth()/ 2) - (lrImage.getWidth()/2)) + "\n");
                            }
                            // If the button pushed right
                            else{
                                // This is the final right position
                                lrParms.leftMargin = (xStartPos + ((lrImageBackground.getWidth()/ 2) - (lrImage.getWidth()/2)));
                                connectedTo.write("R" + Math.abs((lrImageBackground.getWidth() / 2) - (lrImage.getWidth() / 2)) + "\n");
                            }
                        }else{
                            // Sending to bluetooth the current position
                            if((event.getRawX() - dx) < xStartPos){
                                connectedTo.write("L" + (int)(xStartPos - (event.getRawX() - dx)) + "\n");
                            }else if((event.getRawX() - dx) == xStartPos){
                                connectedTo.write("L0\n");
                            }else{
                                connectedTo.write("R" + (int)Math.abs(xStartPos - (event.getRawX() - dx)) + "\n");
                            }

                            // Getting new putton position
                            lrParms.leftMargin = (int)(event.getRawX() - dx);
                        }
                        // Set the right or left text to visible or invisible
                        if((event.getRawX() - dx) < xStartPos){
                            rightText.setVisibility(View.INVISIBLE);
                            leftText.setVisibility(View.VISIBLE);
                        }else if((event.getRawX() - dx) == xStartPos){
                            leftText.setVisibility(View.INVISIBLE);
                            rightText.setVisibility(View.INVISIBLE);
                        }else{
                            leftText.setVisibility(View.INVISIBLE);
                            rightText.setVisibility(View.VISIBLE);
                        }
                        // Setting the new positions
                        lrImage.setLayoutParams(lrParms);
                        break;
                }
                // ReDraw the relative layout with the new button positions
                lrLayout.invalidate();
                return true;
            }
        };
    }

    private View.OnTouchListener upDownTouchListener(){
        return new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        udParms = (RelativeLayout.LayoutParams) udImage.getLayoutParams();
                        if(finalPositionFlag == 0) {
                            dy = (int) (event.getRawY() - udParms.topMargin);
                            yStartPos = (int) event.getRawY() - dy;
                            finalPositionFlag = 1;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        //udParms.topMargin = yStartPos;
                        //udImage.setLayoutParams(udParms);
                        //forwardText.setVisibility(View.INVISIBLE);
                        //backwardText.setVisibility(View.INVISIBLE);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(Math.abs(yStartPos - (event.getRawY() - dy)) > ((udImageBackgound.getHeight()/ 2) - (udImage.getHeight()/2))){
                            if((event.getRawY() - dy) < yStartPos){
                                udParms.topMargin = (yStartPos - ((udImageBackgound.getHeight()/ 2) - (udImage.getHeight()/2)));
                                connectedTo.write("U" + ((udImageBackgound.getHeight() / 2) - (udImage.getHeight() / 2)) + "\n");

                            }
                            else{
                                udParms.topMargin = (yStartPos + ((udImageBackgound.getHeight()/ 2) - (udImage.getHeight()/2)));
                                connectedTo.write("D" + Math.abs((udImageBackgound.getHeight() / 2) - (udImage.getHeight() / 2)) + "\n");
                            }
                        }else{
                            if(event.getRawY() - dy < yStartPos){
                                connectedTo.write("U" + Integer.toString((int) (yStartPos - (event.getRawY() - dy))) + "\n");
                            }else if(event.getRawY() - dy == yStartPos){
                                connectedTo.write("U0\n");
                            }else{
                                connectedTo.write("D" + Integer.toString((int)Math.abs(yStartPos - (event.getRawY() - dy))) + "\n");
                            }

                            udParms.topMargin = (int)(event.getRawY() - dy);
                        }
                        if(event.getRawY() - dy < yStartPos){
                            backwardText.setVisibility(View.INVISIBLE);
                            forwardText.setVisibility(View.VISIBLE);
                        }else if(event.getRawY() - dy == yStartPos){
                            forwardText.setVisibility(View.INVISIBLE);
                            backwardText.setVisibility(View.INVISIBLE);
                        }else{
                            forwardText.setVisibility(View.INVISIBLE);
                            backwardText.setVisibility(View.VISIBLE);
                        }
                        udImage.setLayoutParams(udParms);
                        break;
                }
                udLayout.invalidate();
                return true;
            }
        };
    }
}

