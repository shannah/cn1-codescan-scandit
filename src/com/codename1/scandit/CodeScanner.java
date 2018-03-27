/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.scandit;

import com.codename1.io.Log;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Bar Code and QR Code scanner using the Scandit SDK
 * 
 * <p><a href="https://www.scandit.com/"/>Scandit Website</a></p>
 * 
 * <p>Supports Android and iOS Only</p>
 * 
 * <h4>Basic Usage</h4>
 * <pre>{@code
 * CodeScanner.setLicenseKey("xxxxx");
 * CodeScanner.getInstance().scanQRCode(new ScanResult() {

        @Override
        public void scanCompleted(String contents, String formatName, byte[] rawBytes) {
            Log.p("Scan completed "+contents+" format "+formatName);
            

        }

        @Override
        public void scanCanceled() {
            Log.p("Scan canceled");
        }

        @Override
        public void scanError(int errorCode, String message) {
            Log.p("Scan error "+errorCode+": "+message);
        }
    });
 * 
 * }</pre>
 * 
 * <p>Convenience methods exist for {@link #scanBarCode(com.codename1.scandit.ScanResult) } and {@link #scanQRCode(com.codename1.scandit.ScanResult) },
 * but you can also supply a specific list of the types of codes you want to scan via the {@link #scan(java.util.Collection, com.codename1.scandit.ScanResult) } method.
 * </p>
 * <p>There are static class constants for the available symbologies.</p>
 *
 * @author Steve Hannah
 */
public class CodeScanner {
    private ScanResult callback;
    private NativeCodeScanner nativeInstance;
    private IosCodeScanner iosInstance;
    private static CodeScanner instance;
    private static String licenseKey;
    static boolean debug;
    
    /**
     * Set this flag to add extra debug stack traces on errors.
     * @param debug 
     */
    public static void setDebug(boolean debug) {
        CodeScanner.debug = debug;
    }
    
    /**
     * \brief Sentinel value to represent an unknown symbology
     */
    public static final int SYMBOLOGY_Unknown = 0x0000000;
    /** 
     * EAN-13 1D barcode symbology.
     */
    public static final int SYMBOLOGY_EAN13  = 0x0000001;
    /** 
     * UPC-12/UPC-A 1D barcode symbology.
     */
    public static final int SYMBOLOGY_UPC12 = 0x0000004;
    /** 
     * UPC-E 1D barcode symbology.
     */
    public static final int SYMBOLOGY_UPCE = 0x0000008;
    /** 
     * Code 39 barcode symbology. Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_CODE39 = 0x0000020;
    /**
     * PDF417 barcode symbology. Only available in the Professional and Enterprise Packages. 
     */
    public static final int SYMBOLOGY_PDF417  = 0x0000400;
    /**
     * Data Matrix 2D barcode symbology. Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_DATAMATRIX = 0x0000200;
    /**
     * QR Code 2D barcode symbology. 
     */
    public static final int  SYMBOLOGY_QR = 0x0000100;
    /**
     * Interleaved-Two-of-Five (ITF) 1D barcode symbology. Only available in the Professional and 
     * Enterprise Packages.
     */
    public static final int SYMBOLOGY_ITF = 0x0000080;
    /**
     * Code 128 1D barcode symbology, including GS1-Code128. Only available in the Professional and
     * Enterprise Packages. 
     */
    public static final int SYMBOLOGY_CODE128 = 0x0000010;
    /** 
     * Code 93 barcode symbology. Only available in the Professional and Enterprise Packages. 
     */
    public static final int SYMBOLOGY_CODE93 = 0x0000040;
    /** 
     * MSI Plessey 1D barcode symbology. Only available in the Professional and Enterprise Packages. 
     */
    public static final int SYMBOLOGY_MSIPLESSEY = 0x0000800;
    /** 
     * GS1 DataBar 14 1D barcode symbology. Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_GS1DATABAR = 0x0001000;
    /** 
     * GS1 DataBar Expanded 1D barcode symbology. Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_GS1DATABAREXPANDED = 0x0002000;
    /** 
     * Codabar 1D barcode symbology. Only available in the Professional and Enterprise Packages. 
     */
    public static final int SYMBOLOGY_CODABAR= 0x0004000;
    /** 
     * EAN-8 1D barcode symbology.
     */
    public static final int SYMBOLOGY_EAN8  = 0x0000002;
    /** 
     * Aztec Code 2D barcode symbology. Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_AZTEC = 0x0008000;
    /**
     * Two-digit add-on for UPC and EAN codes.
     *
     * In order to scan two-digit add-on codes, at least one of these symbologies must be activated
     * as well: \ref SBSSymbologyEAN13, \ref SBSSymbologyUPC12, \ref SBSSymbologyUPCE, or 
     * \ref SBSSymbologyEAN8 and the maximum number of codes per frame has to be set to at least 2.
     *
     * Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_TWODIGITADDON = 0x0010000;
    /**
     * Five-digit add-on for UPC and EAN codes.
     *
     * In order to scan five-digit add-on codes, at least one of these symbologies must be activated
     * as well: \ref SBSSymbologyEAN13, \ref SBSSymbologyUPC12, \ref SBSSymbologyUPCE, or 
     * \ref SBSSymbologyEAN8 and the maximum number of codes per frame has to be set to at least 2.
     *
     * Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_FIVEDIGITADDON = 0x0020000;
    /**
     * Code 11 1D barcode symbology
     *
     * Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_CODE11= 0x0080000;
    /**
     * MaxiCode 2D barcode symbology
     *
     * Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_MAXICODE = 0x0040000;
    
    /**
     * GS1 DataBar Limited 1D barcode symbology
     *
     * Only available in the Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_GS1DATABARLIMITED = 0x0100000;
    /**
     * Code25 1D barcode symbology
     *
     * Also known as 'Industrial 2 of 5', 'Standard 2 of 5' or 'Discrete 2 of 5'. 
     *
     * Only available in Professional and Enterprise Packages.
     */
    public static final int SYMBOLOGY_CODE25 = 0x0200000;
    
    /**
     * Micro PDF417 2D barcode symbology
     *
     * Only available in Professional and Enterprise Packages
     */
    public static final int SYMBOLOGY_MICROPDF417 = 0x0400000;
    
    /**
     * Royal Mail 4 State Customer Code (RM4SCC)
     *
     * Only available in Professional and Enterprise Packages
     */
    public static final int SYMBOLOGY_RM4SCC = 0x0800000;
    
    /**
     * Royal Dutch TPG Post KIX
     *
     * Only available in Professional and Enterprise Packages
     */
    public static final int SYMBOLOGY_KIX = 0x1000000;
    
    /**
     * DotCode 2d barcode symbology
     *
     * Only available in Professional and Enterprise Packages
     */
    public static final int SYMBOLOGY_DOTCODE = 0x2000000;
    
    /**
     * Sets the license key for your Scandit app. This must be called before 
     * performing a scan.
     * @param key 
     */
    public static void setLicenseKey(String key) {
        licenseKey = key;
    }
    
    /**
     * Gets the scandit license key.
     * @return 
     */
    public static String getLicenseKey() {
        return licenseKey;
    }
    
    private static void extractNativeFiles() {
        try {
            ScanditInstaller installer = (ScanditInstaller)NativeLookup.create(ScanditInstaller.class);
            if (installer != null && installer.isSupported()) {
                installer.extractNativeFiles();
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }
    
    private CodeScanner() {
        try {
            if (IosCodeScanner.isSupported()) {
                iosInstance = new IosCodeScanner(licenseKey);
            } else {
                nativeInstance = (NativeCodeScanner)NativeLookup.create(NativeCodeScanner.class);
            }
        } catch (Throwable ex) {
            Log.p("Failed to load code scanner on this platform.");
        }
    }
    
    /**
     * Install the native components.
     */
    public static void install() {
        Display.getInstance().setProperty("ShowScanditInstalledMessage", "true");
        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                Display.getInstance().callSerially(()->{
                    CodeScanner.isSupported();
                });
            }
            
        };
        timer.schedule(tt, 2000);
        
    }
    
    public static boolean isSupported() {
        if (Display.getInstance().isSimulator()) {
            extractNativeFiles();
            return false;
        }
        if (IosCodeScanner.isSupported()) {
            return true;
        }
        getInstance();
        return instance.nativeInstance != null && instance.nativeInstance.isSupported();
    }
    
    /**
     * Returns the instance of the code scanner, notice that this method is equivalent 
     * to Display.getInstance().getCodeScanner().
     * 
     * @return instance of the code scanner
     */
    public static CodeScanner getInstance() {
        if (instance == null) {
            instance = new CodeScanner();
        }
        return instance;
    }
    
    /**
     * Convenience utility method for converting a list of symbologis into a Collection so
     * it can be passed to the {@link #scan(java.util.Collection, com.codename1.scandit.ScanResult) } method.
     * @param symbologies The symbologies to use.  See the {@literal SYMBOLOGY_XXXX} constants in this class.
     * @return The list as a collection.
     */
    public static Collection<Integer> createSymbologySet(int... symbologies) {
        HashSet<Integer> out = new HashSet<Integer>();
        for (int i: symbologies) {
            out.add(i);
        }
        return out;
    }
    
    /**
     * Opens up a scanning window to allow the user to scan a single code in the 
     * specified symbologies.
     * @param symbologies The symbologies to use.  See class constants {@literal SYMBOLOGY_XXX}
     * @param callback Callback to handle the scanning result.
     */
    public void scan(Collection<Integer> symbologies, ScanResult callback) {
        if (IosCodeScanner.isSupported()) {
            iosInstance.scan(symbologies, callback);
            return;
        }
        this.callback = callback;
        int[] l = new int[symbologies.size()];
        int i=0;
        for (int sym : symbologies) {
            l[i] = sym;
            i++;
        }
        nativeInstance.scan(l);
    }
    
        
    /**
     * Opens a scanning window to scan QR codes.
     * 
     * @param callback scan results
     */
    public void scanQRCode(ScanResult callback) {
        scan(createSymbologySet(SYMBOLOGY_QR), callback);
    }
        
    /**
     * Opens a scanning window to scan bar codes.
     * 
     * @param callback scan results
     */
    public void scanBarCode(ScanResult callback) {
        scan(createSymbologySet( SYMBOLOGY_EAN13 ,
                                 SYMBOLOGY_UPC12,
                                 SYMBOLOGY_EAN8,
                                 SYMBOLOGY_UPCE,
                                 SYMBOLOGY_CODE39 ,
                                 SYMBOLOGY_CODE128,
                                 SYMBOLOGY_ITF,
                                 SYMBOLOGY_DATAMATRIX),
                callback);
    }
    
    
    /**
     * Called upon a successful scan operation
     * 
     * @param contents the contents of the data
     * @param formatName the format of the scan
     * @param rawBytes the bytes of data
     */
    static void scanCompletedCallback(final String contents, final String formatName, final byte[] rawBytes) {
        if (getInstance().callback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    getInstance().callback.scanCompleted(contents, formatName, rawBytes);
                    getInstance().callback = null;
                    Display.getInstance().getCurrent().revalidate();
                    Display.getInstance().getCurrent().repaint();
                }
            }); 
        }
    }
    
    /**
     * Invoked if the user canceled the scan
     */
    static void scanCanceledCallback() {
        if (getInstance().callback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    getInstance().callback.scanCanceled();
                    getInstance().callback = null;
                    Display.getInstance().getCurrent().revalidate();
                    Display.getInstance().getCurrent().repaint();
                }
            });
        }
    }
    
    /**
     * Invoked if an error occurred during the scanning process
     * 
     * @param errorCode code
     * @param message descriptive message
     */
    static void scanErrorCallback(final int errorCode, final String message) {
        if (getInstance().callback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    getInstance().callback.scanError(errorCode, message);
                    getInstance().callback = null;
                    Display.getInstance().getCurrent().revalidate();
                    Display.getInstance().getCurrent().repaint();
                }
            });
            
        }
    }
}
