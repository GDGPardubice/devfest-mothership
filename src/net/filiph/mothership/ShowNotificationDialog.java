package net.filiph.mothership;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: Jirka
 * Date: 24.4.13
 * Time: 20:13
 * To change this template use File | Settings | File Templates.
 */
public class ShowNotificationDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_show_notification)
                .setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User approved the dialog
                        getActivity().getApplicationContext().getSharedPreferences(MainActivity.PREFS_NAME, 0).edit()
                                .putBoolean("showNotification", true)
                                .commit();
                    }
                })
                .setNegativeButton(R.string.dialog_button_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        getActivity().getApplicationContext().getSharedPreferences(MainActivity.PREFS_NAME, 0).edit()
                                .putBoolean("showNotification", false)
                                .commit();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}