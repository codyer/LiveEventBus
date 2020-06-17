/*
 * ************************************************************
 * 文件：MultiProcessImpl.java  模块：ipc  项目：ElegantBus
 * 当前修改时间：2020年06月17日 02:23:52
 * 上次修改时间：2020年06月17日 02:16:02
 * 作者：Cody.yi   https://github.com/codyer
 *
 * 描述：ipc
 * Copyright (c) 2020
 * ************************************************************
 */

package cody.bus;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.alibaba.fastjson.JSON;


/**
 * 支持进程间事件总线的扩展，每个进程有一个实例
 * messenger 实现
 */
class MultiProcessImpl implements BusFactory.MultiProcess {
    private String mPkgName;
    private Context mContext;
    private final String mProcessName;
    private ProcessManager mProcessManager;

    final class ProcessManager {
        Messenger messenger;

        ProcessManager(final Messenger messenger) {
            this.messenger = messenger;
        }

        void post(final EventWrapper eventWrapper) throws RemoteException {
            Message message = Message.obtain(null, ProcessManagerService.MSG_POST_TO_SERVICE);
            message.replyTo = mProcessMessenger;
            message.obj = eventWrapper;
            messenger.send(message);
        }

        void register() throws RemoteException {
            doRegister(ProcessManagerService.MSG_REGISTER);
        }

        IBinder asBinder() {
            return messenger.getBinder();
        }

        void unregister() throws RemoteException {
            doRegister(ProcessManagerService.MSG_UNREGISTER);
        }

        private void doRegister(final int what) throws RemoteException {
            Message message = Message.obtain(null, what);
            message.replyTo = mProcessMessenger;
            message.obj = mProcessName;
            messenger.send(message);
        }
    }

    @SuppressLint("HandlerLeak")
    private Messenger mProcessMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ProcessManagerService.MSG_ON_POST:
                    postToCurrentProcess((EventWrapper) msg.obj, false);
                    break;
                case ProcessManagerService.MSG_ON_POST_STICKY:
                    postToCurrentProcess((EventWrapper) msg.obj, true);
                    break;
            }
            super.handleMessage(msg);
        }
    });

    private MultiProcessImpl() {
        mProcessName = Application.getProcessName();
        BusFactory.setDelegate(this);
    }

    private final static class InstanceHolder {
        @SuppressLint("StaticFieldLeak")
        static final MultiProcessImpl INSTANCE = new MultiProcessImpl();
    }

    private static MultiProcessImpl ready() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 进程创建时调用，一般在 Application 的 onCreate 中调用
     * 多应用且多进程场景请使用
     *
     * @param context 上下文
     * @param pkgName 共享服务且常驻的包名
     *                如果是单应用，即为应用的包名
     *                如果是多个应用，即为常驻的主应用的包名
     *                主应用必须安装，否则不能正常运行
     */
    static void support(Context context, String pkgName) {
        ready().mContext = context;
        ready().mPkgName = pkgName;
        ready().bindService();
    }

    /**
     * 进程结束时调用，一般在 Application 的 onTerminate 中调用
     */
    static void stopSupport() {
        ready().unbindService();
    }

    @Override
    public String pkgName() {
        return mPkgName;
    }

    @Override
    public <T> void postToService(EventWrapper eventWrapper, T value) {
        try {
            if (mProcessManager == null) {
                bindService();
            } else {
                eventWrapper.json = JSON.toJSONString(value);
                mProcessManager.post(eventWrapper);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mProcessManager = new ProcessManager(new Messenger(service));
            try {
                mProcessManager.register();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mProcessManager = null;
            ElegantLog.d("onServiceDisconnected, process = " + mProcessName);
        }
    };

    private void postToCurrentProcess(EventWrapper eventWrapper, final boolean sticky) {
        Object value = null;
        try {
            value = JSON.parseObject(eventWrapper.json, Class.forName(eventWrapper.type));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (value == null) return;
        if (sticky) {
            BusFactory.ready().create(eventWrapper).postStickyToCurrentProcess(value);
        } else {
            BusFactory.ready().create(eventWrapper).postToCurrentProcess(value);
        }
    }

    private void bindService() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(pkgName(), ProcessManagerService.CLASS_NAME));
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        mContext.unbindService(mServiceConnection);
        if (mProcessManager != null && mProcessManager.asBinder().isBinderAlive()) {
            try {
                // 取消注册
                mProcessManager.unregister();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mContext = null;
    }
}