package com.agent.keepaliveui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private TextView statusText;
    private Switch swFreeze;
    private CheckBox cbAllUsers;
    private EditText etWatch;
    private Spinner spUser;
    private ListView listApps;
    private LinearLayout protectBox;

    private List<Integer> users = new ArrayList<>();
    private int curUser = 0;
    private List<Ctl.AppInfo> curApps = new ArrayList<>();
    private Set<String> protectPkgs = new HashSet<>();
    private AppAdapter adapter;

    private boolean suppressSave = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!RootShell.available()) {
            TextView tv = new TextView(this);
            tv.setText("未检测到 root 权限。\n本应用需要 root 才能读写模块配置。");
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(24), dp(24), dp(24), dp(24));
            setContentView(tv);
            return;
        }

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("UnlimitedKeepAlive");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        statusText = new TextView(this);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusText.setTextColor(Color.GRAY);
        statusText.setPadding(0, dp(12), 0, dp(12));
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvFreeze = new TextView(this);
        tvFreeze.setText("冻结 PowerKeeper");
        tvFreeze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        row1.addView(tvFreeze, new LinearLayout.LayoutParams(0, -2, 1));
        swFreeze = new Switch(this);
        row1.addView(swFreeze);
        root.addView(row1, new LinearLayout.LayoutParams(-1, -2));

        cbAllUsers = new CheckBox(this);
        cbAllUsers.setText("保护所有用户（含全部分身）");
        cbAllUsers.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        root.addView(cbAllUsers, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout rowWatch = new LinearLayout(this);
        rowWatch.setOrientation(LinearLayout.HORIZONTAL);
        rowWatch.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvWatch = new TextView(this);
        tvWatch.setText("补刷间隔（秒，0=仅开机）");
        tvWatch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        rowWatch.addView(tvWatch, new LinearLayout.LayoutParams(0, -2, 1));
        etWatch = new EditText(this);
        etWatch.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etWatch.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        etWatch.setGravity(Gravity.CENTER);
        rowWatch.addView(etWatch, new LinearLayout.LayoutParams(dp(100), -2));
        root.addView(rowWatch, new LinearLayout.LayoutParams(-1, -2));

        Button btnApply = new Button(this);
        btnApply.setText("保存并应用");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0, dp(8), 0, dp(8));
        root.addView(btnApply, btnLp);

        TextView tvProtect = new TextView(this);
        tvProtect.setText("保护应用列表");
        tvProtect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvProtect.setPadding(0, dp(12), 0, dp(4));
        root.addView(tvProtect, new LinearLayout.LayoutParams(-1, -2));

        protectBox = new LinearLayout(this);
        protectBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(protectBox, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout rowUser = new LinearLayout(this);
        rowUser.setOrientation(LinearLayout.HORIZONTAL);
        rowUser.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvUser = new TextView(this);
        tvUser.setText("选择用户：");
        tvUser.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        rowUser.addView(tvUser);
        spUser = new Spinner(this);
        rowUser.addView(spUser, new LinearLayout.LayoutParams(-2, -2));
        root.addView(rowUser, new LinearLayout.LayoutParams(-1, -2));

        listApps = new ListView(this);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(-1, 0, 1);
        listLp.setMargins(0, dp(8), 0, 0);
        root.addView(listApps, listLp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        setTitle("UnlimitedKeepAlive");

        btnApply.setOnClickListener(v -> saveConfig());
        spUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < users.size()) {
                    curUser = users.get(pos);
                    loadApps();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        loadStatus();
        loadUsers();
    }

    private void loadStatus() {
        new AsyncTask<Void, Void, Ctl.Status>() {
            String err;
            @Override protected Ctl.Status doInBackground(Void... v) {
                try {
                    return Ctl.status();
                } catch (Exception e) {
                    err = e.getMessage();
                    return null;
                }
            }
            @Override protected void onPostExecute(Ctl.Status s) {
                if (s == null) {
                    statusText.setText("读取失败: " + err);
                    return;
                }
                protectPkgs.clear();
                protectPkgs.addAll(s.pkgs);
                swFreeze.setChecked(s.freezePk);
                cbAllUsers.setChecked(s.allUsers);
                etWatch.setText(String.valueOf(s.watchInterval));
                String pkState = s.powerkeeperActive ? "PowerKeeper: 运行中" : "PowerKeeper: 已冻结";
                statusText.setText("模块: " + (s.enabled ? "已启用" : "已停用") + " ｜ " + pkState + "\n" + s.log);
                refreshProtectList();
                suppressSave = false;
                swFreeze.setOnCheckedChangeListener((b, c) -> { if (!suppressSave) saveConfig(); });
                cbAllUsers.setOnCheckedChangeListener((b, c) -> { if (!suppressSave) saveConfig(); });
            }
        }.execute();
    }

    private void loadUsers() {
        new AsyncTask<Void, Void, List<Integer>>() {
            @Override protected List<Integer> doInBackground(Void... v) {
                try {
                    return Ctl.users();
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
            @Override protected void onPostExecute(List<Integer> list) {
                users = list.isEmpty() ? java.util.Collections.singletonList(0) : list;
                List<String> names = new ArrayList<>();
                for (int u : users) names.add(u == 0 ? "主空间 (0)" : "分身 (" + u + ")");
                ArrayAdapter<String> ad = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names);
                spUser.setAdapter(ad);
                curUser = users.get(0);
                loadApps();
            }
        }.execute();
    }

    private void loadApps() {
        new AsyncTask<Void, Void, List<Ctl.AppInfo>>() {
            String err;
            @Override protected List<Ctl.AppInfo> doInBackground(Void... v) {
                try {
                    List<Ctl.AppInfo> third = Ctl.apps(curUser, false);
                    List<Ctl.AppInfo> sys = Ctl.apps(curUser, true);
                    third.addAll(sys);
                    return third;
                } catch (Exception e) {
                    err = e.getMessage();
                    return new ArrayList<>();
                }
            }
            @Override protected void onPostExecute(List<Ctl.AppInfo> list) {
                curApps = list;
                adapter = new AppAdapter(MainActivity.this, curApps, protectPkgs, (pkg, checked) -> {
                    if (checked) {
                        protectPkgs.add(pkg);
                        runCtl("addpkg " + pkg, "已添加 " + pkg);
                    } else {
                        protectPkgs.remove(pkg);
                        runCtl("rmpkg " + pkg, "已移除 " + pkg);
                    }
                    refreshProtectList();
                });
                listApps.setAdapter(adapter);
                if (err != null) toast("加载应用列表失败: " + err);
            }
        }.execute();
    }

    private void refreshProtectList() {
        protectBox.removeAllViews();
        for (final String pkg : protectPkgs) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView tv = new TextView(this);
            tv.setText(pkg);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.addView(tv, new LinearLayout.LayoutParams(0, -2, 1));
            Button btnRm = new Button(this);
            btnRm.setText("移除");
            btnRm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnRm.setOnClickListener(v -> {
                protectPkgs.remove(pkg);
                runCtl("rmpkg " + pkg, "已移除 " + pkg);
                refreshProtectList();
                if (adapter != null) adapter.notifyDataSetChanged();
            });
            row.addView(btnRm);
            protectBox.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void saveConfig() {
        final String watch = etWatch.getText().toString();
        if (watch.isEmpty()) {
            toast("补刷间隔不能为空");
            return;
        }
        new AsyncTask<Void, Void, Boolean>() {
            String err;
            @Override protected Boolean doInBackground(Void... v) {
                try {
                    Ctl.set("FREEZE_POWERKEEPER", swFreeze.isChecked() ? "1" : "0");
                    Ctl.set("ALL_USERS", cbAllUsers.isChecked() ? "1" : "0");
                    Ctl.set("WATCH_INTERVAL", watch);
                    Ctl.apply();
                    return true;
                } catch (Exception e) {
                    err = e.getMessage();
                    return false;
                }
            }
            @Override protected void onPostExecute(Boolean ok) {
                if (ok) toast("已保存并应用");
                else toast("保存失败: " + err);
            }
        }.execute();
    }

    private void runCtl(final String args, final String okMsg) {
        new AsyncTask<Void, Void, Boolean>() {
            String err;
            @Override protected Boolean doInBackground(Void... v) {
                try {
                    RootShell.exec("sh /data/adb/modules/unlimited_keepalive/webui_ctl.sh " + args);
                    return true;
                } catch (Exception e) {
                    err = e.getMessage();
                    return false;
                }
            }
            @Override protected void onPostExecute(Boolean ok) {
                if (ok) toast(okMsg);
                else toast("操作失败: " + err);
            }
        }.execute();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
