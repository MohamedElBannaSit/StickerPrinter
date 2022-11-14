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

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app1.stickerprinter.R;
import com.example.app1.stickerprinter.zebra.chooseprinter.PrinterConnectionDialog;
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

import java.util.ArrayList;

public class ZebraActivity extends AppCompatActivity {

    private static final String TAG = "ZebraActivity";
    private Connection connection;
    private ZebraPrinter printer;

    private EditText ipAddressEditText;
    private EditText portNumberEditText;

    private Button testButton;
    private TextView statusField;

    PrinterConnectionDialog mPrinterConnectionDialog;

    Bundle shipDataBundle;
    ArrayList<Integer> shipmentQty;
    String shipmentShip_code;
    String shipmentStore_name;
    String shipmentFrom_name;
    String shipmentFrom_phone;
    String shipmentTo_name;
    String shipmentTo_phone;
    String shipmentTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zebra);

        initBundleData();

        mPrinterConnectionDialog = new PrinterConnectionDialog();

        ipAddressEditText = (EditText) this.findViewById(R.id.ipAddressInput);
        ipAddressEditText.setText("192.168.5.194");

        portNumberEditText = (EditText) this.findViewById(R.id.portInput);
        portNumberEditText.setText("9100");


        statusField = (TextView) this.findViewById(R.id.statusText);


        testButton = (Button) this.findViewById(R.id.testButton);
        testButton.setOnClickListener(v -> new Thread(() -> {
            enableTestButton(false);
            if (shipDataBundle != null)
                doConnectionTest();
        }).start());

        ImageButton findPrinterButton = (ImageButton) this.findViewById(R.id.search_printer);
        findPrinterButton.setOnClickListener(view ->
        {
            mPrinterConnectionDialog.show(getFragmentManager(), TAG);
        });
    }


    private void initBundleData() {
        shipDataBundle = getIntent().getExtras();
        if (shipDataBundle != null) {
            shipmentQty = shipDataBundle.getIntegerArrayList("qty");
            shipmentShip_code = shipDataBundle.getString("ship_code");
            shipmentStore_name = shipDataBundle.getString("store_name");
            shipmentFrom_name = shipDataBundle.getString("from_name");
            shipmentFrom_phone = shipDataBundle.getString("from_phone");
            shipmentTo_name = shipDataBundle.getString("to_name");
            shipmentTo_phone = shipDataBundle.getString("store_phone");
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
            for (int i = 0; i < shipmentQty.size(); i++) {
                int productQty = shipmentQty.get(i);
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
                            "^FD" + shipmentShip_code + "^FS\n" +
                            "^BY2,3,152^FT67,371^B3N,N,,Y,N\n" +
                            "^FD" + shipmentShip_code + "^FS\n" +
                            "^FT31,123^A0N,44,216^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD" + shipmentStore_name + "^FS\n" +
                            "^FT25,64^A0N,44,43^FH\\^FDContact Us^FS\n" +
                            "^FT31,198^A0N,56,55^FH\\^FD" + shipmentTo_phone + "^FS\n" +
                            "^PQ" + productQty + ",0,1,Y^XZ\n" +
                            "^XA^ID000.GRF^FS^XZ\n").getBytes());

//                    "^XA\n" +
//                            "^MMT\n" +
//                            "^PW759\n" +
//                            "^LL0999\n" +
//                            "^LS0\n" +
//                            "^FT544,192^XG000.GRF,1,1^FS\n" +
//                            "^FO14,13^GB733,962,2^FS\n" +
//                            "^FO14,211^GB733,0,4^FS\n" +
//                            "^FO14,394^GB733,0,4^FS\n" +
//                            "^FO501,13^GB0,197,4^FS\n" +
//                            "^FT180,451^A0N,54,103^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD??? ?????? ???????^FS\n" +
//                            "^FT190,511^A0N,39,120^FH\\^A@N,0,35,E:TT0003M_.TTF^CI28^FD??? ???? ??????^FS\n" +
//                            "^FT190,559^A0N,39,153^FH\\^FD????? ??????^FS\n" +
//                            "^FO14,596^GB733,0,4^FS\n" +
//                            "^FT221,777^A0N,34,146^FH\\^A@N,0,50,E:TT0003M_.TTF^CI28^FD????? ??????^FS\n" +
//                            "^FT198,722^A0N,39,91^FH\\^A@N,0,35,E:TT0003M_.TTF^CI28^FD??? ???? ?????? ????^FS\n" +
//                            "^FT186,651^A0N,54,81^FH\\^FD??? ?????? ???? ???????^FS\n" +
//                            "^FO19,818^GB728,0,4^FS\n" +
//                            "^BY2,3,127^FT81,949^B3N,N,,Y,N\n" +
//                            "^FDDMM-RYD-56789012^FS\n" +
//                            "^BY2,3,152^FT67,371^B3N,N,,Y,N\n" +
//                            "^FDDMM-RYD-456789012^FS\n" +
//                            "^FT31,123^A0N,44,216^FH\\^FDJED^FS\n" +
//                            "^FT25,64^A0N,44,43^FH\\^FDContact us^FS\n" +
//                            "^FT31,198^A0N,56,55^FH\\^FD05012345678^FS\n" +
//                            "^PQ1,0,1,Y^XZ\n" +
//                            "^XA^ID000.GRF^FS^XZ"

//                    "^XA\n" +
//                            "^MMT\n" +
//                            "^PW759\n" +
//                            "^LL0999\n" +
//                            "^LS0\n" +
//                            "^FT480,224^XG000.GRF,1,1^FS\n" +
//                            "^FO14,13^GB733,962,2^FS\n" +
//                            "^FO14,211^GB733,0,4^FS\n" +
//                            "^FO476,13^GB0,197,4^FS\n" +
//                            "^FO14,386^GB733,0,4^FS\n" +
//                            "^FO353,215^GB0,171,4^FS\n" +
//                            "^FT252,470^A0N,72,91^FH\\^FD??? ?????? ???????^FS\n" +
//                            "^FT393,515^A0N,43,79^FH\\^FD??? ???? ??????^FS\n" +
//                            "^FT537,583^A0N,56,55^FH\\^FD????? ??????^FS\n" +
//                            "^FO14,596^GB733,0,4^FS\n" +
//                            "^FT462,791^A0N,41,79^FH\\^FD????? ??????^FS\n" +
//                            "^FT278,730^A0N,43,79^FH\\^FD??? ???? ?????? ????^FS\n" +
//                            "^FT117,670^A0N,72,91^FH\\^FD??? ?????? ???? ???????^FS\n" +
//                            "^FO19,818^GB728,0,4^FS\n" +
//                            "^BY3,3,82^FT38,931^B3N,N,,Y,N\n" +
//                            "^FD123456789012^FS\n" +
//                            "^BY2,3,152^FT16,173^B3N,N,,Y,N\n" +
//                            "^FD123456789012^FS\n" +
//                            "^FT438,268^A0N,65,55^FH\\^FDDestination^FS\n" +
//                            "^FT462,386^A0N,106,105^FH\\^FDJED^FS\n" +
//                            "^FT81,259^A0N,44,43^FH\\^FDContact us^FS\n" +
//                            "^FT38,350^A0N,56,55^FH\\^FD05012345678^FS\n" +
//                            "^PQ1,0,1,Y^XZ\n" +
//                            "^XA^ID000.GRF^FS^XZ"


                }
            }


        } else {
            disconnect();
        }
    }

    public void setPrinterIP(DiscoveredPrinter discoveredPrinter) {
        ipAddressEditText.setText(discoveredPrinter.address);
    }

