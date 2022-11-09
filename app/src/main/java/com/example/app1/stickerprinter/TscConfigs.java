package com.example.app1.stickerprinter;

import com.example.tscdll.TscWifiActivity;

import java.util.ArrayList;

import static java.awt.font.TextAttribute.FONT;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class TscConfigs {
    TscWifiActivity TscDll;
    String ip;
    Bundle data;


    public TscConfigs(TscWifiActivity m, String p, Bundle d1) {
        this.TscDll = m;
        this.ip = p;
        this.data = d1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {


             data.getIntegerArrayList("qty").forEach(s -> {
                 int productQty = s;
                for (int j = 0; j < productQty; j++) {
                    printTsc(j + 1, productQty);
                }
            });
        }


    }


    void printTsc(int index, int len) {
        try {


            TscDll.openport(this.ip, 9100);
            // TscDll.setup(paper_width,paper_height,speed,density,sensor,sensor_distance,sensor_offset);
//                    TscDll.clearbuffer();


            TscDll.sendcommand("SPEED 4\r\n");
            TscDll.sendcommand("DENSITY 12\r\n");
            TscDll.sendcommand("CODEPAGE UTF-8\r\n");
//                    TscDll.sendcommand("SET TEAR ON\r\n");
//                    TscDll.sendcommand("SET COUNTER @1 1\r\n");
//                    TscDll.sendcommand("@1 = \"0001\"\r\n");
            TscDll.sendcommand("SIZE 4,4\r\n");
            TscDll.sendcommand("GAP 2mm,0\r\n");
            TscDll.sendcommand("DIRECTION 1\r\n");
            //TscDll.sendcommand("FORMFEED\r\n");
            TscDll.sendcommand("CLS\r\n");

            TscDll.printerfont(200, 23, "4", 0, 1, 1, "ALSAIF-EXPRESS");

//          SHIP-BARCODE
            TscDll.barcode(50, 123, "128", 100, 0, 0, 3, 8, data.getString("ship_code"));

//          SHIP-CODE
            TscDll.printerfont(50, 273, "4", 0, 1, 1, data.getString("ship_code"));

//          PRODUCT_BARCODE
            TscDll.barcode(50, 373, "128", 80, 0, 0, 3, 3, data.getString("ship_code"));

//          Product_index_From
            TscDll.printerfont(50, 474, "3", 0, 1, 1, (index) + " From " + len);

//          to_Store_name
            TscDll.printerfont(50, 523, "3", 0, 1, 1, "Store:" + data.getString("store_name") + "\tPhone:" + data.getString("store_phone"));

            TscDll.printlabel(1, 1);


            TscDll.closeport(5000);

        } catch (Exception e) {
            Log.e("printTsc", e.getLocalizedMessage());
        }
    }
}
