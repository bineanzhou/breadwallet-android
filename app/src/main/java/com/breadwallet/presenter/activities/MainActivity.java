package com.breadwallet.presenter.activities;

import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.FragmentAbout;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentWipeWallet;
import com.breadwallet.presenter.fragments.MainFragmentDecoder;
import com.breadwallet.presenter.fragments.MainFragmentSettingsAll;
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.others.CurrencyManager;
import com.breadwallet.tools.others.NetworkChangeReceiver;

import java.util.HashMap;
import java.util.Map;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/4/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static MainActivity app;
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public CustomPagerAdapter pagerAdapter;
    public static RelativeLayout pageIndicator;
    public ImageView pageIndicatorLeft;
    public ImageView pageIndicatorRight;
    public View middleView;
    public Map<String, Integer> burgerButtonMap;
    public Button burgerButton;
    public Button locker;
    public MainFragmentSettingsAll mainFragmentSettingsAll;
    public static ParallaxViewPager parallaxViewPager;
    public FragmentSettings fragmentSettings;
    public FragmentAbout fragmentAbout;
    public MainFragmentDecoder mainFragmentDecoder;
    public ClipboardManager myClipboard;
    public FragmentCurrency fragmentCurrency;
    public FragmentRecoveryPhrase fragmentRecoveryPhrase;
    public FragmentWipeWallet fragmentWipeWallet;
    public RelativeLayout burgerButtonLayout;
    public RelativeLayout lockerButtonLayout;
    public FragmentScanResult fragmentScanResult;
    private boolean doubleBackToExitPressedOnce;
    public static final int BURGER = 0;
    public static final int CLOSE = 1;
    public static final int BACK = 2;
    public static final float PAGE_INDICATOR_SCALE_UP = 1.3f;
    public static boolean beenThroughSavedInstanceMethod = false;
    public ViewFlipper viewFlipper;
    public PasswordDialogFragment passwordDialogFragment;
    public RelativeLayout networkErrorBar;
    private NetworkChangeReceiver receiver = new NetworkChangeReceiver();
    public static boolean unlocked = false;
    public static final String UNLOCKED = "unlocked";

    /**
     * Public constructor used to assign the current instance to the app variable
     */

    public MainActivity() {
        app = this;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        beenThroughSavedInstanceMethod = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "Activity created!");
        if (savedInstanceState != null) {
            return;
        }
        final FragmentManager fm = getSupportFragmentManager();
        initializeViews();

        burgerButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(burgerButton);
                if (FragmentAnimator.level > 1 || scanResultFragmentOn || decoderFragmentOn) {
                    Log.e(TAG, "CHECK:Should press back!");
                    app.onBackPressed();
                } else {
                    //check multi pressing availability here, because method onBackPressed does the checking as well.
                    if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                        FragmentAnimator.pressMenuButton(app, mainFragmentSettingsAll);
                        Log.e(TAG, "CHECK:Should press menu");
                    }
                }
            }
        });
        lockerButtonLayout.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(locker);
                passwordDialogFragment.show(fm, TAG);
            }
        });
        scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);

    }

    @Override
    protected void onResume() {
        super.onResume();
        resetMiddleView();
        networkErrorBar.setVisibility(CurrencyManager.isNetworkAvailable() ? View.GONE : View.VISIBLE);
        startStopReceiver(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "Activity onPause");
        startStopReceiver(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        CurrencyManager.stopTimerTask();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        CurrencyManager.stopTimerTask();
        Log.e(TAG, "Activity Destroyed!");

    }

    /**
     * Initializes all the views and components
     */

    private void initializeViews() {
        burgerButtonLayout = (RelativeLayout) findViewById(R.id.main_burger_button_layout);
        lockerButtonLayout = (RelativeLayout) findViewById(R.id.main_locker_button_layout);
        networkErrorBar = (RelativeLayout) findViewById(R.id.main_internet_status_bar);
        burgerButton = (Button) findViewById(R.id.main_button_burger);
        viewFlipper = (ViewFlipper) MainActivity.app.findViewById(R.id.middle_view_flipper);
        locker = (Button) findViewById(R.id.main_button_locker);
        pageIndicator = (RelativeLayout) findViewById(R.id.main_pager_indicator);
        pageIndicatorLeft = (ImageView) findViewById(R.id.circle_indicator_left);
        middleView = findViewById(R.id.main_label_breadwallet);
        pageIndicatorRight = (ImageView) findViewById(R.id.circle_indicator_right);
        pagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
        burgerButtonMap = new HashMap<>();
        fragmentSettings = new FragmentSettings();
        mainFragmentSettingsAll = new MainFragmentSettingsAll();
        mainFragmentDecoder = new MainFragmentDecoder();
        fragmentAbout = new FragmentAbout();
        fragmentCurrency = new FragmentCurrency();
        fragmentRecoveryPhrase = new FragmentRecoveryPhrase();
        fragmentWipeWallet = new FragmentWipeWallet();
        fragmentScanResult = new FragmentScanResult();
        passwordDialogFragment = new PasswordDialogFragment();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.main_viewpager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        burgerButtonMap.put("burger", R.drawable.burger);
        burgerButtonMap.put("close", R.drawable.x);
        burgerButtonMap.put("back", R.drawable.navigationback);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (FragmentAnimator.level > 1 || scanResultFragmentOn || decoderFragmentOn) {
                this.onBackPressed();
            } else if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                FragmentAnimator.pressMenuButton(app, mainFragmentSettingsAll);
            }
        }
        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
            Log.e(TAG, "onBackPressed!");
            if (FragmentAnimator.wipeWalletOpen) {
                FragmentAnimator.pressWipeWallet(this, fragmentSettings);
                activityButtonsEnable(true);
                return;
            }
            //switch the level of fragments creation.
            switch (FragmentAnimator.level) {
                case 0:
                    if (doubleBackToExitPressedOnce) {
                        super.onBackPressed();
                        break;
                    }
                    if (decoderFragmentOn) {
                        FragmentAnimator.hideDecoderFragment();
                        break;
                    }
                    if (scanResultFragmentOn) {
                        FragmentAnimator.hideScanResultFragment();
                        break;
                    }
                    this.doubleBackToExitPressedOnce = true;
                    ((BreadWalletApp) getApplicationContext()).showCustomToast(this,
                            getResources().getString(R.string.mainactivity_press_back_again), 140,
                            Toast.LENGTH_SHORT);
                    makeDoubleBackToExitPressedOnce(1000);

                    break;
                case 1:
                    FragmentAnimator.pressMenuButton(this, mainFragmentSettingsAll);
                    FragmentAnimator.hideDecoderFragment();
                    break;
                default:
                    FragmentAnimator.animateSlideToRight(this);
                    break;
            }
        }
    }

    /**
     * Sets the little circle indicator to the selected page
     *
     * @patam x The page for the indicator to be shown
     */

    public void setPagerIndicator(int x) {
        if (x == 0) {
            Log.d(TAG, "Left Indicator changed");
            pageIndicatorLeft.setImageResource(R.drawable.circle_indicator_active);
            pageIndicatorRight.setImageResource(R.drawable.circle_indicator);
            scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);
            scaleView(pageIndicatorRight, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP, 1f);
        } else if (x == 1) {
            Log.d(TAG, "Right Indicator changed");
            pageIndicatorLeft.setImageResource(R.drawable.circle_indicator);
            pageIndicatorRight.setImageResource(R.drawable.circle_indicator_active);
            scaleView(pageIndicatorRight, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);
            scaleView(pageIndicatorLeft, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP, 1f);
        } else {
            Log.e(TAG, "Something went wrong setting the circle pageIndicator");
        }
    }

    public void setBurgerButtonImage(int x) {
        String item = null;
        switch (x) {
            case 0:
                item = "burger";
                break;
            case 1:
                item = "close";
                break;
            case 2:
                item = "back";
                break;
        }
        if (item != null && item.length() > 0)
            burgerButton.setBackgroundResource(burgerButtonMap.get(item));
    }

    public void activityButtonsEnable(boolean b) {
        Log.e(TAG, "TEST VISIBILITY: IN");
        Log.e(TAG, "TEST VISIBILITY: 0");
        if (!unlocked) {
            Log.e(TAG, "TEST VISIBILITY: 1");
            locker.setVisibility(b ? View.VISIBLE : View.GONE);
            locker.setClickable(b);
            lockerButtonLayout.setClickable(b);
        } else {
            Log.e(TAG, "TEST VISIBILITY: 2");
            locker.setVisibility(View.GONE);
            locker.setClickable(false);
            lockerButtonLayout.setClickable(false);
        }
        parallaxViewPager.setClickable(b);
        burgerButton.setVisibility(View.VISIBLE);
        burgerButton.setClickable(b);
        burgerButtonLayout.setVisibility(View.VISIBLE);
        burgerButtonLayout.setClickable(b);
    }

    public void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        v.startAnimation(anim);
    }

    private void makeDoubleBackToExitPressedOnce(int ms) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, ms);
    }

    public void resetMiddleView() {
        if (unlocked) {
            String tmp = CurrencyManager.getCurrentBalanceText();
            ((BreadWalletApp) getApplication()).setTopMidleView(BreadWalletApp.SETTINGS_TEXT, tmp);
        } else {
            ((BreadWalletApp) getApplication()).setTopMidleView(BreadWalletApp.BREAD_WALLET_IMAGE, "");
        }
    }

    private void startStopReceiver(boolean b) {
        if (b) {
            this.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        } else {
            this.unregisterReceiver(receiver);
        }
    }

    public void setUnlocked(boolean b) {
        unlocked = b;
        locker.setVisibility(b ? View.GONE : View.VISIBLE);
        lockerButtonLayout.setClickable(!b);
    }

    public void updateUI() {

    }

}
