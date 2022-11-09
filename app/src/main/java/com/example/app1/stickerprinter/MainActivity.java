package com.example.app1.stickerprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app1.stickerprinter.zebra.ZebraActivity;
import com.example.tscdll.TscWifiActivity;
import com.honeywell.printer.PrinterSdk;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private TextView _curDevIp;
    private Button _buttonFindPrinter;
    private Button _buttonPrintBarcode;
    private Button _buttonSetPrinter, backBtn;

    private Spinner _spinner;
    Map<String, String> _mapPrinterIpName = new HashMap<>();

    int _iLeftEdge = 50;
    int _iTextY = 300;
    int _iTextHeight = 10;
    int _iRightEdge = 590;
    int _iBarcodeY = 10;
    int _iBarcodeEnlarge = 6;
    int _iQREnlarge = 6;

    //PrinterSdk _sdk = new PrinterSdk();
    ArrayList<String> qty = new ArrayList<>();
    String shipCode;
    String toStoreName;
    String toStorePhone;

    RadioGroup rdGrp;
    View tscView, honeyView;
    Button tscPrintBtn, tscBackBtn, /*btnZebraPrinter,*/
            btnTestPrinter;
    EditText tscIp;

    public static String bundleToString(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String string = "Bundle{";
        for (String key : bundle.keySet()) {
            string += " " + key + " => " + bundle.get(key) + ";";
        }
        string += " }Bundle";
        return string;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _curDevIp = (TextView) findViewById(R.id.textView);
        rdGrp = (RadioGroup) findViewById(R.id.radio_grp);
        tscView = findViewById(R.id.tsc_include);
        honeyView = findViewById(R.id.honey_include);
//        btnZebraPrinter = findViewById(R.id.btn_zebra);
        btnTestPrinter = findViewById(R.id.btn_test);


        honeyView.setVisibility(View.GONE);


        getSupportActionBar().setTitle("ALSAIF-EXPRESS");  // provide compatibility to all the versions

        rdGrp.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.tsc_radio_id:
                    System.out.println("TSC Selected");
                    tscView.setVisibility(View.VISIBLE);
                    honeyView.setVisibility(View.GONE);
                    break;
                case R.id.honey_radio_id:
                    System.out.println("Honey Selected");
                    tscView.setVisibility(View.GONE);
                    honeyView.setVisibility(View.VISIBLE);
                    break;
            }
        });


        Intent i = getIntent();
        Bundle shipDataBundle = i.getBundleExtra("ship_data");
        if (shipDataBundle != null) {
            Log.e("Bundle", bundleToString(shipDataBundle));


            qty = shipDataBundle.getStringArrayList("qty");
            shipCode = shipDataBundle.getString("ship_code");
            toStoreName = shipDataBundle.getString("store_name");
            toStorePhone = shipDataBundle.getString("store_phone");
            shipDataBundle.getString("from_name");
            shipDataBundle.getString("from_phone");
            shipDataBundle.getString("to_name");
            shipDataBundle.getString("total");
        }
        try {
            setupTscPrinter();
            setupHoneywellPrinter();
        } catch (Exception e) {
            System.out.println("ErrorPrinterSetup:" + e.getMessage());
        }
