package win.lioil.bluetooth.util;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

public class BluetoothUtil {
    private final static String TAG = "BluetoothUtil";

    private static final double A_Value = 60; // A - 发射端和接收端相隔1米时的信号强度
    private static final double n_Value = 2.0; //  n - 环境衰减因子

    public static double getDistance(int rssi) { //根据Rssi获得返回的距离,返回数据单位为m
        int iRssi = Math.abs(rssi);
        double power = (iRssi - A_Value) / (10 * n_Value);
        return Math.pow(10, power);
    }

    // 获取蓝牙的开关状态
    public static boolean getBlueToothStatus(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean enabled;
        switch (bluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
                enabled = true;
                break;
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
            default:
                enabled = false;
                break;
        }
        return enabled;
    }

    // 打开或关闭蓝牙
    public static void setBlueToothStatus(Context context, boolean enabled) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (enabled) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.disable();
        }
    }

    // 建立蓝牙配对
    public static boolean createBond(BluetoothDevice device) {
        //判断设备是否配对，没有配对再配，配对了就不需要配了
        //if (device.getBondState() == BluetoothDevice.BOND_NONE)
        try {
            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
            Log.d(TAG, "开始配对");
            Boolean result = (Boolean) createBondMethod.invoke(device);
            Log.d(TAG, "配对结果="+result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //自动配对设置Pin值
    static public boolean autoBond(Class btClass,BluetoothDevice device,String strPin) throws Exception {
        Method autoBondMethod = btClass.getMethod("setPin",new Class[]{byte[].class});
        Boolean result = (Boolean)autoBondMethod.invoke(device,new Object[]{strPin.getBytes()});
        return result;
    }

    // 取消蓝牙配对
    public static boolean removeBond(BluetoothDevice device) {
        try {
            Method createBondMethod = BluetoothDevice.class.getMethod("removeBond");
            Log.d(TAG, "取消配对");
            Boolean result = (Boolean) createBondMethod.invoke(device);
            Log.d(TAG, "取消结果="+result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 建立A2DP连接
    public static boolean connectA2dp(BluetoothA2dp a2dp, BluetoothDevice device) {
        try {
            Method setMethod = BluetoothA2dp.class.getMethod("setPriority", BluetoothDevice.class, int.class);
            Boolean setResult = (Boolean) setMethod.invoke(a2dp, device, 100);
            Log.d(TAG, "设置优先级结果="+setResult);
            Method connectMethod = BluetoothA2dp.class.getMethod("connect", BluetoothDevice.class);
            Boolean connectResult = (Boolean) connectMethod.invoke(a2dp, device);
            Log.d(TAG, "A2DP连接结果="+connectResult);
            return connectResult;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 取消A2DP连接
    public static boolean disconnectA2dp(BluetoothA2dp a2dp, BluetoothDevice device) {
        try {
            Method method = BluetoothA2dp.class.getMethod("disconnect", BluetoothDevice.class);
            Boolean result = (Boolean) method.invoke(a2dp, device);
            Log.d(TAG, "A2DP取消结果="+result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