//    String getZPLCode() {
//        ZebraLabel zebraLabel = new ZebraLabel(912, 912);
//        zebraLabel.setDefaultZebraFont(ZebraFont.ZEBRA_ZERO);
//
//        zebraLabel.addElement(new ZebraText(10, 84, "Product:", 14));
//        zebraLabel.addElement(new ZebraText(395, 85, "Camera", 14));
//
//        zebraLabel.addElement(new ZebraText(10, 161, "CA201212AA", 14));
//
//        //Add Code Bar 39
//        zebraLabel.addElement(new ZebraBarCode39(10, 297, "CA201212AA", 118, 2, 2));
//
//        zebraLabel.addElement(new ZebraText(10, 365, "Qté:", 11));
//        zebraLabel.addElement(new ZebraText(180, 365, "3", 11));
//        zebraLabel.addElement(new ZebraText(317, 365, "QA", 11));
//
//        zebraLabel.addElement(new ZebraText(10, 520, "Ref log:", 11));
//        zebraLabel.addElement(new ZebraText(180, 520, "0035", 11));
//        zebraLabel.addElement(new ZebraText(10, 596, "Ref client:", 11));
//        zebraLabel.addElement(new ZebraText(180, 599, "1234", 11));
//
//        return zebraLabel.getZplCode();
//    }


}
