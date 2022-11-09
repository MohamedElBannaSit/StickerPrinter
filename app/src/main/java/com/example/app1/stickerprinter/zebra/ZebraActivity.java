/**
 * ********************************************
 * CONFIDENTIAL AND PROPRIETARY
 * <p/>
 * The source code and other information contained herein is the confidential and the exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published,
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 * <p/>
 * Copyright ZIH Corp. 2015
 * <p/>
 * ALL RIGHTS RESERVED
 * *********************************************
 */


package com.example.app1.stickerprinter.zebra;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app1.stickerprinter.MainActivity;
import com.example.app1.stickerprinter.R;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import java.util.ArrayList;

public class ZebraActivity extends AppCompatActivity {

    private static final String TAG = "ZebraActivity";
    private Connection connection;

    private RadioButton btRadioButton;
    private EditText macAddressEditText;
    private EditText ipAddressEditText;
    private EditText portNumberEditText;
    private static final String bluetoothAddressKey = "ZEBRA_DEMO_BLUETOOTH_ADDRESS";
    private static final String tcpAddressKey = "ZEBRA_DEMO_TCP_ADDRESS";
    private static final String tcpPortKey = "ZEBRA_DEMO_TCP_PORT";
    private static final String PREFS_NAME = "OurSavedAddress";

    private Button testButton;
    private ZebraPrinter printer;
    private TextView statusField;

    Bundle shipDataBundle;
    ArrayList<Integer> shipmentQty;
    String shipmentShip_code;
    String shipmentStore_name;
    String shipmentStore_phone;
    String shipmentFrom_name;
    String shipmentFrom_phone;
    String shipmentTo_name;
    String shipmentTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zebra);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        initBundleData();

        ipAddressEditText = (EditText) this.findViewById(R.id.ipAddressInput);
        String ip = settings.getString(tcpAddressKey, "");
        ipAddressEditText.setText(ip);

        portNumberEditText = (EditText) this.findViewById(R.id.portInput);
        String port = settings.getString(tcpPortKey, "");
        portNumberEditText.setText(port);

        macAddressEditText = (EditText) this.findViewById(R.id.macInput);
        String mac = settings.getString(bluetoothAddressKey, "");
        macAddressEditText.setText(mac);

