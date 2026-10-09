package com.agent.keepaliveui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class AppAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<Ctl.AppInfo> apps;
    private final PackageManager pm;
    private final java.util.Set<String> selected;
    private final OnToggleListener listener;

    public interface OnToggleListener {
        void onToggle(String pkg, boolean checked);
    }

    public AppAdapter(Context ctx, List<Ctl.AppInfo> apps, java.util.Set<String> selected, OnToggleListener listener) {
        this.ctx = ctx;
        this.apps = apps;
        this.pm = ctx.getPackageManager();
        this.selected = selected;
        this.listener = listener;
    }

    @Override public int getCount() { return apps.size(); }
    @Override public Object getItem(int i) { return apps.get(i); }
    @Override public long getItemId(int i) { return i; }

    static class VH {
        ImageView icon;
        TextView name;
        TextView pkg;
        CheckBox check;
    }

    @Override
    public View getView(int position, View convert, ViewGroup parent) {
        VH h;
        if (convert == null) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = dp(12);
            row.setPadding(pad, pad, pad, pad);

            h = new VH();
            h.icon = new ImageView(ctx);
            int isz = dp(40);
            row.addView(h.icon, new LinearLayout.LayoutParams(isz, isz));

            LinearLayout texts = new LinearLayout(ctx);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
            tlp.setMargins(dp(12), 0, dp(12), 0);
            row.addView(texts, tlp);

            h.name = new TextView(ctx);
            h.name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
            h.name.setSingleLine(true);
            texts.addView(h.name);

            h.pkg = new TextView(ctx);
            h.pkg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            h.pkg.setTextColor(android.graphics.Color.GRAY);
            h.pkg.setSingleLine(true);
            texts.addView(h.pkg);

            h.check = new CheckBox(ctx);
            row.addView(h.check);

            row.setTag(h);
            convert = row;
        } else {
            h = (VH) convert.getTag();
        }

        final Ctl.AppInfo app = apps.get(position);
        String label = app.label;
        if (label == null || label.isEmpty()) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(app.pkg, 0);
                label = ai.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException e) {
                label = app.pkg;
            }
        }
        h.name.setText(label);
        h.pkg.setText(app.pkg);

        Drawable icon = null;
        try {
            icon = pm.getApplicationIcon(app.pkg);
        } catch (PackageManager.NameNotFoundException ignored) {}
        h.icon.setImageDrawable(icon != null ? icon : ctx.getResources().getDrawable(android.R.drawable.sym_def_app_icon));

        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(selected.contains(app.pkg));
        h.check.setOnCheckedChangeListener((btn, checked) -> listener.onToggle(app.pkg, checked));
        convert.setOnClickListener(v -> h.check.toggle());
        return convert;
    }

    private int dp(int v) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }
}
