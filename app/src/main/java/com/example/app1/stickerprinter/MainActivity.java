package com.example.app1.stickerprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app1.stickerprinter.zebra.DemoSleeper;
import com.example.app1.stickerprinter.zebra.chooseprinter.PrinterConnectionDialog;
import com.example.tscdll.TscWifiActivity;
import com.honeywell.printer.PrinterSdk;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    static final int MSG_PRINTING = 11115;
    static final int MSG_PRINT_END = 11116;
    static final int MSG_FINDING = 11111;
    static final int MSG_FIND_END = 11112;
    static final int MSG_LOCAL_IP = 11113;
    private static final String TAG = "MainActivity";
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

    String shipmentFrom_name;
    String shipmentFrom_phone;
    String shipmentTo_name;
    String shipmentTo_phone;
    String shipmentTotal;

    RadioGroup rdGrp;
    View tscView, honeyView, zebraView;
    Button tscPrintBtn, tscBackBtn;
    EditText tscIp;
    PrinterConnectionDialog mPrinterConnectionDialog;
    Bundle shipDataBundle;
    //refresh UI
    MyHandler _handler = new MyHandler(this);
    private TextView _curDevIp;
    private Button _buttonFindPrinter;
    private Button _buttonPrintBarcode;
    private Button _buttonSetPrinter, backBtn;
    private Spinner _spinner;
    private Connection connection;
    private ZebraPrinter printer;
    private EditText ipAddressEditText;
    private EditText portNumberEditText;
    private Button testButton;
    private TextView statusField;

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

        _curDevIp = findViewById(R.id.textView);
        rdGrp = findViewById(R.id.radio_grp);
        tscView = findViewById(R.id.tsc_include);
        honeyView = findViewById(R.id.honey_include);
        zebraView = findViewById(R.id.zebra_include);
//        btnZebraPrinter = findViewById(R.id.btn_zebra);
//        btnTestPrinter = findViewById(R.id.btn_test);


        tscView.setVisibility(View.GONE);
        honeyView.setVisibility(View.GONE);


        getSupportActionBar().setTitle("ALSAIF-EXPRESS");  // provide compatibility to all the versions

        rdGrp.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.tsc_radio_id:
                    System.out.println("TSC Selected");
                    tscView.setVisibility(View.VISIBLE);
                    honeyView.setVisibility(View.GONE);
                    zebraView.setVisibility(View.GONE);
                    break;
                case R.id.honey_radio_id:
                    System.out.println("Honey Selected");
                    tscView.setVisibility(View.GONE);
                    zebraView.setVisibility(View.GONE);
                    honeyView.setVisibility(View.VISIBLE);
                    break;
                case R.id.zebra_radio_id:
                    System.out.println("Zebra Selected");
                    tscView.setVisibility(View.GONE);
                    honeyView.setVisibility(View.GONE);
                    zebraView.setVisibility(View.VISIBLE);
                    break;
            }
        });


        Intent i = getIntent();
        shipDataBundle = i.getBundleExtra("ship_data");

        if (shipDataBundle != null) {


            qty = shipDataBundle.getStringArrayList("qty");
            shipCode = shipDataBundle.getString("ship_code");
            toStoreName = shipDataBundle.getString("store_name");
            toStorePhone = shipDataBundle.getString("store_phone");
            shipmentFrom_name = shipDataBundle.getString("from_name");
            shipmentFrom_phone = shipDataBundle.getString("from_phone");
            shipmentTo_name = shipDataBundle.getString("to_name");
            shipmentTo_phone = shipDataBundle.getString("to_phone");
            shipmentTotal = shipDataBundle.getString("total");

            Log.e("Bundle", bundleToString(shipDataBundle));
        }

        try {
            setupTscPrinter();
            setupHoneywellPrinter();
            setupZebraPrinter();
        } catch (Exception e) {
            System.out.println("ErrorPrinterSetup:" + e.getMessage());
        }

