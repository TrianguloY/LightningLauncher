package com.google.android.hotword.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHotwordService extends IInterface {
    void requestHotwordDetection(String packageName, boolean detect) throws RemoteException;

    abstract class Stub extends Binder implements IHotwordService {
        static final int TRANSACTION_requestHotwordDetection = 1;
        private static final String DESCRIPTOR = "com.google.android.hotword.service.IHotwordService";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IHotwordService asInterface(IBinder binder) {
            if (binder == null)
                return null;
            IInterface iInterface = binder.queryLocalInterface(DESCRIPTOR);
            if (iInterface != null && iInterface instanceof IHotwordService)
                return (IHotwordService) iInterface;
            return new Proxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                default:
                    return super.onTransact(code, data, reply, flags);
                case INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                case TRANSACTION_requestHotwordDetection:
                    data.enforceInterface(DESCRIPTOR);
                    String packageName = data.readString();
                    boolean detect = data.readInt() == 1;
                    requestHotwordDetection(packageName, detect);
                    return true;
            }
        }

        private static class Proxy implements IHotwordService {
            private final IBinder mRemote;

            Proxy(IBinder iBinder) {
                mRemote = iBinder;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            public void requestHotwordDetection(String packageName, boolean detect) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(getInterfaceDescriptor());
                    data.writeString(packageName);
                    data.writeInt(detect ? 1 : 0);
                    mRemote.transact(TRANSACTION_requestHotwordDetection, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }
        }
    }
}