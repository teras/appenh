/*
 *
 * This file is part of ApplicationEnhancer.
 *
 * ApplicationEnhancer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * ApplicationEnhancer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.appenh;

@SuppressWarnings("UseSpecificCatch")
class WindowsEnhancer extends DefaultEnhancer {
    private final boolean scaleUI = calculateIfUIShouldBeScaled();

    private boolean calculateIfUIShouldBeScaled() {
        try {
            if (Double.parseDouble(System.getProperty("os.version")) < 10)  // needs Windows 10
                return true;
        } catch (NumberFormatException ignored) {
        }
        // Needs Java 9+
        return System.getProperty("java.version", "1.").startsWith("1.");
    }

    @Override
    public void blendWindowTitle(boolean blended) {
        String value = blended ? "true" : "false";
        System.setProperty("flatlaf.useWindowDecorations", value);
        System.setProperty("flatlaf.menuBarEmbedded", value);
    }

    @Override
    public boolean shouldScaleUI() {
        return scaleUI;
    }
}
