package com.kaisar.xposed.godmode.service;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.IGodModeManager;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.RuleProvider;
import com.kaisar.xposed.godmode.injection.GodModeInjector;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.BitmapHelper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class RemoteGMManager extends IGodModeManager.Stub {

    public static final RemoteGMManager INSTANCE = new RemoteGMManager();
    private static boolean mInited;
    private static Uri mConfigUri;
    private static WeakReference<Context> mContext = null;
    private static XC_LoadPackage.LoadPackageParam mPParam;
    private static File mLocalRules;

    public static void init(Context pCon, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (mInited) return;
        mInited = true;

        if (pCon instanceof Application) {
            mContext = new WeakReference<>(pCon);
        } else {
            mContext = new WeakReference<>(pCon.getApplicationContext());
        }
        mPParam = loadPackageParam;

        mConfigUri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".viewRule?pn=" + mContext.get().getPackageName());
        GodModeInjector.notifyEditModeChanged(INSTANCE.isInEditMode());
        mLocalRules = new File(GodModeApplication.getBaseDir(pCon), "rule_cache.json");
        updateRules();
    }

    public static void updateRules() {
        GodModeInjector.notifyViewRulesChanged(INSTANCE.getRules(mPParam.packageName));
    }

    @Override
    public boolean hasLight() throws RemoteException {
        return false;
    }

    @Override
    public void setEditMode(boolean enable) throws RemoteException {

    }

    @Override
    public boolean isInEditMode() {
        Uri.Builder tBuilder = mConfigUri.buildUpon().path(RuleProvider.PATH_EDIT_MODE);
        String tStr = mContext.get().getContentResolver().getType(tBuilder.build());
        return !TextUtils.isEmpty(tStr) && Boolean.parseBoolean(tStr);
    }

    @Override
    public void addObserver(String packageName, IObserver observer) throws RemoteException {

    }

    @Override
    public void removeObserver(String packageName, IObserver observer) throws RemoteException {

    }

    @Override
    public AppRules getAllRules() throws RemoteException {
        return null;
    }

    @Override
    public ActRules getRules(String packageName) {
        Uri.Builder tBuilder = mConfigUri.buildUpon().path(RuleProvider.PATH_GET_RULES);
        String tJson = mContext.get().getContentResolver().getType(tBuilder.build());

        boolean tSave = true;
        if (tJson == null) {
            Logger.i("GodMode", "未获取到ViewRule配置, 尝试载入本地缓存规则");
            tJson = FileUtils.readContent(mLocalRules);
            tSave = false;
        }

        ActRules rules = ActRules.EMPTY;
        if (!tJson.isEmpty()) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                rules = gson.fromJson(tJson, ActRules.class);
                if (rules == null) rules = ActRules.EMPTY;
            } catch (Throwable e) {
                Logger.e("GodMode", "获取ViewRule配置时出错!", e);
                return ActRules.EMPTY;
            }
        }
        Logger.d("GodMode", "已获取到" + rules.size() + "条规则");
        if (tSave) saveRuleToLocal(rules);

        return rules;
    }


    private void saveRuleToLocal(ActRules pRules) {
        FileUtils.writeData(mLocalRules, new Gson().toJson(pRules));
    }

    @Override
    public boolean writeRule(String packageName, ViewRule viewRule, Bitmap pIcon) throws RemoteException {
        Logger.i("GodMode", "新增屏蔽规则: " + viewRule);
        ActRules actRules = GodModeInjector.actRuleProp.get();
        List<ViewRule> viewRules = actRules.get(viewRule.activityClass);
        if (viewRules == null) {
            actRules.put(viewRule.activityClass, viewRules = new ArrayList<>());
        }
        viewRules.add(viewRule);
        //getType测试
        Uri.Builder tBuilder = mConfigUri.buildUpon().path(RuleProvider.PATH_WRITE_RULE);
        String tKey = new Random(System.currentTimeMillis()).nextInt(100000000) + "";
        tBuilder.appendQueryParameter("key", tKey);
        tBuilder.appendQueryParameter("rule", new Gson().toJson(viewRule));
        ContentResolver tResolver = mContext.get().getContentResolver();
        String tUriStr = tResolver.getType(tBuilder.build());
        if (tUriStr != null) {
            Uri tImgUri = Uri.parse(tUriStr);
            OutputStream tOStream = null;
            try {
                tOStream = tResolver.openOutputStream(tImgUri);
                tOStream.write(BitmapHelper.iconToBArr(pIcon));
            } catch (IOException e) {
                Logger.e("GodMode", "无法写入规则图片快照", e);
            } finally {
                FileUtils.closeStream(tOStream);
                // 不管是否成功写入,都需要向远端发送消息以删除缓存的数据
                tBuilder = mConfigUri.buildUpon().path(RuleProvider.PATH_RULE_IMG);
                tBuilder.appendQueryParameter("key", tKey);
                tResolver.getType(tBuilder.build());
            }
        }

        return true;
    }

    @Override
    public boolean updateRule(String packageName, ViewRule viewRule) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteRule(String packageName, ViewRule viewRule) throws RemoteException {
        Uri.Builder tBuilder = mConfigUri.buildUpon().path(RuleProvider.PATH_DELETE_RULE);
        tBuilder.appendQueryParameter("rule", new Gson().toJson(viewRule));
        return Boolean.parseBoolean(mContext.get().getContentResolver().getType(tBuilder.build()));
    }

    @Override
    public boolean deleteRules(String packageName) throws RemoteException {
        return false;
    }

    @Override
    public ParcelFileDescriptor openImageFileDescriptor(String filePath) throws RemoteException {
        return null;
    }
}
