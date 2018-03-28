/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.scandit;

import com.codename1.io.Log;
import com.codename1.objc.Objc;
import com.codename1.objc.Pointer;
import com.codename1.ui.Display;
import com.codename1.objc.Runtime;
import com.codename1.objc.Method;
import com.codename1.objc.Objc.DelegateObject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



/**
 * Codescanner wrapper for iOS only
 * @author Steve Hannah
 */
class IosCodeScanner {
    private static final int SBSCameraFacingDirectionBack=0;
    private static final int SBSCameraFacingDirectionFront =1;
    
    public static final int ERR_SCAN_IN_PROGRESS=1;
    
    private static enum BSCameraSwitchVisibility {
        SBSCameraSwitchVisibilityNever,
        SBSCameraSwitchVisibilityOnTablet,
        SBSCameraSwitchVisibilityAlways
    };
    
    private Pointer picker;
    
    public static boolean isSupported() {
        return Runtime.getInstance().isSupported();
    }
    
    private ScanResult callback;
    
    private static Pointer createScanSettings(Collection<Integer> symbologies) {
        Pointer scanSettings = Objc.eval("SBSScanSettings.defaultSettings").asPointer();
        Objc.setProperty(scanSettings, "cameraFacingPreference", 0);
        if (symbologies != null) {
            for (int i : symbologies) {
                Objc.eval(scanSettings, "setSymbology:enabled:", i, true);
            }
        }
        return scanSettings;
        
    }
    
    public IosCodeScanner(String licenseKey) {
        Log.p("Creating new IosCodeScanner");
        Objc.dispatch_sync(()->{
            Objc.eval("SBSLicense.setAppKey:", licenseKey);
            
            picker = Objc.eval("SBSBarcodePicker.alloc.initWithSettings:", createScanSettings(null)).asPointer();
            if (picker == null || picker.address == 0) {
                throw new RuntimeException("Failed to create picker");
            }
            Pointer overlayController = Objc.getProperty(picker, "overlayController").asPointer();
            Objc.eval(overlayController, "showToolBar:", true);
            Objc.eval(overlayController, "setCameraSwitchVisibility:", BSCameraSwitchVisibility.SBSCameraSwitchVisibilityAlways);
            
            DelegateObject delegate = Objc.makeDelegate()
                    
                    //https://docs.scandit.com/5.5/ios/protocol_s_b_s_scan_delegate-p.html
                    .add(new Method("SBSScanDelegate", "barcodePicker:didScan:") {

                        @Override
                        public Object invoke(Object... args) {
                            Objc.dispatch_async(()->{
                                if (!inProgress || modalBufferResult != null) {
                                    // prevent this from scanning twice
                                    return;
                                }
                                try {
                                    //Pointer picker = getArgAsPointer(args[0]);
                                    Pointer session = getArgAsPointer(args[1]);
                                    Pointer recognized = Objc.getProperty(session, "newlyRecognizedCodes").asPointer();
                                    Pointer code = Objc.eval(recognized, "firstObject").asPointer();
                                    String symbologyName = Objc.getProperty(code, "symbologyName").asString();
                                    String data = Objc.getProperty(code, "data").asString();
                                    Log.p("Scanned "+symbologyName+" barcode: "+data);

                                    // The animation to show the camera isn't finished
                                    // we'll store this result in a buffer and then call the callback
                                    // in the completion handler.
                                    if (modalStartAnimationDone) {
                                        Objc.eval(picker, "stopScanning");

                                        Objc.dismissViewController(picker, true, null);
                                        inProgress = false;
                                        Display.getInstance().callSerially(()->{
                                            callback.scanCompleted(data, symbologyName, null);
                                        });
                                    } else {
                                        modalBufferResult = data;
                                        modalBufferSymbologyName = symbologyName;
                                    }


                                } catch (Throwable t) {
                                    if (CodeScanner.debug) {
                                        Log.e(t);
                                    }
                                    Display.getInstance().callSerially(()->{
                                        callback.scanError(0, t.getMessage());
                                    });
                                }
                                return;
                            });
                            return null;
                            
                        }

                    })
                    
                    //https://docs.scandit.com/5.5/ios/protocol_s_b_s_overlay_controller_did_cancel_delegate-p.html
                    .add("overlayController:didCancelWithStatus:", new Method("v@:@@") {

                        @Override
                        public Object invoke(Object... args) {
                            Objc.dispatch_async(()->{
                                Objc.eval(picker, "stopScanning");
                                inProgress = false;
                                Objc.dismissViewController(picker, true, null);
                                Display.getInstance().callSerially(()->{
                                    callback.scanCanceled();
                                });
                            });
                            
                            
                            return null;
                        }
                        
                    });

            Objc.setProperty(picker, "scanDelegate", delegate);
            Objc.setProperty(overlayController, "cancelDelegate", delegate);
        });
    }
    
    
    
    public void setSymbologyEnabled(int symbology, boolean enabled) {
        Objc.eval(picker, "setSymbology:enabled:", symbology, enabled);
    }
    
    private boolean modalStartAnimationDone;
    private String modalBufferResult;
    private String modalBufferSymbologyName;
    private boolean inProgress;
    
    private void returnModalBuffer() {
        Display.getInstance().callSerially(()->{
            if (callback != null) {
                callback.scanCompleted(modalBufferResult, modalBufferSymbologyName, null);
            }
        });
    }
    
    
    /**
     * Scans based on the settings in this class and returns the results
     * 
     * @param callback scan results
     */
    public void scan(Collection<Integer> symbologies, ScanResult callback) {
        if (inProgress) {
            Log.p("Scanning is currently in progress");
            if (callback != null) {
                Display.getInstance().callSerially(()->callback.scanError(ERR_SCAN_IN_PROGRESS, "Scan already in progress"));
            }
            return;
        }
        inProgress = true;
        Log.p("In scanQRCode");
        this.callback = callback;
        Objc.eval(picker, "applyScanSettings:completionHandler:", createScanSettings(symbologies), (Runnable)()->{
            Objc.dispatch_async(()->{
                Objc.eval(picker, "startScanning");
                Log.p("About to present View controller");
                modalStartAnimationDone = false;
                modalBufferResult = null;
                Objc.presentViewController(picker, true, ()->{
                    if (modalBufferResult != null) {
                        Objc.eval(picker, "stopScanning");
                        inProgress = false;
                        Objc.dismissViewController(picker, true, null);
                        returnModalBuffer();
                    }
                    modalStartAnimationDone = true;
                });
            });
        });
        
        
        
    }
        
    
    protected void finalize() throws Throwable {
        try {
            Objc.eval(picker, "release");
        } catch (Throwable t) {
            Log.e(t);
        }
    }
    
    
}
