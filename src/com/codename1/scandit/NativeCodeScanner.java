/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.scandit;

import com.codename1.system.NativeInterface;

/**
 *
 * @author shannah
 */
public interface NativeCodeScanner extends NativeInterface {
    public void scan(int[] symbologies);
    
}
