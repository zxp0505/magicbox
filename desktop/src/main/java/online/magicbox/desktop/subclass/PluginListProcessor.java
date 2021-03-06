package online.magicbox.desktop.subclass;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.georgeyang.database.Mdb;
import cn.georgeyang.util.Logutil;
import online.magicbox.desktop.entity.AppInfoBean;
import online.magicbox.desktop.entity.PluginItemEntity;
import online.magicbox.lib.PluginActivity;
import online.magicbox.lib.PluginConfig;

/**
 * Created by george.yang on 2016-4-6.
 */
public class PluginListProcessor {
    private Context mcontext;
    private String appDownloadDir;
    private Mdb mdb;
    public PluginListProcessor (Context context) {
        mcontext = context;
        appDownloadDir = mcontext.getFilesDir().getAbsolutePath();
        mdb = new Mdb(context);
    }

    public List<AppInfoBean> doSomething(List<PluginItemEntity> pluginList) {
        List<AppInfoBean> ret = new ArrayList<>();

        for (PluginItemEntity pluginItemBean:pluginList) {
            if ("y".equalsIgnoreCase(pluginItemBean.isDel)){
                continue;
            }
            AppInfoBean infoBean = new AppInfoBean();
            infoBean.imageUrl = pluginItemBean.imageUrl;
            infoBean.name = pluginItemBean.name;
            infoBean.type = AppInfoBean.Type_plugin;
            infoBean.downUrl = pluginItemBean.downUrl;
            infoBean.packageName = pluginItemBean.packageName;
            infoBean.size = pluginItemBean.size;
            infoBean.mainClass = pluginItemBean.mainClass;
            infoBean.lastVersionCode = pluginItemBean.versionCode;
            infoBean.installVersionCode = pluginItemBean.versionCode;

            PluginItemEntity dbBean = mdb.findOnebyWhereDesc(PluginItemEntity.class,"_addTime",String.format("packageName='%s'",new Object[]{pluginItemBean.packageName}));
            if (dbBean==null) {
                pluginItemBean._id = pluginItemBean.Id;
                pluginItemBean.save();
            } else {
                pluginItemBean.installVersionCode = dbBean.installVersionCode;
                infoBean.installVersionCode = dbBean.installVersionCode;
            }

            String apkFileName = String.format("%s_%s.apk",new Object[]{pluginItemBean.packageName,pluginItemBean.installVersionCode+""});
            Logutil.showlog("apk:" + apkFileName);
            File pluginFile = new File(appDownloadDir,apkFileName);
            boolean hasInstall = pluginFile.exists();
            infoBean.isInstall = hasInstall;

            if (hasInstall) {
                Intent intent = PluginActivity.buildIntent(mcontext,pluginItemBean.packageName,pluginItemBean.mainClass, PluginConfig.System,pluginItemBean.installVersionCode+"");
                infoBean.intent = intent;
                infoBean.installTime = pluginFile.lastModified();
            } else {
                Intent intent = PluginActivity.buildIntent(mcontext,pluginItemBean.packageName,pluginItemBean.mainClass, PluginConfig.System,pluginItemBean.versionCode+"");
                infoBean.intent = intent;
                infoBean.installTime = System.currentTimeMillis();
            }

            ret.add(infoBean);
        }

        ret.addAll(getPhoneAppList());
        Collections.sort(ret);
        return ret;
    }

    private List<AppInfoBean> getPhoneAppList () {
        List<AppInfoBean> appInfoBeanList = new ArrayList<>();
        //列出普通应用程序
        PackageManager pm = mcontext.getPackageManager();
        //得到系统安装的所有程序包的PackageInfo对象
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for(PackageInfo pi:packages) {
            try {
                AppInfoBean infoBean = new AppInfoBean();
                infoBean.isInstall = true;
                infoBean.name = pi.applicationInfo.loadLabel(pm)+"";
                infoBean.packageName = pi.applicationInfo.packageName;
                if (mcontext.getPackageName().equals(infoBean.packageName)) {
                    continue;
                }
                String dir = pi.applicationInfo.publicSourceDir;
                infoBean.installTime = new File(dir).lastModified();
                infoBean.icon = pi.applicationInfo.loadIcon(pm);
                Intent intent = pm.getLaunchIntentForPackage(pi.packageName);
                if (intent !=null) {
                    infoBean.intent = intent;
                    if((pi.applicationInfo.flags& ApplicationInfo.FLAG_SYSTEM)!=0) {
                        infoBean.type = AppInfoBean.Type_System;
                    } else if((pi.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM) == 0) {
                        infoBean.type = AppInfoBean.Type_Normal;
                    }
                    appInfoBeanList.add(infoBean);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return appInfoBeanList;
    }
}
