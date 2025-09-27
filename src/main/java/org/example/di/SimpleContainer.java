package org.example.di;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SimpleContainer {
    private static final Injector injector = Guice.createInjector(new FunctionsModule());

    public static <T> T get(Class<T> cls) {
        return injector.getInstance(cls);
    }
}
