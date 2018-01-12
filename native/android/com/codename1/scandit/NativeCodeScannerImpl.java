package com.codename1.scandit;
import android.content.Intent;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.IntentResultListener;
import com.scandit.barcodepicker.BarcodePickerActivity;
import com.scandit.barcodepicker.ScanditLicense;

public class NativeCodeScannerImpl {
    private static final int REQUEST_BARCODE_PICKER_ACTIVITY = 55;
    
    public void scan(int[] symbologiesToEnable) {
        ScanditLicense.setAppKey(CodeScanner.getLicenseKey());
	Intent launchIntent = new Intent(AndroidNativeUtil.getActivity(), BarcodePickerActivity.class);
        launchIntent.putExtra("enabledSymbologies", symbologiesToEnable);
        //launchIntent.putExtra("guiStyle", ScanOverlay.GUI_STYLE_LASER);
        launchIntent.putExtra("restrictScanningArea", true);
        launchIntent.putExtra("scanningAreaHeight", 0.1f);
        AndroidNativeUtil.startActivityForResult(launchIntent, REQUEST_BARCODE_PICKER_ACTIVITY, new IntentResultListener() {

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode != REQUEST_BARCODE_PICKER_ACTIVITY) {
			return;
		}
		
		if (data.getBooleanExtra("barcodeRecognized", false)) {
                    CodeScanner.scanCompletedCallback(data.getStringExtra("barcodeData"), data.getStringExtra("barcodeSymbologyName"), null);
                    
		} else {
                    CodeScanner.scanCanceledCallback();
                }
            }
        });

        

        
    }

    public boolean isSupported() {
        return true;
    }

}
