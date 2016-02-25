package com.Quhuhu.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.Quhuhu.R;
import com.Quhuhu.netcenter.utils.SystemSupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wally.yan on 2016/1/4.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static CrashHandler INSTANCE;
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CrashHandler();
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成. 开发者可以根据自己的情况来自定义异常处理逻辑
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false
     */
    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return true;
        }
        String environment = mContext.getResources().getString(R.string.environment);
        if ("0".equals(environment)) {
            return false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                //保存日志文件
                saveCrash(ex);
            }
        }).start();
        return false;
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    private String saveCrash(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        sb.append("手机品牌" + "=" + Build.MODEL + "\n");
        sb.append("系统版本" + "=" + Build.VERSION.RELEASE + "\n");
        sb.append("app版本" + "=" + SystemSupport.getAppVersion(mContext) + "\n");

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory() + "/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
