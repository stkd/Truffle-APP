package niu.bdlab.turfflev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.ArrayAdapter;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;


public class SensorDialog extends DialogFragment {
    private static final String TAG =  SensorDialog.class.getSimpleName();
    private static String addtext = null;
    private static String[] senList = new String[] {"","設備A", "設備B", "設備C", "設備D", "設備E", "設備F"};

    public interface SensorDialogParent {
        public String getSavePath();
    }

    public static  SensorDialog showlog(final Activity parent/* add parameters here if you need */) {
        SensorDialog dialog = newInstance(/* add parameters here if you need */);
        try {
            dialog.show(parent.getFragmentManager(), TAG);
        } catch (final IllegalStateException e) {
            dialog = null;
        }
        return dialog;
    }

    public static  SensorDialog newInstance(/* add parameters here if you need */) {
        final  SensorDialog dialog = new  SensorDialog();
        final Bundle args = new Bundle();
        // add parameters here if you need
        dialog.setArguments(args);
        return dialog;
    }

    private EditText meditText;
    private Spinner mSpinner;
    private ArrayAdapter<String> mAdapter;

    public SensorDialog(/* no arguments */) {
        // Fragment need default constructor
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
            savedInstanceState = getArguments();
    }

    @Override
    public void onSaveInstanceState(final Bundle saveInstanceState) {
        final Bundle args = getArguments();
        if (args != null)
            saveInstanceState.putAll(args);
        super.onSaveInstanceState(saveInstanceState);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(initset());
        builder.setTitle(R.string.sen_select);
        builder.setPositiveButton(android.R.string.ok, mOnDialogClickListener);
        builder.setNegativeButton(R.string.notchange , mOnDialogClickListener);
        final Dialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    /**
     * create view that this fragment shows
     * @return
     */
    private final View initset() {
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_sensor, null);
        mSpinner = (Spinner)rootView.findViewById(R.id.spinner2);
        meditText = (EditText)rootView.findViewById(R.id.editText1);
        final View empty = rootView.findViewById(android.R.id.empty);
        mSpinner.setEmptyView(empty);
        mAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,senList);
        mSpinner.setAdapter(mAdapter);
        return rootView;
    }

    private final DialogInterface.OnClickListener mOnDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    String svPath = ((SensorDialog.SensorDialogParent)getActivity()).getSavePath();
                    Log.d("test",svPath);
                    if(meditText.getText().toString().equals(""))
                    {
                        addtext = senList[mSpinner.getSelectedItemPosition()];
                    }
                    else
                    {
                        addtext = meditText.getText().toString();
                    }
                    String newPath;
                    newPath = svPath.substring(0,svPath.length()-4)+addtext+".png";
                    try {
                        File file = new File(svPath);
                        file.renameTo(new File(newPath));
                        MediaScannerConnection.scanFile(getActivity(), new String[]{ newPath }, null, null);
                    }catch (Exception e){
                        Log.e("TestFunction","Rename Failed");
                    }
                    Toast.makeText(getActivity(),newPath,Toast.LENGTH_SHORT).show();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    @Override
    public void onCancel(final DialogInterface dialog) {
        super.onCancel(dialog);
    }
}
