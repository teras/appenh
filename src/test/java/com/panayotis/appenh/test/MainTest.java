/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.appenh.test;

import com.panayotis.appenh.EnhancerManager;
import javax.swing.JFrame;

/**
 *
 * @author teras
 */
public class MainTest {

    public static void main(String[] args) {
        EnhancerManager.getDefault().registerAbout(new Runnable() {
            @Override
            public void run() {
                System.out.println("About clicked");
            }
        });
        EnhancerManager.getDefault().registerPreferences(new Runnable() {
            @Override
            public void run() {
                System.out.println("Preferences clicked");
            }
        });
        JFrame f = new JFrame();
        f.setVisible(true);
        f.setSize(400, 300);
    }
}
