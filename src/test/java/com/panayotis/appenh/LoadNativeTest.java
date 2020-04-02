/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.appenh;

import com.panayotis.loadlib.LoadLib;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teras
 */
public class LoadNativeTest {

    @Test
    public void testLoad() {
        if (EnhancerManager.getDefault() instanceof MacEnhancer) {
            assertTrue("Unable to load native library", LoadLib.load(MacEnhancer.LIB_LOCATION));
            System.out.println(            EnhancerManager.getDefault().getThemeName());
        }
    }
}
