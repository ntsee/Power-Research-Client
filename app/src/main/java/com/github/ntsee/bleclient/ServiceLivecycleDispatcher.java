package com.github.ntsee.bleclient;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Helper class to dispatch lifecycle events for a service. Use it only if it is impossible
 * to use {@link LifecycleService}.
 */
@SuppressWarnings("WeakerAccess")
class ServiceLifecycleDispatcher {
    private final LifecycleRegistry mRegistry;
    private final Handler mHandler;
    private DispatchRunnable mLastDispatchRunnable;
    /**
     * @param provider {@link LifecycleOwner} for a service, usually it is a service itself
     */
    public ServiceLifecycleDispatcher(@NonNull LifecycleOwner provider) {
        mRegistry = new LifecycleRegistry(provider);
        mHandler = new Handler();
    }
    private void postDispatchRunnable(Lifecycle.Event event) {
        if (mLastDispatchRunnable != null) {
            mLastDispatchRunnable.run();
        }
        mLastDispatchRunnable = new DispatchRunnable(mRegistry, event);
        mHandler.postAtFrontOfQueue(mLastDispatchRunnable);
    }
    /**
     * Must be a first call in {@link Service#onCreate()} method, even before super.onCreate call.
     */
    public void onServicePreSuperOnCreate() {
        postDispatchRunnable(Lifecycle.Event.ON_CREATE);
    }
    /**
     * Must be a first call in {@link Service#onBind(Intent)} method, even before super.onBind
     * call.
     */
    public void onServicePreSuperOnBind() {
        postDispatchRunnable(Lifecycle.Event.ON_START);
    }
    /**
     * Must be a first call in {@link Service#onStart(Intent, int)} or
     * {@link Service#onStartCommand(Intent, int, int)} methods, even before
     * a corresponding super call.
     */
    public void onServicePreSuperOnStart() {
        postDispatchRunnable(Lifecycle.Event.ON_START);
    }
    /**
     * Must be a first call in {@link Service#onDestroy()} method, even before super.OnDestroy
     * call.
     */
    public void onServicePreSuperOnDestroy() {
        postDispatchRunnable(Lifecycle.Event.ON_STOP);
        postDispatchRunnable(Lifecycle.Event.ON_DESTROY);
    }
    /**
     * @return {@link Lifecycle} for the given {@link LifecycleOwner}
     */
    public Lifecycle getLifecycle() {
        return mRegistry;
    }
    static class DispatchRunnable implements Runnable {
        private final LifecycleRegistry mRegistry;
        final Lifecycle.Event mEvent;
        private boolean mWasExecuted = false;
        DispatchRunnable(@NonNull LifecycleRegistry registry, Lifecycle.Event event) {
            mRegistry = registry;
            mEvent = event;
        }
        @Override
        public void run() {
            if (!mWasExecuted) {
                mRegistry.handleLifecycleEvent(mEvent);
                mWasExecuted = true;
            }
        }
    }
}