//        btnZebraPrinter.setOnClickListener(view -> MainActivity.this.startActivity(new Intent(this, ChooseFormatScreen.class).putExtras(shipDataBundle)));
//        if (shipDataBundle != null) {
//            btnTestPrinter.setOnClickListener(view -> MainActivity.this.startActivity(new Intent(this, ZebraActivity.class).putExtras(shipDataBundle)));
//        } else {
//            Toast.makeText(this, "Open Reception App First", Toast.LENGTH_SHORT).show();
//        }

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

    private void setupZebraPrinter() {
        mPrinterConnectionDialog = new PrinterConnectionDialog();

        ipAddressEditText = this.findViewById(R.id.ipAddressInput);
        ipAddressEditText.setText("192.168.5.194");

        portNumberEditText = this.findViewById(R.id.portInput);
        portNumberEditText.setText("9100");


        statusField = this.findViewById(R.id.statusText);


        testButton = this.findViewById(R.id.testButton);
        testButton.setOnClickListener(v -> new Thread(() -> {
            enableTestButton(false);
            if (shipDataBundle != null)
                doConnectionTest();
        }).start());

        ImageButton findPrinterButton = this.findViewById(R.id.search_printer);
        findPrinterButton.setOnClickListener(view ->
                mPrinterConnectionDialog.show(getFragmentManager(), TAG));
    }

    public ZebraPrinter connect() {
        setStatus("Connecting...", Color.YELLOW);
        connection = null;

        try {
            int port = Integer.parseInt(portNumberEditText.getText().toString());
            connection = new TcpConnection(ipAddressEditText.getText().toString(), port);

        } catch (NumberFormatException e) {
            setStatus("Port Number Is Invalid", Color.RED);
            return null;
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

                setStatus("Sending Data", Color.BLUE);
            } else if (printerStatus.isHeadOpen) {
                setStatus("Printer Head Open", Color.RED);
            } else if (printerStatus.isPaused) {
                setStatus("Printer is Paused", Color.RED);
            } else if (printerStatus.isPaperOut) {
                setStatus("Printer Media Out", Color.RED);
            }
            DemoSleeper.sleep(1500);
        } catch (ConnectionException e) {
            setStatus(e.getMessage(), Color.RED);
        } finally {
            disconnect();
        }
    }

    private void enableTestButton(final boolean enabled) {
        runOnUiThread(() -> testButton.setEnabled(enabled));
    }

    private void doConnectionTest() {
        printer = connect();
        if (printer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                shipDataBundle.getIntegerArrayList("qty").forEach(s -> {
                    int productQty = s;
                    for (int j = 0; j < productQty; j++) {
                        sendTestLabel(("^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR5,5~SD15^JUS^LRN^CI0^XZ\n" +
                                "~DG000.GRF,05376,028,\n" +
                                ",::::::::::::::::::::::::::::gH0H1H510,gG08AJAH80,Y0H5M754,Y0RA0,V015DSD,W0UA80,U017H757R74,U02ALAI02ALA,T05FDJD510H0H15DIDFD0,T0KA80O0KA8,S057I740P017J740,S0JA80S02AHA80,Q015DIDU01DID0,R0JAW02AHA8,Q057H740W0J7,Q0JAg0IA80,P05FDD50J0I15151510J015DFD0,P0IA80gG0IA8,O017H740J0S4J05774,O02AHAgJ0IA,N015DD0J0O151515150H015FDC0,O0IAgL0IA0,N0I740I040404S4I017H70,N02AA0gL02AA8,M01DFD0H0O151515M5I01FDC,M02AA80gM0IA,M0I7K0W454540H057740,M0IAgP0HA80,L01DDC001S151515L5H017DD0,L02AA80gO02AA0,L0H750H0404040404W4H01574,L0HA80gQ0HA8,K01DD1001O151515R5H017DD,K02AA0gR02AA,K0H740H0404X4545454400177,K0HA80gS0HA80,J01DDC01T151515P5H017DC0,K0HAgU02AA0,J0H570H0404H404gI4H05770,J02A80gU0HA0,J05DC01P1515151515R5H01FDC,J0HA80gU0HA8,I01770H0gG45454545454400774,J0HAgW02AA,I05FD001S15151515R5H01DD,I02A80gW0HA,I0H74004gS4H0H7,I02A80gW02A80,H01FD001151H1515151010L01015P5H05FC0,I0HAgY02A80,H0177004N4W0454545454001740,H02AA0gY0HA0,H05DC01M1g0H1L5401DD0,H02A80N080gO0HA0,H0H74004K47F40Y0M4H0H70,H02A80M0IAgO02A8,01DF001515155FD7D010W015J5H05DC,H0HAM02AA82AA0gL02A8,0177004J47F7C7FF540V0H5454540574,H0HAM0HA2AHA2AA0U02AA0K02A8,01DD015151FFD1FHF1FFD10Q015FDHDI5H01DD,H0A80K0A8A2A8AKA80P02AJA80J0HA,0174004457F077F477F5F7F40P057I7J40175,02A80I02AA0AHA8AHA8AJAR02AHAK0HA,07DC01557C7DFD7DFF7FHF7FFD10N0H15DHDH501DD,02A80H0HA82AA82AHA2AHA2AIA80Q0HAJ0HA,077404575C7F7C7FHF7FHF7F7F7540P057540077,02A8002A0A2A0AHA8AHA8AMAR02A0H02A,05F8015D1FHF1FHF1FHFDFHFDFHFIDP0155015D,02A800820A8A0AWA80P08002A80,077007075F477FC7F7C7F7F7N740O01007740,02A00202AA82AA82AHA2AHA2AHA2AKAR02A80,1FD00117D5D7F5DFF7FTFHDQ05DC0,0AA8008A80AHA2AHA2AWAP02A80,075001F5C1FDF57FF57F7H7F757F7M750N0H740,0AA0H0A0A2A8AHA8AgGAN02A80,1DD001F17FF1FHF9FHFDFHFDFHFDFDFDMDM05DC0,0AA0H020AHA8AHA8AgHA80K02A80,0770H0157C75F477F7U757K740J07540,0AA0I02A82AHA2AHA2AHA2AHA2ASAK02A80,1FD0I0H1D7F5F7FDFHFDFHFDFHFDFHFDFFDDFDFDD0I05FC0,0AA0K0IA0AHA8AHA8AHA8AHA8ARA80H02A80,0770K057F1F7F5F775F7757H757R750H0H740,0AA0M02AgPA8002A80,1DD0M05FFC7FFD7FHF5FDF5DDF5FDFDFDFDJDH05DC0,0AA80M02A8AgNAH02A80,0770O057L757H757H757H757N75007540,0AA0P02AA2AHA2AgGAH02A80,15D0P015FKFDFLFIDFDFDFDFDIDH05DC0,02A80R0HA8AgIAH02A80,07740R057D7H757H757T740077,02A80T0gJAI02A80,05D80150Q015D5DFD5DUD5015D,02A80V0gGA80H0HA,077400450S05757H7H5Q75440077,02A80I080S02AVA80I0HA,01DC01H150S015FDFDHDFDFDFDHDFD5H501DD,02A80J0A0U0TA80J0HA,0174004H4740T057Q7H54540174,H0HAL0A80U02AOA80K0HA,01DD01J15D50T015DIDFDIDJ5H01FC,H0HAM0HA80U0NA80K02A8,H0H7H0K4577540T05757H754J4H0H74,H0HAN02AA80U02AIAN02A8,H05FC0151H1H5FD0V015DFD5K5017D8,H02A80M0280X02A0N0HA8,H0H74004K450Y014H45454401770,H02AA0gY0HA0,H01DD001N1W0H1M5H01DD0,I0HAgY02AA0,H01770H0O4U0P4H0H740,I0HAgY02A80,H015DC0115151515151H101010101515P5015FC0,I02A80gW0HA80,I0H74004gM45454540177,I02AA0gV02AA,I01DD00115151515151515151515H515N5H05DD,J0HA80gU02A8,J0H74004gQ4H0H74,J02A80gU0HA0,J05DD001151515H515H515V5H01FD0,J02AA0gT02AA0,J01774004gG454545454007740,K0HA80gS0HA80,K05DD0011515151515151515R5H01DD,K02AA0gR02AA,K01770H0gN4H0H74,L0HA80gQ0HA8,K015FD0015gK5H01FDC,L02AA0gP02AA8,L05774004S4545454545440H0H750,M0HA80gO0HA80,M05DD0015515H515U5I05DDC0,M02AA80gM02AA,M017740H0gH4H01774,N0IAgM02AA8,N05FD40015Y5H015FD0,N02AHAgL0IA0,N017H7J0H454H45454545454540H0I740,O02AA80gI0IA,O01DHDJ015S5I015DHD,P0IA80gG0IA8,P057750J0R4J057570,Q0IA80Y0IA80,P015DHDK0H1L510H0H15FDF,Q02AHA80W0JA,R0J740T017I74,R02AIAU02AIA0,R015DIDR015DDFD40,T0KAR0KA,T0K7540L0157J74,U0MAK02AKA80,T015DFDFDLDFDFDFDD,V02AUA8,W0U740,X02AQA0,X015DOD50,gG0MA80,gH0K50,,:::^XA\n" +
                                "^MMT\n" +
                                "^PW759\n" +
                                "^LL0999\n" +
                                "^LS0\n" +
                                "^FT544,192^XG000.GRF,1,1^FS\n" +
                                "^FO14,13^GB733,962,2^FS\n" +
                                "^FO14,211^GB733,0,4^FS\n" +
                                "^FO14,394^GB733,0,4^FS\n" +
                                "^FO501,13^GB0,197,4^FS\n" +
                                "^FT180,451^A0N,54,103^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD" + shipmentFrom_name + "^FS\n" +
                                "^FT190,511^A0N,39,120^FH\\^A@N,0,42,E:TT0003M_.TTF^CI28^FD" + shipmentFrom_phone + "^FS\n" +
                                "^FO14,596^GB733,0,4^FS\n" +
                                "^FT221,777^A0N,34,146^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD" + shipmentTo_name + "^FS\n" +
                                "^FT198,722^A0N,39,91^FH\\^A@N,0,42,E:TT0003M_.TTF^CI28^FD" + shipmentTo_phone + "^FS\n" +
                                "^FO19,818^GB728,0,4^FS\n" +
                                "^BY2,3,127^FT81,949^B3N,N,,Y,N\n" +
                                "^FD" + shipCode + "^FS\n" +
                                "^BY2,3,152^FT67,371^B3N,N,,Y,N\n" +
                                "^FD" + shipCode + "^FS\n" +
                                "^FT31,123^A0N,44,216^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD" + toStoreName + "^FS\n" +
                                "^FT25,64^A0N,44,43^FH\\^FDContact Us^FS\n" +
                                "^FT31,198^A0N,56,55^FH\\^FD" + toStorePhone + "^FS\n" +
                                "^PQ" + (j + 1) + ",0,1,Y^XZ\n" +
                                "^XA^ID000.GRF^FS^XZ\n").getBytes());
                    }
                });
            }
        } else {
            disconnect();
        }
    }

    public void setPrinterIP(DiscoveredPrinter discoveredPrinter) {
        ipAddressEditText.setText(discoveredPrinter.address);
    }

    void setupTscPrinter() {
        tscPrintBtn = findViewById(R.id.tsc_print);
        tscBackBtn = findViewById(R.id.tsc_back_btn);
        tscIp = findViewById(R.id.tsc_edit_ip);
        tscBackBtn.setOnClickListener(v -> finish());
    }

    void setupHoneywellPrinter() {
        _buttonFindPrinter = findViewById(R.id.findPrinterId);
        FindPrinterButtonListener buttonListener = new FindPrinterButtonListener();
        _buttonFindPrinter.setOnClickListener(buttonListener);

        _buttonSetPrinter = findViewById(R.id.setPrinterId);
        _buttonSetPrinter.setOnClickListener(new SetPrinterButtonListener());


        _buttonPrintBarcode = findViewById(R.id.printBarcodeButton);
        _buttonPrintBarcode.setOnClickListener(new PrintButtonListener());


        _spinner = findViewById(R.id.spinner);

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
        sdk.PRN_AddBarcodeToLabel("xxxx", "QRCODE", 2, 33, 333, 1, 1, 6);
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

    public static class MyHandler extends Handler {
        private final WeakReference<MainActivity> reference;

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
                            ArrayAdapter<String> arr_adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, prtNameIpList);
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
                                    SamplePrintText(shipCode, j + 1, productQty, _iBarcodeEnlarge, curPrinterIp, toStoreName, toStorePhone);
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

}