//        TextView t2 = (TextView) findViewById(R.id.launchpad_link);
//        t2.setMovementMethod(LinkMovementMethod.getInstance());

        statusField = (TextView) this.findViewById(R.id.statusText);


        btRadioButton = (RadioButton) this.findViewById(R.id.bluetoothRadio);


        RadioGroup radioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bluetoothRadio) {
                    toggleEditField(macAddressEditText, true);
                    toggleEditField(portNumberEditText, false);
                    toggleEditField(ipAddressEditText, false);
                } else {
                    toggleEditField(portNumberEditText, true);
                    toggleEditField(ipAddressEditText, true);
                    toggleEditField(macAddressEditText, false);
                }
            }
        });
        testButton = (Button) this.findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new Thread(() -> {
                    enableTestButton(false);
                    if (shipDataBundle != null)
                        doConnectionTest();
                }).start();
            }
        });


    }

    private void initBundleData() {
        shipDataBundle = getIntent().getExtras();
        if (shipDataBundle != null) {
            shipmentQty = shipDataBundle.getIntegerArrayList("qty");
            shipmentShip_code = shipDataBundle.getString("ship_code");
            shipmentStore_name = shipDataBundle.getString("store_name");
            shipmentStore_phone = shipDataBundle.getString("store_phone");
            shipmentFrom_name = shipDataBundle.getString("from_name");
            shipmentFrom_phone = shipDataBundle.getString("from_phone");
            shipmentTo_name = shipDataBundle.getString("to_name");
            shipmentTotal = shipDataBundle.getString("total");
            shipDataBundle.getStringArrayList("qty");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public ZebraPrinter connect() {
        setStatus("Connecting...", Color.YELLOW);
        connection = null;
        if (isBluetoothSelected()) {
            connection = new BluetoothConnection(getMacAddressFieldText());
            SettingsHelper.saveBluetoothAddress(this, getMacAddressFieldText());
        } else {
            try {
                int port = Integer.parseInt(getTcpPortNumber());
                connection = new TcpConnection(getTcpAddress(), port);
                SettingsHelper.saveIp(this, getTcpAddress());
                SettingsHelper.savePort(this, getTcpPortNumber());
            } catch (NumberFormatException e) {
                setStatus("Port Number Is Invalid", Color.RED);
                return null;
            }
        }

        try {
            connection.open();
            setStatus("Connected", Color.GREEN);
        } catch (ConnectionException e) {
            setStatus("Comm Error! Disconnecting", Color.RED);
            DemoSleeper.sleep(1000);
            disconnect();
        }

        ZebraPrinter printer = null;

        if (connection.isConnected()) {
            try {

                printer = ZebraPrinterFactory.getInstance(connection);
                setStatus("Determining Printer Language", Color.YELLOW);
                String pl = SGD.GET("device.languages", connection);
                setStatus("Printer Language " + pl, Color.BLUE);
            } catch (ConnectionException e) {
                setStatus("Unknown Printer Language", Color.RED);
                printer = null;
                DemoSleeper.sleep(1000);
                disconnect();
            } catch (ZebraPrinterLanguageUnknownException e) {
                setStatus("Unknown Printer Language", Color.RED);
                printer = null;
                DemoSleeper.sleep(1000);
                disconnect();
            }
        }

        return printer;
    }

    public void disconnect() {
        try {
            setStatus("Disconnecting", Color.RED);
            if (connection != null) {
                connection.close();
            }
            setStatus("Not Connected", Color.RED);
        } catch (ConnectionException e) {
            setStatus("COMM Error! Disconnected", Color.RED);
        } finally {
            enableTestButton(true);
        }
    }

    private void setStatus(final String statusMessage, final int color) {
        runOnUiThread(new Runnable() {
            public void run() {
                statusField.setBackgroundColor(color);
                statusField.setText(statusMessage);
            }
        });
        DemoSleeper.sleep(1000);
    }


    private void sendTestLabel(byte[] lableDesign) {

        try {
            ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);

            PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();

            if (printerStatus.isReadyToPrint) {

                connection.write(lableDesign);
//                for (int i = 0; i < shipmentQty.size(); i++) {
//                    int productQty = shipmentQty.get(i);
//                    for (int j = 0; j < productQty; j++) {
//                        connection.write(lableDesign);
//                    }
//                }
                setStatus("Sending Data", Color.BLUE);
            } else if (printerStatus.isHeadOpen) {
                setStatus("Printer Head Open", Color.RED);
            } else if (printerStatus.isPaused) {
                setStatus("Printer is Paused", Color.RED);
            } else if (printerStatus.isPaperOut) {
                setStatus("Printer Media Out", Color.RED);
            }
            DemoSleeper.sleep(1500);

//            if (connection instanceof BluetoothConnection) {
//                String friendlyName = ((BluetoothConnection) connection).getFriendlyName();
//                setStatus(friendlyName, Color.MAGENTA);
//                DemoSleeper.sleep(500);
//            }
        } catch (ConnectionException e) {
            setStatus(e.getMessage(), Color.RED);
        } finally {
            disconnect();
        }
    }

    private void enableTestButton(final boolean enabled) {
        runOnUiThread(new Runnable() {
            public void run() {
                testButton.setEnabled(enabled);
            }
        });
    }
    private void doConnectionTest() {
        printer = connect();

        if (printer != null) {
            sendTestLabel(("^XA\n" +
                    "^A2N,50,50,B:TT0003M_.TTF\n"+
                    "^FO100,200\n" +
                    "^PA1,1,1,1\n" +
                    "^BCN,100,Y,N,N\n"+
                    "^FD" + shipmentShip_code + "^FS\n" +
                    "^A@N,50,50,E:TT0003M_.TTF^CI28^FB" + shipmentTo_name + "^FS\n" +
                    "^A@N,50,50,E:TT0003M_.TTF^CI28^FB" + shipmentStore_name + "^FS\n" +
                    "^PQ" + 2 + "\n" +
                    "^XZ").getBytes());
        } else {
            disconnect();
        }
    }

    private byte[] getConfigLabel(int productQty) {
        byte[] configLabel = null;
        try {
            PrinterLanguage printerLanguage = printer.getPrinterControlLanguage();
            SGD.SET("device.languages", "zpl", connection);

            if (printerLanguage == PrinterLanguage.ZPL) {

//                String s = "^^XA\n" +
//                        "^FO50,50\n" +
//                        "^PA1,1,1,1\n" +
//                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentStore_name + "^FS\n" +
//                        "^XZ";

//                String shipmentShip_code;
//                String shipmentStore_name;
//                String shipmentStore_phone;
//                String shipmentFrom_name;
//                String shipmentFrom_phone;
//                String shipmentTo_name;
//                String shipmentTotal;

                String s = "^XA\n" +
                        "^DFR:SAMPLE.GRF^FS\n" +
                        "^FO20,30^GB750,1100,4^FS\n" +
                        "^FO20,30^GB750,200,4^FS\n" +
                        "^FO20,30^GB750,400,4^FS\n" +
                        "^FO20,30^GB750,700,4^FS\n" +
                        "^FO20,226^GB325,204,4^FS\n" +
                        "^FO30,40^ADN,36,20^FDShip to:^FS\n" +
                        "^FO30,260^ADN,18,10^FDPart number #^FS\n" +
                        "^FO360,260^ADN,18,10^FDDescription:^FS\n" +
                        "^FO30,750^ADN,36,20^FDFrom:^FS\n" +
                        "^FO150,125^ADN,36,20^FN1^FS (ship to)\n" +
                        "^FO60,330^ADN,36,20^FN2^FS(part num)\n" +
                        "^FO400,330^ADN,36,20^FN3^FS(description)\n" +
                        "^FO70,480^BY4^B3N,,200^FN4^FS(barcode)\n" +
                        "^FO150,800^ADN,36,20^FN5^FS (from)\n" +
                        "^XZ\n" +
                        "^XA\n" +
                        "^XFR:SAMPLE.GRF\n" +
                        "^FN1^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentTotal + "^FS\n" +
                        "^FN2^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentShip_code + "^FS\n" +
                        "^FN3^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentStore_name + "^FS\n" +
                        "^FN4^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentShip_code + "^FS\n" +
                        "^FN5^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentTo_name + "^FS\n" +
                        "^PQ" + productQty + "\n" +
                        "^XZ";

                s = "^XA\n" +
                        "^FO100,100\n" +
                        "^PA1,1,1,1\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentTo_name + "^FS\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentShip_code + "^FS\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentStore_name + "^FS\n" +
                        "^XZ";

                s = "^XA\n" +
                        "^FO50,50\n" +
                        "^PA1,1,1,1\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentShip_code + "^FS\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentTo_name + "^FS\n" +
                        "^A@N,50,50,E:TT0003M_.TTF^CI28^FD" + shipmentStore_name + "^FS\n" +
                        "^XZ";


                configLabel = s.getBytes();
            } else if (printerLanguage == PrinterLanguage.CPCL) {
                String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "T 0 0 137 177 \r\n" + shipmentShip_code + "T 0 0 137 177 Express\r\n" + "PRINT\r\n";
                configLabel = cpclConfigLabel.getBytes();
            }
        } catch (ConnectionException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return configLabel;
    }

    private void toggleEditField(EditText editText, boolean set) {
        /*
         * Note: Disabled EditText fields may still get focus by some other means, and allow text input.
         *       See http://code.google.com/p/android/issues/detail?id=2771
         */
        editText.setEnabled(set);
        editText.setFocusable(set);
        editText.setFocusableInTouchMode(set);
    }

    private boolean isBluetoothSelected() {
        return btRadioButton.isChecked();
    }

    private String getMacAddressFieldText() {
        return macAddressEditText.getText().toString();
    }

    private String getTcpAddress() {
        return ipAddressEditText.getText().toString();
    }

    private String getTcpPortNumber() {
        return portNumberEditText.getText().toString();
    }


}
