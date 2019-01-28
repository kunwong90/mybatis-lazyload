package com.squareup.okhttp3.example.dnsoverhttps;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestDohMain {
    public static void main(String[] args) throws IOException {
        Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);

        OkHttpClient bootstrapClient = new OkHttpClient.Builder().build();

        List<String> names = Arrays.asList("google.com", "graph.facebook.com", "sdflkhfsdlkjdf.ee");

        try {
            System.out.println("uncached\n********\n");
            List<DnsOverHttps> dnsProviders =
                    DohProviders.providers(bootstrapClient, false, false, false);
            runBatch(dnsProviders, names);

            Cache dnsCache =
                    new Cache(new File("./target/TestDohMain.cache." + System.currentTimeMillis()),
                            10 * 1024 * 1024);

            System.out.println("Bad targets\n***********\n");

            HttpUrl url = HttpUrl.get("https://dns.cloudflare.com/.not-so-well-known/run-dmc-query");
            List<DnsOverHttps> badProviders = Collections.singletonList(
                    new DnsOverHttps.Builder().client(bootstrapClient).url(url).post(true).build());
            runBatch(badProviders, names);

            System.out.println("cached first run\n****************\n");
            names = Arrays.asList("google.com", "graph.facebook.com");
            bootstrapClient = bootstrapClient.newBuilder().cache(dnsCache).build();
            dnsProviders = DohProviders.providers(bootstrapClient, true, true, true);
            runBatch(dnsProviders, names);

            System.out.println("cached second run\n*****************\n");
            dnsProviders = DohProviders.providers(bootstrapClient, true, true, true);
            runBatch(dnsProviders, names);
        } finally {
            bootstrapClient.connectionPool().evictAll();
            bootstrapClient.dispatcher().executorService().shutdownNow();
            Cache cache = bootstrapClient.cache();
            if (cache != null) {
                cache.close();
            }
        }
    }

    private static void runBatch(List<DnsOverHttps> dnsProviders, List<String> names) {
        long time = System.currentTimeMillis();

        for (DnsOverHttps dns : dnsProviders) {
            System.out.println("Testing " + dns.url());

            for (String host : names) {
                System.out.print(host + ": ");
                System.out.flush();

                try {
                    List<InetAddress> results = dns.lookup(host);
                    System.out.println(results);
                } catch (UnknownHostException uhe) {
                    Throwable e = uhe;

                    while (e != null) {
                        System.out.println(e.toString());

                        e = e.getCause();
                    }
                }
            }

            System.out.println();
        }

        time = System.currentTimeMillis() - time;

        System.out.println("Time: " + (((double) time) / 1000) + " seconds\n");
    }
}
