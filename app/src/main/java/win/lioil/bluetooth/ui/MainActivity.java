package win.lioil.bluetooth.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.app.APP;
import win.lioil.bluetooth.ble.BleClientActivity;
import win.lioil.bluetooth.ble.BleServerActivity;
import win.lioil.bluetooth.bt.BtClientActivity;
import win.lioil.bluetooth.bt.BtServerActivity;

public class MainActivity extends BaseActivity {

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initViews() {

    }

    @Override
    public void initData() {
        // 检查蓝牙开关
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            APP.toast("本机没有找到蓝牙硬件或驱动！", 0);
            finish();
            return;
        } else {
            if (!adapter.isEnabled()) {
                //直接开启蓝牙
                //adapter.enable();

                //或者
                turnOnBlueTooth(this, 100);
            }
        }

        // 检查是否支持BLE蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            APP.toast("本机不支持低功耗蓝牙！", 0);
            finish();
            return;
        }
    }

    public void turnOnBlueTooth(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
        //对应的,在onActivityResult()方法可以知道是否成功打开蓝牙
    }

    public void btClient(View view) {
        startActivity(new Intent(this, BtClientActivity.class));
    }

    public void btServer(View view) {
        startActivity(new Intent(this, BtServerActivity.class));
    }

    public void bleClient(View view) {
        startActivity(new Intent(this, BleClientActivity.class));
    }

    public void bleServer(View view) {
        startActivity(new Intent(this, BleServerActivity.class));
    }
}
