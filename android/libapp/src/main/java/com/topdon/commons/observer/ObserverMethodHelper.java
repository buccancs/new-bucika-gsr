package com.topdon.commons.observer;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.Tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObserverMethodHelper {
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private boolean isObserveAnnotationRequired;

    ObserverMethodHelper(boolean isObserveAnnotationRequired) {
        this.isObserveAnnotationRequired = isObserveAnnotationRequired;
    }

    void clearCache() {
        METHOD_CACHE.clear();
    }

    Runnable generateRunnable(Observer observer, Method method, MethodInfo info) {
        MethodInfo.Parameter[] parameters = info.getParameters();
        if (parameters == null || parameters.length == 0) {
            return () -> {
                try {
                    method.invoke(observer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            };
        } else {
            final Object[] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters[i];
                params[i] = parameter.getValue();
            }
            return () -> {
                try {
                    method.invoke(observer, params);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            };
        }
    }

    String generateKey(String tag, String name, Class<?>[] paramTypes) {
        StringBuilder sb = new StringBuilder();
        if (tag.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(tag);
        }
        for (Class<?> type : paramTypes) {
            sb.append(",").append(type);
        }
        return sb.toString();
    }

    Map<String, Method> findObserverMethod(Observer observer) {
        Map<String, Method> map = METHOD_CACHE.get(observer.getClass());
        if (map != null) {
            return map;
        }
        map = new HashMap<>();
        List<Method> methods = new ArrayList<>();
        Class<?> cls = observer.getClass();
        while (cls != null && !cls.isInterface() && Observer.class.isAssignableFrom(cls)) {
            Method[] ms = null;
            try {
                ms = cls.getDeclaredMethods();
            } catch (Throwable ignore) {
            }
            if (ms != null) {
                for (Method m : ms) {
                    int ignore = Modifier.ABSTRACT | Modifier.STATIC | 0x40 | 0x1000;
                    if ((m.getModifiers() & Modifier.PUBLIC) != 0 && (m.getModifiers() & ignore) == 0 &&  !contains(methods, m)) {
                        methods.add(m);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        for (Method method : methods) {
            Observe anno = method.getAnnotation(Observe.class);          
            if (anno != null || !isObserveAnnotationRequired) {
                Tag tagAnno = method.getAnnotation(Tag.class);
                String tag = tagAnno == null ? "" : tagAnno.value();
                String key = generateKey(tag, method.getName(), method.getParameterTypes());
                map.put(key, method);
            }
        }
        if (!map.isEmpty()) {
            METHOD_CACHE.put(observer.getClass(), map);
        }
        return map;
    }

    private static boolean contains(List<Method> methods, Method method) {
        for (Method m : methods) {
            if (m.getName().equals(method.getName()) && m.getReturnType().equals(method.getReturnType()) &&
                    equalParamTypes(m.getParameterTypes(), method.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }
}
