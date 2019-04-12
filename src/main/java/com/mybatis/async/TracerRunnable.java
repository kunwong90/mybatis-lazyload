package com.mybatis.async;

import org.slf4j.MDC;

import java.util.Map;

public abstract class TracerRunnable implements Runnable {

    private Map<String, String> context;

    public TracerRunnable() {
        this.context = MDC.getCopyOfContextMap();
    }

    @Override
    public void run() {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        try {
            runWithMDC();
        } finally {
            if (previous == null) {
                // 防止使用线程池导致后面打印的traceId一致
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }

    public abstract void runWithMDC();
}
