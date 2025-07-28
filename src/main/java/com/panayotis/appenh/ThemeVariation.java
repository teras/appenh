package com.panayotis.appenh;

public enum ThemeVariation {
    AUTO,
    LIGHT,
    DARK;

    @Override
    public String toString() {
        String name = name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