//        btnZebraPrinter.setOnClickListener(view -> MainActivity.this.startActivity(new Intent(this, ChooseFormatScreen.class).putExtras(shipDataBundle)));
        if (shipDataBundle != null) {
            btnTestPrinter.setOnClickListener(view -> MainActivity.this.startActivity(new Intent(this, ZebraActivity.class).putExtras(shipDataBundle)));
        } else {
            Toast.makeText(this, "Open Reception App First", Toast.LENGTH_SHORT).show();
        }

        tscPrintBtn.setOnClickListener(v -> {
            //GetIpSharedPref
//                String sharedIP=getIpSharedPref();
//                tscIp.setText(sharedIP);
            String ipAddress = tscIp.getText().toString();
            if (!ipAddress.isEmpty()) {
                new TscConfigs(new TscWifiActivity(), ipAddress, shipDataBundle);
                //SaveIpShared
                //saveIpSharedPref(ipAddress);
                finish();
            }

        });
    }


    void setupTscPrinter() {
        tscPrintBtn = findViewById(R.id.tsc_print);
        tscBackBtn = findViewById(R.id.tsc_back_btn);
        tscIp = findViewById(R.id.tsc_edit_ip);
        tscBackBtn.setOnClickListener(v -> finish());
    }

    void setupHoneywellPrinter() {
        _buttonFindPrinter = (Button) findViewById(R.id.findPrinterId);
        FindPrinterButtonListener buttonListener = new FindPrinterButtonListener();
        _buttonFindPrinter.setOnClickListener(buttonListener);

        _buttonSetPrinter = (Button) findViewById(R.id.setPrinterId);
        _buttonSetPrinter.setOnClickListener(new SetPrinterButtonListener());


        _buttonPrintBarcode = (Button) findViewById(R.id.printBarcodeButton);
        _buttonPrintBarcode.setOnClickListener(new PrintButtonListener());


        _spinner = (Spinner) findViewById(R.id.spinner);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
        //auto find printer
        _buttonFindPrinter.performClick();

    }

    String getIpSharedPref() {
        SharedPreferences sharedpreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return sharedpreferences.getString("ip", "");
    }

    void saveIpSharedPref(String v) {
        SharedPreferences sharedpreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedpreferences.edit();
        edit.putString("ip", v.trim());
        edit.apply();
    }

    static final int MSG_PRINTING = 11115;
    static final int MSG_PRINT_END = 11116;

    static final int MSG_FINDING = 11111;
    static final int MSG_FIND_END = 11112;
    static final int MSG_LOCAL_IP = 11113;
    //refresh UI
    MyHandler _handler = new MyHandler(this);

    public static class MyHandler extends Handler {
        private WeakReference<MainActivity> reference;

        MyHandler(MainActivity activity) {
            reference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                MainActivity activity = reference.get();

                if (activity != null) {
                    switch (msg.what) {
                        case MSG_FINDING:
                            activity._buttonFindPrinter.setText("جاري البحث ...");
                            activity._buttonFindPrinter.setEnabled(false);
                            activity._buttonPrintBarcode.setEnabled(false);
//                            activity._buttonPrintQrCode.setEnabled(false);
                            // init UI
                            activity._curDevIp.setText("0.0.0.0");
                            activity._spinner.setAdapter(null);
                            break;
                        case MSG_FIND_END:
                            activity._buttonFindPrinter.setText("بحث");
                            activity._buttonFindPrinter.setEnabled(true);
                            activity._buttonPrintBarcode.setEnabled(true);
//                            activity._buttonPrintQrCode.setEnabled(true);

                            ArrayList<String> prtNameIpList = new ArrayList<String>();
                            prtNameIpList.addAll(activity._mapPrinterIpName.keySet());
                            ArrayAdapter<String> arr_adapter = new ArrayAdapter<String>(
                                    activity,
                                    android.R.layout.simple_spinner_item, prtNameIpList);
                            arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            activity._spinner.setAdapter(arr_adapter);
                            break;
                        case MSG_LOCAL_IP:
                            String strIp = (String) msg.obj;
                            activity._curDevIp.setText(strIp);
                            break;
                        case MSG_PRINTING:
                            activity._buttonPrintBarcode.setEnabled(false);
//                            activity._buttonPrintBarcode.setText("printting...");
//                            activity._buttonPrintQrCode.setEnabled(false);
//                            activity._buttonPrintQrCode.setText("printting...");
//                            activity._buttonFindPrinter.setEnabled(false);
                            activity._buttonFindPrinter.setText("جاري الطباعة");
                            break;
                        case MSG_PRINT_END:
                            activity._buttonPrintBarcode.setEnabled(true);
//                            activity._buttonPrintBarcode.setText("Print Bar");
//                            activity._buttonPrintQrCode.setEnabled(true);
//                            activity._buttonPrintQrCode.setText("Print QR");
                            activity._buttonFindPrinter.setEnabled(true);
                            activity._buttonFindPrinter.setText("بحث");
                            break;
                        default:
                            // do something...
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //on push find printer button
    class FindPrinterButtonListener implements View.OnClickListener {
        boolean _bFinding = false;

        @Override
        public void onClick(View v) {
            new Thread() {
                @Override
                public void run() {
                    super.run();

                    if (_bFinding) {
                        return;
                    }
                    _bFinding = true;
                    _handler.sendEmptyMessage(MSG_FINDING);
                    _mapPrinterIpName.clear();   //remove printer name

                    String strLocalIp = GetCurrentIp();

                    Map<String, String> mapTemp = PrinterSdk.PRN_FindPrinters(strLocalIp, 60);
                    if (null != mapTemp) {
                        for (String str : mapTemp.keySet()) {
                            _mapPrinterIpName.put(str + "(" + mapTemp.get(str) + ")", str);
                        }
                    }

                    _bFinding = false;
                    _handler.sendEmptyMessage(MSG_FIND_END);

                    System.out.println("find over");
                }
            }.start();

            System.out.println("find end");
        }
    }

    //on push set printer button
    class SetPrinterButtonListener implements View.OnClickListener {
        boolean m_bRunning = false;

        @Override
        public void onClick(View v) {
            if (m_bRunning) {
                return;
            }

            m_bRunning = true;
            v.setEnabled(false);

            if (null == _spinner.getSelectedItem() || 0 == _spinner.getSelectedItem().toString().length())
                return;

            String strIp = _mapPrinterIpName.get(_spinner.getSelectedItem().toString());
            if (null == strIp || 0 == strIp.length()) {
                return;
            }

            sampleSetCfg(strIp);

            m_bRunning = false;
            v.setEnabled(true);
        }
    }


    //on push print button
    class PrintButtonListener implements View.OnClickListener {
        boolean m_bRunning = false;

        @Override
        public void onClick(final View v) {
            new Thread() {
                @Override
                public void run() {
                    if (m_bRunning) {
                        return;
                    }
                    m_bRunning = true;

                    _handler.sendEmptyMessage(MSG_PRINTING);

                    try {
                        String strCurPrinterId = _spinner.getSelectedItem().toString();
                        if (strCurPrinterId.isEmpty()) {
                            return;
                        }

                        String curPrinterIp = _mapPrinterIpName.get(strCurPrinterId);
                        if (null == curPrinterIp) {
                            return;
                        }


                        if (v.getId() == R.id.printBarcodeButton) {

                            for (int i = 0; i < qty.size(); i++) {
                                int productQty = Integer.parseInt(qty.get(i));
                                for (int j = 0; j < productQty; j++) {
                                    SamplePrintText(shipCode,
                                            j + 1,
                                            productQty
                                            , _iBarcodeEnlarge, curPrinterIp,
                                            toStoreName,
                                            toStorePhone
                                    );
                                }
                            }
                            finish();

                        } else {
                            //   SamplePrintQRcode(strEditText, _iQREnlarge, curPrinterIp);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    m_bRunning = false;
                    _handler.sendEmptyMessage(MSG_PRINT_END);
                }
            }.start();
        }
    }


    // get device ip
    private String GetCurrentIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String strIp = intToIp(ipAddress);

        Message msg = Message.obtain();
        msg.what = MSG_LOCAL_IP;
        msg.obj = strIp;
        _handler.sendMessage(msg);

        return strIp;
    }


    //ip address int to str
    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    //if text is chinese
    boolean IsChinese(String strIn) {
        for (int i = 0; i < strIn.length(); i++) {
            String strTemp = strIn.substring(i, i + 1);
            if (java.util.regex.Pattern.matches("[\u4E00-\u9FA5]", strTemp)) {
                return true;
            }
        }
        return false;
    }

    //find printers
    void sampleFindPrinters() {
        String strLocalIp = "192.168.1.5";
        Map<String, String> mapTemp = PrinterSdk.PRN_FindPrinters(strLocalIp, 60);
        if (null != mapTemp) {
            for (String str : mapTemp.keySet()) {
                _mapPrinterIpName.put(str + "(" + mapTemp.get(str) + ")", str);
            }
        }
    }


    //config printer
    void sampleSetCfg(String strIp) {
        PrinterSdk sdk = new PrinterSdk();
        if (0 != sdk.PRN_Connect(strIp)) {
            Log.e("printer", "error");
            return;
        }

        try {
            sdk.PRN_SetCfg(PrinterSdk.PRN_CFG_ID.PRN_CFG_DARKNESS, 3);
            sdk.PRN_SetCfg(PrinterSdk.PRN_CFG_ID.PRN_CFG_MEDIA_TYPE, PrinterSdk.PRN_MEDIA_TYPE.PRN_MEDIA_TYPE_CONTINUOUS_VAR.ordinal());
            sdk.PRN_SetCfg(PrinterSdk.PRN_CFG_ID.PRN_CFG_MEDIA_WIDTH, 640);
            sdk.PRN_WriteConfig();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.PRN_CloseConnect();
        }
    }


    /**
     * //sample of query printer model
     *
     * @return, printer model
     */
    String sampleQueryPrinter(String strIp) {
        PrinterSdk sdk = new PrinterSdk();
        String[] strOut = new String[1];

        //connect printer
        if (0 != sdk.PRN_Connect(strIp)) {
            Log.e("printer", "error");
            return "";
        }

        try {
            sdk.PRN_GetInfoString(PrinterSdk.PRN_INFO_TYPE.PRN_INFO_MODEL_NAME, strOut);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.PRN_CloseConnect();
        }

        return strOut[0];
    }


    /**
     * //sample of print text and barcode
     *
     * @param strText,         text of barcode
     * @param iBarcodeEnlarge, barcode magnification
     */
    void SamplePrintBarcode(String strText, int iBarcodeEnlarge, String strIp) {
        PrinterSdk sdk = new PrinterSdk();

        //connect printer
        if (0 != sdk.PRN_Connect(strIp)) {
            Log.e("printer", "error");
            return;
        }
        try {
            //add text to print
            String strFont = "Andale Mono";
            int iTextX = 20;
            int iTextY = 100;
            sdk.PRN_AddTextToLabelEx(strText,  //text
                    strFont,        //font
                    _iTextHeight,   //text height
                    iTextX,         //text coordinates x
                    iTextY,         //text coordinates y
                    1,          //direction
                    1);         //position of coordinates

            //add barcode to print
            String strBarcodeType;
            int iBarcodeHeight = 0;
            strBarcodeType = "CODE93";
            iBarcodeHeight = 100;

            sdk.PRN_AddBarcodeToLabelEx("jkjkjk",   //text
                    strBarcodeType,                                                 //barcode type
                    iBarcodeHeight,                                                 //bracode height
                    (_iRightEdge + _iLeftEdge) / 2,                           //coordinates x
                    _iBarcodeY,                                                     //coordinates y
                    1,                                                          //direction
                    2,                                                        //the coordinates are the lower middle of the bar code
                    iBarcodeEnlarge,                                                //magnification
                    null);                                                  //using default parameter.

            //print 1 copy
            sdk.PRN_PrintLabel(1);

            sdk.PRN_ClearLabelBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.PRN_CloseConnect();
        }
    }

    void SamplePrintText(String shipCode, int productIndexNumber, int size, int iBarcodeEnlarge, String strIp, String toStoreName, String toStorePhone) {
        PrinterSdk sdk = new PrinterSdk();

        //connect printer
        if (0 != sdk.PRN_Connect(strIp)) {
            Log.e("printer", "error");
            return;
        }
        try {
            //add text to print
            String strFont = "Andale Mono";
            int iTextX = 20;
            int iTextY = 100;

            //add barcode to print
            String strBarcodeType;
            strBarcodeType = "CODE93";

            sdk.PRN_AddTextToLabelEx("ALSAIF-EXPRESS",  //text
                    strFont,        //font
                    25,   //text height
                    100,         //text coordinates x
                    850,         //text coordinates y
                    1,          //direction
                    1);

            //DMM-BARCODE
            sdk.PRN_AddBarcodeToLabelEx(shipCode,   //text
                    strBarcodeType,                                                 //barcode type
                    80,                                                 //bracode height
                    100,                           //coordinates x
                    720,                                                     //coordinates y
                    1,                                                          //direction
                    1,                                                        //the coordinates are the lower middle of the bar code
                    iBarcodeEnlarge,                                                //magnification
                    null);
            //DMM-TEXT
            sdk.PRN_AddTextToLabelEx(shipCode,  //text
                    strFont,        //font
                    20,   //text height
                    300,         //text coordinates x
                    600,         //text coordinates y
                    1,          //direction
                    1);         //position of coordinates


            //ProductData/BarCode
            sdk.PRN_AddBarcodeToLabelEx(shipCode,   //text
                    strBarcodeType,                                                 //barcode type
                    80,                                                 //bracode height
                    50,                           //coordinates x
                    450,                                                     //coordinates y
                    1,                                                          //direction
                    1,                                                        //the coordinates are the lower middle of the bar code
                    iBarcodeEnlarge,                                                //magnification
                    null);
//            //using default parameter.
            sdk.PRN_AddTextToLabelEx(productIndexNumber + " From " + size,  //text
                    strFont,        //font
                    16,   //text height
                    300,         //text coordinates x
                    350,         //text coordinates y
                    1,          //direction
                    1);

            sdk.PRN_AddTextToLabelEx("Store: " + toStoreName,  //text
                    strFont,        //font
                    16,   //text height
                    100,         //text coordinates x
                    250,         //text coordinates y
                    1,          //direction
                    1);
            sdk.PRN_AddTextToLabelEx("TEL: " + toStorePhone,  //text
                    strFont,        //font
                    16,   //text height
                    100,         //text coordinates x
                    150,         //text coordinates y
                    1,          //direction
                    1);//position of coordinates

            //print 1 copy
            sdk.PRN_PrintLabel(1);

            sdk.PRN_ClearLabelBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.PRN_CloseConnect();
        }
    }

//    void SamplePrintText(String shipCode,int productIndexNumber,int size, int iBarcodeEnlarge, String strIp){
//        PrinterSdk sdk = new PrinterSdk();
//
//        //connect printer
//        if (0 != sdk.PRN_Connect(strIp)) {
//            Log.e("printer", "error");
//            return;
//        }
//        try {
//            //add text to print
//            String strFont = "Andale Mono";
//            int iTextX = 20;
//            int iTextY = 100;
//
//            //add barcode to print
//            String strBarcodeType;
//            strBarcodeType = "CODE93";
//
////            sdk.PRN_AddTextToLabelEx("ALSAIF-EXPRESS",  //text
////                    strFont,        //font
////                    16,   //text height
////                    300,         //text coordinates x
////                    250,         //text coordinates y
////                    1,          //direction
////                    1);
//            sdk.PRN_AddBarcodeToLabelEx(shipCode,   //text
//                    strBarcodeType,                                                 //barcode type
//                    80,                                                 //bracode height
//                    300,                           //coordinates x
//                    250,                                                     //coordinates y
//                    1,                                                          //direction
//                    2,                                                        //the coordinates are the lower middle of the bar code
//                    iBarcodeEnlarge,                                                //magnification
//                    null);
//                    //using default parameter.
//
//            sdk.PRN_AddTextToLabelEx(shipCode,  //text
//                    strFont,        //font
//                    20,   //text height
//                    300,         //text coordinates x
//                    170,         //text coordinates y
//                    1,          //direction
//                    1);         //position of coordinates
//
//
//            //ProductData
//
//            sdk.PRN_AddBarcodeToLabelEx(shipCode,   //text
//                    strBarcodeType,                                                 //barcode type
//                    50,                                                 //bracode height
//                    300,                           //coordinates x
//                    80,                                                     //coordinates y
//                    1,                                                          //direction
//                    2,                                                        //the coordinates are the lower middle of the bar code
//                    iBarcodeEnlarge,                                                //magnification
//                    null);
////            //using default parameter.
////
//            sdk.PRN_AddTextToLabelEx(productIndexNumber+" From "+size,  //text
//                    strFont,        //font
//                    16,   //text height
//                    300,         //text coordinates x
//                    30,         //text coordinates y
//                    1,          //direction
//                    1);         //position of coordinates
//
//            //print 1 copy
//            sdk.PRN_PrintLabel(1);
//
//            sdk.PRN_ClearLabelBuffer();
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        finally {
//            sdk.PRN_CloseConnect();
//        }
//    }

    /**
     * sample of print text and QR code
     *
     * @param strText
     * @param iQREnlarge
     */
    void SamplePrintQRcode(String strText, int iQREnlarge, String strIp) {
        PrinterSdk sdk = new PrinterSdk();

        //connect printer
        if (0 != sdk.PRN_Connect(strIp)) {
            Log.e("printer", "error");
            return;
        }
        try {
            //add text to print
            String strFont = "Andale Mono";
            int iTextX = 20;
            int iTextY = 100;
            sdk.PRN_AddTextToLabelEx(strText,  //text
                    strFont,                    //font
                    _iTextHeight,               //text height
                    iTextX,                     //text coordinates x
                    iTextY,                     //text coordinates y
                    1,                      //direction
                    1);                     //position of coordinates

            //add barcode to print
            String strBarcodeType;
            int iBarcodeHeight = 0;
            strBarcodeType = "QRCODE";
            iBarcodeHeight = 2;

            int[] adv = new int[12];
            adv[0] = 2; // adv[0] is secrity level.    (default is 1)
            adv[1] = 2; // adv[1] is model.            (default is 2)
            sdk.PRN_AddBarcodeToLabelEx("klkl", //text
                    strBarcodeType,                                 //barcode type
                    iBarcodeHeight,                                 //barcode height
                    (_iRightEdge + _iLeftEdge) / 2,         //coordinates x
                    _iBarcodeY,                                     //coordinates y
                    1,                                          //direction
                    2,                                      //the coordinates are the lower middle of the bar code
                    iQREnlarge,                                     //magnification
                    adv);                                       //Advanced parameters

            //print 1 copy
            sdk.PRN_PrintLabel(1);

            sdk.PRN_ClearLabelBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sdk.PRN_CloseConnect();
        }
    }

    //print
    void SamplePrintImage(String strIp) {
        PrinterSdk sdk = new PrinterSdk();
        sdk.PRN_Connect(strIp);
        sdk.PRN_IndBarcodeFont("Andale Mono", 15, 10, 10);
        sdk.PRN_AddTextToLabel("aaa", _iLeftEdge, _iBarcodeY);
        sdk.PRN_AddBarcodeToLabel("xxxx",
                "QRCODE",
                2,
                33,
                333,
                1,
                1,
                6);
        String strPath = Environment.getExternalStorageDirectory().getPath();
        strPath += "\\test.bmp";
        try {
            File f = new File(strPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sdk.PRN_AddImageToLabel(strPath, 2, 2);
        sdk.PRN_PrintLabel(1);
        sdk.PRN_CloseConnect();
    }

    //get printer status
    int SampleGetPrinterStatus(String strIp) {
        PrinterSdk sdk = new PrinterSdk();
        sdk.PRN_Connect(strIp);
        int[] iRet = {0};
        int iResult = sdk.PRN_GetPrinterStatus(iRet);
        sdk.PRN_CloseConnect();

        return iResult;
    }

}
