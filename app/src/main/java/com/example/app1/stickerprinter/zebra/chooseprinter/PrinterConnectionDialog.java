package com.example.app1.stickerprinter.zebra.chooseprinter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.app1.stickerprinter.MainActivity;
import com.example.app1.stickerprinter.R;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

import java.util.ArrayList;

public class PrinterConnectionDialog extends DialogFragment {
    private static final String TAG = "PRINTER_CNNCTN_DIALOG";

    private MainActivity mainActivity;
    private TextView emptyView;

    private DiscoveredPrinterAdapter adapter;
    private ArrayList<DiscoveredPrinter> discoveredPrinters;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.i(TAG, "onCreateDialog()");

        mainActivity = (MainActivity) getActivity();
        View view = View.inflate(getActivity(), R.layout.dialog_printer_connect, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setView(view);

        discoveredPrinters = new ArrayList<>();
        adapter = new DiscoveredPrinterAdapter(getActivity(), R.layout.list_item_discovered_printer, discoveredPrinters);

        emptyView = (TextView) view.findViewById(R.id.discoveredPrintersEmptyView);

        ListView discoveredPrintersListView = (ListView) view.findViewById(R.id.discoveredPrintersListView);
        discoveredPrintersListView.setEmptyView(emptyView);
        discoveredPrintersListView.setAdapter(adapter);

        final AlertDialog dialog = builder.create();

        discoveredPrintersListView.setOnItemClickListener((parent, view1, position, id) -> {
            try {
                mainActivity.setPrinterIP(discoveredPrinters.get(position));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            dialog.dismiss();
        });

        try {
            NetworkDiscoverer.findPrinters(new DiscoveryHandler() {
                @Override
                public void foundPrinter(DiscoveredPrinter discoveredPrinter) {

                    try {
                        discoveredPrinters.add(discoveredPrinter);
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }

                    Log.i(TAG, "Discovered a printer");
                }

                @Override
                public void discoveryFinished() {
                    Log.i(TAG, "Discovery finished");
                }

                @Override
                public void discoveryError(String s) {
                    Log.i(TAG, "Discovery error");
                }
            });
        } catch (DiscoveryException e) {
            e.printStackTrace();
        }

        return dialog;
    }


}
