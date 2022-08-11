package com.jrai.flutter_keyboard_visibility;

import static android.content.Context.UI_MODE_SERVICE;

import android.app.Activity;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry;


public class FlutterKeyboardVisibilityPlugin implements FlutterPlugin, ActivityAware, EventChannel.StreamHandler, ViewTreeObserver.OnGlobalLayoutListener {
    private EventChannel.EventSink eventSink;
    private View mainView;
    private boolean isVisible;
    private boolean isTv;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        init(flutterPluginBinding.getBinaryMessenger());
        checkIsTv(flutterPluginBinding.getApplicationContext());
    }

    private void checkIsTv(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        isTv = uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void init(BinaryMessenger messenger) {
        final EventChannel eventChannel = new EventChannel(messenger, "flutter_keyboard_visibility");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        unregisterListener();
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        listenForKeyboard(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        unregisterListener();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        listenForKeyboard(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivity() {
        unregisterListener();
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void onCancel(Object o) {
        this.eventSink = null;
    }

    @Override
    public void onGlobalLayout() {
        if (mainView != null) {
            if (isTv) {
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(mainView);
                if (insets != null) {
                    boolean newState = insets.isVisible(WindowInsetsCompat.Type.ime());
                    onNewState(newState);
                }
            } else {
                Rect r = new Rect();
                mainView.getWindowVisibleDisplayFrame(r);

                boolean newState = ((double) r.height() / (double) mainView.getRootView().getHeight()) < 0.85;
                onNewState(newState);
            }
        }
    }

    private void onNewState(boolean newState) {
        if (newState != isVisible) {
            isVisible = newState;
            if (eventSink != null) {
                eventSink.success(isVisible ? 1 : 0);
            }
        }
    }

    private void listenForKeyboard(Activity activity) {
        mainView = activity.<ViewGroup>findViewById(android.R.id.content);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    private void unregisterListener() {
        if (mainView != null) {
            mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            mainView = null;
        }
    }
}
