package com.linxiaoran.keepaliveui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootShell {

    public static String exec(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
        BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = out.readLine()) != null) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        StringBuilder esb = new StringBuilder();
        while ((line = err.readLine()) != null) {
            if (esb.length() > 0) esb.append('\n');
            esb.append(line);
        }
        p.waitFor();
        int code = p.exitValue();
        if (code != 0) {
            throw new RuntimeException("exit=" + code + " " + esb);
        }
        return sb.toString();
    }

    public static boolean available() {
        try {
            exec("id");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
