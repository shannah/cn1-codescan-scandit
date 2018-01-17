package com.codename1.scandit;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScanditInstallerImpl implements com.codename1.scandit.ScanditInstaller{
    public void extractNativeFiles() {
        try {
            File nativeDir = new File("native");
            File iosNative = new File(nativeDir, "ios");
            File androidNative = new File(nativeDir, "android");
            File aarFile = new File(androidNative, "ScanditBarcodeScanner.aar");
            File frameworkFile = new File(iosNative, "ScanditBarcodeScanner.framework");
            File iosLibFile = new File(iosNative, "libScandirBarcodeScanner.a");

            boolean aarFilePresent = aarFile.exists();
            if (!aarFilePresent && new File("lib/impl/native/android/"+aarFile.getName()).exists()) {
                aarFilePresent = true;
            }
            if (frameworkFile.exists()) {
                File headersDir = new File(frameworkFile, "Headers");
                for (File headerFile : headersDir.listFiles()) {
                    File destHeaderFile = new File(iosNative, headerFile.getName());
                    Log.p("Copying "+headerFile+" to "+destHeaderFile);
                    try (FileInputStream fis = new FileInputStream(headerFile)) {
                        try (FileOutputStream fos = new FileOutputStream(destHeaderFile)) {
                            Util.copy(fis, fos);
                        }
                    }
                }
                File frameworkBinaryFile = new File(frameworkFile, "ScanditBarcodeScanner");
                Log.p("Copying "+frameworkBinaryFile+" to "+iosLibFile);
                try (FileInputStream fis = new FileInputStream(frameworkBinaryFile)) {
                    try (FileOutputStream fos = new FileOutputStream(iosLibFile)) {
                        Util.copy(fis, fos);
                    }
                }
                Log.p("Deleting "+frameworkFile);
                delete(frameworkFile);
            }
            
            
            
            boolean iosLibFilePresent = iosLibFile.exists();
            if (!iosLibFilePresent && new File("lib/impl/native/ios/"+iosLibFile.getName()).exists()) {
                iosLibFilePresent = true;
            }
            String platformsMessage = "";
            if (iosLibFilePresent && !aarFilePresent) {
                platformsMessage = "Android";
            } else if (aarFilePresent && !iosLibFilePresent) {
                platformsMessage = "iOS";
            } else if (!aarFilePresent && !iosLibFilePresent) {
                platformsMessage = "Android or iOS";
            }
            String message = "The ScanditSDK could not be found for "+platformsMessage+".  Please copy the the ScanditBarcodeScanner.aar file into your project's native/android directory, and the ScanditBarcodeScanner.framework file into the native/ios directory.";
            if (!aarFilePresent || !iosLibFilePresent) {
                Display.getInstance().callSerially(()->{
                    Command ok = new Command("OK");
                    Command learnMore = new Command("Learn More");
                    Command res = Dialog.show("Scandit", new SpanLabel(message), ok, learnMore);
                    if (res == learnMore) {
                        Display.getInstance().execute("https://github.com/shannah/cn1-codescan-scandit");
                    }
                });
            } else {
                if ("true".equals(Display.getInstance().getProperty("ShowScanditInstalledMessage", "false"))) {
                    Display.getInstance().callSerially(()->{
                        ToastBar.showMessage("The ScanditSDK was successfully installed", FontImage.MATERIAL_SCANNER);
                    });
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to extract native files", t);
        }
    }
    
    private static void delete(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                delete(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }

    public boolean isSupported() {
        return true;
    }

}
