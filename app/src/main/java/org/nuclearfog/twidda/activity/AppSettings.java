package org.nuclearfog.twidda.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.nuclearfog.twidda.BuildConfig;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FontAdapter;
import org.nuclearfog.twidda.adapter.LocationAdapter;
import org.nuclearfog.twidda.backend.LocationLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.engine.TwitterEngine;
import org.nuclearfog.twidda.backend.items.TrendLocation;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.DialogBuilder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnDialogClick;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.DatabaseAdapter;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.util.List;
import java.util.regex.Matcher;

import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.nuclearfog.twidda.activity.MainActivity.RETURN_APP_LOGOUT;
import static org.nuclearfog.twidda.activity.MainActivity.RETURN_DB_CLEARED;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.DEL_DATABASE;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.LOGOUT_APP;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.WRONG_PROXY;

/**
 * Settings Activity class.
 *
 * @author nuclearfog
 */
public class AppSettings extends AppCompatActivity implements OnClickListener, OnDismissListener, OnSeekBarChangeListener,
        OnCheckedChangeListener, OnItemSelectedListener, OnDialogClick, OnColorChangedListener {

    private enum ColorMode {
        BACKGROUND,
        FONTCOLOR,
        HIGHLIGHT,
        POPUPCOLOR,
        ICONCOLOR,
        CARDCOLOR,
        NONE
    }

    private GlobalSettings settings;
    private LocationLoader locationAsync;
    private LocationAdapter locationAdapter;

    private Dialog connectDialog, databaseDialog, logoutDialog, color_dialog_selector;
    private View root, layout_key, layout_proxy, layout_auth_en, layout_auth;
    private EditText proxyAddr, proxyPort, proxyUser, proxyPass, api_key1, api_key2;
    private CompoundButton enableProxy, enableAuth, hqImage, enableAPI;
    private Spinner locationSpinner;
    private TextView list_size;
    private Button[] colorButtons;

    private ColorMode mode = ColorMode.NONE;
    private int color = 0;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_settings);
        Button delButton = findViewById(R.id.delete_db);
        Button logout = findViewById(R.id.logout);
        Toolbar toolbar = findViewById(R.id.toolbar_setting);
        View trend_card = findViewById(R.id.settings_trend_card);
        View user_card = findViewById(R.id.settings_data_card);
        CompoundButton toggleImg = findViewById(R.id.toggleImg);
        CompoundButton toggleAns = findViewById(R.id.toggleAns);
        Spinner fontSpinner = findViewById(R.id.spinner_font);
        SeekBar listSizeSelector = findViewById(R.id.settings_list_seek);
        enableProxy = findViewById(R.id.settings_enable_proxy);
        enableAuth = findViewById(R.id.settings_enable_auth);
        hqImage = findViewById(R.id.settings_image_hq);
        enableAPI = findViewById(R.id.settings_set_custom_keys);
        locationSpinner = findViewById(R.id.spinner_woeid);
        proxyAddr = findViewById(R.id.edit_proxy_address);
        proxyPort = findViewById(R.id.edit_proxy_port);
        proxyUser = findViewById(R.id.edit_proxyuser);
        proxyPass = findViewById(R.id.edit_proxypass);
        api_key1 = findViewById(R.id.settings_custom_key1);
        api_key2 = findViewById(R.id.settings_custom_key2);
        list_size = findViewById(R.id.settings_list_size);

        layout_proxy = findViewById(R.id.settings_layout_proxy);
        layout_auth_en = findViewById(R.id.settings_layout_auth_enable);
        layout_auth = findViewById(R.id.settings_layout_proxy_auth);
        layout_key = findViewById(R.id.settings_layout_key);

        root = findViewById(R.id.settings_layout);

        TypedArray buttons = getResources().obtainTypedArray(R.array.color_button);
        colorButtons = new Button[buttons.length()];
        for (int index = 0; index < buttons.length(); index++)
            colorButtons[index] = findViewById(buttons.getResourceId(index, 0));
        buttons.recycle();

        toolbar.setTitle(R.string.title_settings);
        setSupportActionBar(toolbar);

        settings = GlobalSettings.getInstance(this);
        if (!settings.getLogin()) {
            trend_card.setVisibility(GONE);
            user_card.setVisibility(GONE);
        }
        if (!settings.isProxyEnabled()) {
            layout_proxy.setVisibility(GONE);
            layout_auth_en.setVisibility(GONE);
            layout_auth.setVisibility(GONE);
        }
        if (!settings.isCustomApiSet()) {
            layout_key.setVisibility(GONE);
        }
        locationAdapter = new LocationAdapter(settings);
        locationAdapter.addTop(settings.getTrendLocation());
        locationSpinner.setAdapter(locationAdapter);
        FontAdapter fontAdapter = new FontAdapter(settings);
        fontSpinner.setAdapter(fontAdapter);
        fontSpinner.setSelection(settings.getFont());

        AppStyles.setTheme(settings, root);

        setButtonColors();
        toggleImg.setChecked(settings.getImageLoad());
        toggleAns.setChecked(settings.getAnswerLoad());
        enableAPI.setChecked(settings.isCustomApiSet());
        proxyAddr.setText(settings.getProxyHost());
        proxyPort.setText(settings.getProxyPort());
        proxyUser.setText(settings.getProxyUser());
        proxyPass.setText(settings.getProxyPass());
        api_key1.setText(settings.getConsumerKey());
        api_key2.setText(settings.getConsumerSecret());
        list_size.setText(Integer.toString(settings.getListSize()));
        listSizeSelector.setProgress(settings.getListSize() / 10 - 1);
        enableProxy.setChecked(settings.isProxyEnabled());
        enableAuth.setChecked(settings.isProxyAuthSet());
        hqImage.setEnabled(settings.getImageLoad());
        hqImage.setChecked(settings.getImageQuality());

        connectDialog = DialogBuilder.create(this, WRONG_PROXY, this);
        databaseDialog = DialogBuilder.create(this, DEL_DATABASE, this);
        logoutDialog = DialogBuilder.create(this, LOGOUT_APP, this);

        for (Button btn : colorButtons)
            btn.setOnClickListener(this);
        logout.setOnClickListener(this);
        delButton.setOnClickListener(this);
        toggleImg.setOnCheckedChangeListener(this);
        toggleAns.setOnCheckedChangeListener(this);
        enableAPI.setOnCheckedChangeListener(this);
        enableProxy.setOnCheckedChangeListener(this);
        enableAuth.setOnCheckedChangeListener(this);
        hqImage.setOnCheckedChangeListener(this);
        fontSpinner.setOnItemSelectedListener(this);
        locationSpinner.setOnItemSelectedListener(this);
        listSizeSelector.setOnSeekBarChangeListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (settings.getLogin() && locationAsync == null) {
            locationAsync = new LocationLoader(this);
            locationAsync.execute();
        }
    }


    @Override
    public void onBackPressed() {
        if (saveConnectionSettings()) {
            TwitterEngine.resetTwitter();
            super.onBackPressed();
        } else {
            if (!connectDialog.isShowing()) {
                connectDialog.show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (locationAsync != null && locationAsync.getStatus() == RUNNING)
            locationAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.settings, m);
        AppStyles.setMenuIconColor(m, settings.getIconColor());
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings_info) {
            Dialog dialog = new Dialog(this, R.style.AppInfoDialog);
            dialog.setContentView(R.layout.dialog_app_info);
            String versionName = " V" + BuildConfig.VERSION_NAME;
            TextView appInfo = dialog.findViewById(R.id.settings_app_info);
            appInfo.setLinkTextColor(settings.getHighlightColor());
            appInfo.append(versionName);
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onConfirm(DialogBuilder.DialogType type) {
        switch (type) {
            case LOGOUT_APP:
                settings.logout();
                TwitterEngine.resetTwitter();
                DatabaseAdapter.deleteDatabase(getApplicationContext());
                setResult(RETURN_APP_LOGOUT);
                finish();
                break;

            case DEL_DATABASE:
                DatabaseAdapter.deleteDatabase(getApplicationContext());
                setResult(RETURN_DB_CLEARED);
                break;

            case WRONG_PROXY:
                // exit without saving proxy settings
                finish();
                break;
        }
    }


    @Override
    public void onClick(View v) {
        // delete database
        if (v.getId() == R.id.delete_db) {
            if (!databaseDialog.isShowing()) {
                databaseDialog.show();
            }
        }
        // logout from twitter
        else if (v.getId() == R.id.logout) {
            if (!logoutDialog.isShowing()) {
                logoutDialog.show();
            }
        }
        // set background color
        else if (v == colorButtons[0]) {
            mode = ColorMode.BACKGROUND;
            color = settings.getBackgroundColor();
            setColor(color, false);
        }
        // set font color
        else if (v == colorButtons[1]) {
            mode = ColorMode.FONTCOLOR;
            color = settings.getFontColor();
            setColor(color, false);
        }
        // set popup color
        else if (v == colorButtons[2]) {
            mode = ColorMode.POPUPCOLOR;
            color = settings.getPopupColor();
            setColor(color, false);
        }
        // set highlight color
        else if (v == colorButtons[3]) {
            mode = ColorMode.HIGHLIGHT;
            color = settings.getHighlightColor();
            setColor(color, false);
        }
        // set card color
        else if (v == colorButtons[4]) {
            mode = ColorMode.CARDCOLOR;
            color = settings.getCardColor();
            setColor(color, true);
        }
        // set icon color
        else if (v == colorButtons[5]) {
            mode = ColorMode.ICONCOLOR;
            color = settings.getIconColor();
            setColor(color, false);
        }
    }


    @Override
    public void onDismiss(DialogInterface d) {
        if (d == color_dialog_selector) {
            switch (mode) {
                case BACKGROUND:
                    settings.setBackgroundColor(color);
                    break;

                case FONTCOLOR:
                    settings.setFontColor(color);
                    break;

                case POPUPCOLOR:
                    settings.setPopupColor(color);
                    break;

                case HIGHLIGHT:
                    settings.setHighlightColor(color);
                    break;

                case CARDCOLOR:
                    settings.setCardColor(color);
                    break;

                case ICONCOLOR:
                    settings.setIconColor(color);
                    invalidateOptionsMenu();
                    break;
            }
            AppStyles.setTheme(settings, root);
            setButtonColors();
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton c, boolean checked) {
        int viewId = c.getId();
        // toggle image loading
        if (viewId == R.id.toggleImg) {
            settings.setImageLoad(checked);
            hqImage.setEnabled(checked);
        }
        // toggle automatic answer load
        else if (viewId == R.id.toggleAns) {
            settings.setAnswerLoad(checked);
        }
        // enable proxy settings
        else if (viewId == R.id.settings_enable_proxy) {
            if (checked) {
                layout_proxy.setVisibility(VISIBLE);
                layout_auth_en.setVisibility(VISIBLE);
            } else {
                layout_proxy.setVisibility(GONE);
                layout_auth_en.setVisibility(GONE);
                enableAuth.setChecked(false);
            }
        }
        //enable proxy authentication
        else if (viewId == R.id.settings_enable_auth) {
            if (checked) {
                layout_auth.setVisibility(VISIBLE);
            } else {
                layout_auth.setVisibility(GONE);
            }
        }
        // enable high quality images
        else if (viewId == R.id.settings_image_hq) {
            settings.setHighQualityImage(checked);
        }
        // enable custom API setup
        else if (viewId == R.id.settings_set_custom_keys) {
            if (checked) {
                layout_key.setVisibility(VISIBLE);
            } else {
                layout_key.setVisibility(GONE);
            }
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getAdapter() instanceof LocationAdapter) {
            settings.setTrendLocation(locationAdapter.getItem(position));
        } else if (parent.getAdapter() instanceof FontAdapter) {
            settings.setFont(position);
            AppStyles.setViewFont(settings, root);
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    @Override
    public void onColorChanged(int i) {
        color = i;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String text = Integer.toString((progress + 1) * 10);
        list_size.setText(text);
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        settings.setListSize((seekBar.getProgress() + 1) * 10);
    }

    /**
     * set location information from twitter
     *
     * @param data location data
     */
    public void setLocationData(List<TrendLocation> data) {
        locationAdapter.setData(data);
        int position = locationAdapter.getPosition(settings.getTrendLocation());
        if (position > 0) {
            locationSpinner.setSelection(position);
        }
    }

    /**
     * called when an error occurs
     *
     * @param err exception from twitter
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(this, err);
    }

    /**
     * show color picker dialog with preselected color
     *
     * @param preColor    preselected color
     * @param enableAlpha true to enable alpha slider
     */
    private void setColor(int preColor, boolean enableAlpha) {
        if (color_dialog_selector == null || !color_dialog_selector.isShowing()) {
            color_dialog_selector = ColorPickerDialogBuilder.with(this)
                    .showAlphaSlider(enableAlpha).initialColor(preColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                    .setOnColorChangedListener(this).density(15).build();
            color_dialog_selector.setOnDismissListener(this);
            color_dialog_selector.show();
        }
    }

    /**
     * setup all color buttons color
     */
    private void setButtonColors() {
        int[] colors = settings.getAllColors();
        for (int i = 0; i < colorButtons.length; i++) {
            // set button color
            colorButtons[i].setBackgroundColor(colors[i]);
            // invert font & border color
            int invertedColor = (colors[i] | Color.BLACK) ^ 0xffffff;
            colorButtons[i].setTextColor(invertedColor);
            View border = (View) colorButtons[i].getParent();
            border.setBackgroundColor(invertedColor);
        }
    }

    /**
     * check proxy settings and save them if they are correct
     *
     * @return true if settings are saved successfully
     */
    private boolean saveConnectionSettings() {
        boolean checkPassed = true;
        if (enableProxy.isChecked()) {
            checkPassed = proxyAddr.length() > 0 && proxyPort.length() > 0;
            if (checkPassed) {
                Matcher ipMatch = Patterns.IP_ADDRESS.matcher(proxyAddr.getText());
                checkPassed = ipMatch.matches();
            }
            if (checkPassed) {
                int port = 0;
                String portStr = proxyPort.getText().toString();
                if (!portStr.isEmpty()) {
                    port = Integer.parseInt(portStr);
                }
                checkPassed = port > 0 && port < 65536;
            }
            if (enableAuth.isChecked() && checkPassed) {
                checkPassed = proxyUser.length() > 0 && proxyPass.length() > 0;
            }
            if (checkPassed) {
                String proxyAddrStr = proxyAddr.getText().toString();
                String proxyPortStr = proxyPort.getText().toString();
                String proxyUserStr = proxyUser.getText().toString();
                String proxyPassStr = proxyPass.getText().toString();
                settings.setProxyServer(proxyAddrStr, proxyPortStr, proxyUserStr, proxyPassStr);
                settings.setProxyEnabled(true);
                settings.setProxyAuthSet(enableAuth.isChecked());
            }
        } else {
            settings.clearProxyServer();
        }
        if (enableAPI.isChecked()) {
            if (api_key1.length() > 0 && api_key2.length() > 0) {
                String key1 = api_key1.getText().toString();
                String key2 = api_key2.getText().toString();
                settings.setCustomAPI(key1, key2);
            } else {
                checkPassed = false;
            }
        } else {
            settings.removeCustomAPI();
        }
        return checkPassed;
    }
}