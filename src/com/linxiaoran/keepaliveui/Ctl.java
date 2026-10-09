package com.linxiaoran.keepaliveui;

import java.util.ArrayList;
import java.util.List;

public class Ctl {
    private static final String MOD = "/data/adb/modules/unlimited_keepalive";
    private static final String CTL = MOD + "/webui_ctl.sh";

    public static class Status {
        public boolean enabled;
        public boolean freezePk;
        public boolean allUsers;
        public int watchInterval;
        public boolean powerkeeperActive;
        public List<Integer> users = new ArrayList<>();
        public List<String> pkgs = new ArrayList<>();
        public String log = "";
    }

    public static class AppInfo {
        public String pkg;
        public String label;
    }

    private static String ctl(String args) throws Exception {
        return RootShell.exec("sh " + CTL + " " + args);
    }

    public static Status status() throws Exception {
        String json = ctl("status");
        Status s = new Status();
        s.enabled = json.contains("\"enabled\":1");
        s.freezePk = json.contains("\"freeze_pk\":1");
        s.allUsers = json.contains("\"all_users\":1");
        s.watchInterval = extractInt(json, "watch_interval");
        s.powerkeeperActive = json.contains("\"powerkeeper\":\"active\"");
        s.users = extractIntArray(json, "users");
        s.pkgs = extractPkgList(json);
        s.log = extractString(json, "log");
        return s;
    }

    public static void set(String key, String value) throws Exception {
        ctl("set " + key + " " + value);
    }

    public static void addPkg(String pkg) throws Exception {
        ctl("addpkg " + pkg);
    }

    public static void rmPkg(String pkg) throws Exception {
        ctl("rmpkg " + pkg);
    }

    public static void apply() throws Exception {
        ctl("apply");
    }

    public static List<Integer> users() throws Exception {
        String out = ctl("users");
        List<Integer> list = new ArrayList<>();
        for (String line : out.split("\\s+")) {
            try {
                list.add(Integer.parseInt(line.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    public static List<AppInfo> apps(int user, boolean system) throws Exception {
        String out = ctl("apps " + user + " " + (system ? "s" : "3"));
        List<AppInfo> list = new ArrayList<>();
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            AppInfo a = new AppInfo();
            int pipe = line.indexOf('|');
            if (pipe >= 0) {
                a.pkg = line.substring(0, pipe);
                a.label = line.substring(pipe + 1);
            } else {
                a.pkg = line;
                a.label = null;
            }
            list.add(a);
        }
        return list;
    }

    private static int extractInt(String json, String key) {
        String pat = "\"" + key + "\":";
        int i = json.indexOf(pat);
        if (i < 0) return 0;
        i += pat.length();
        int j = i;
        while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '-')) j++;
        try {
            return Integer.parseInt(json.substring(i, j));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Integer> extractIntArray(String json, String key) {
        List<Integer> list = new ArrayList<>();
        String pat = "\"" + key + "\":[";
        int i = json.indexOf(pat);
        if (i < 0) return list;
        int j = json.indexOf(']', i);
        if (j < 0) return list;
        for (String part : json.substring(i + pat.length(), j).split(",")) {
            try {
                list.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    private static List<String> extractPkgList(String json) {
        List<String> list = new ArrayList<>();
        String pat = "\"pkgs\":[";
        int i = json.indexOf(pat);
        if (i < 0) return list;
        int j = json.indexOf(']', i);
        if (j < 0) return list;
        String body = json.substring(i + pat.length(), j);
        for (String item : body.split("\\},\\{")) {
            String p = "\"pkg\":\"";
            int k = item.indexOf(p);
            if (k >= 0) {
                k += p.length();
                int e = item.indexOf('"', k);
                if (e > k) list.add(item.substring(k, e));
            }
        }
        return list;
    }

    private static String extractString(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int i = json.indexOf(pat);
        if (i < 0) return "";
        i += pat.length();
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') sb.append('\n');
                else if (n == 't') sb.append('\t');
                else sb.append(n);
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
