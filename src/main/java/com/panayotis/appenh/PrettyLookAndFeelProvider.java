/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.appenh;

import com.seaglasslookandfeel.SeaGlassLookAndFeel;
import javax.swing.LookAndFeel;

/**
 *
 * @author teras
 */
public class PrettyLookAndFeelProvider {
    public static LookAndFeel getLaF() {
        return new SeaGlassLookAndFeel();
    }
}
