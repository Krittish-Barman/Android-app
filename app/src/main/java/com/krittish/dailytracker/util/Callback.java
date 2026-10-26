package com.krittish.dailytracker.util;

public interface Callback<T> {
    void on(T value);
}
