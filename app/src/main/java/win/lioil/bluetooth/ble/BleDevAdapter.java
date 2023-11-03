package win.lioil.bluetooth.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.bean.BleDev;
import win.lioil.bluetooth.util.BluetoothUtil;

/**
 * todo 注意: BLE设备地址是动态变化(每隔一段时间都会变化),而经典蓝牙设备是出厂就固定不变了！
 * 什么导致改变导致？
 */
public class BleDevAdapter extends RecyclerView.Adapter<BleDevAdapter.VH> {
    private static final String TAG = BleDevAdapter.class.getSimpleName();
    private final Listener mListener;
    private final Handler mHandler = new Handler();
    private final List<BleDev> mDevices = new ArrayList<>();
    public boolean isScanning;

    private BluetoothAdapter mBluetoothAdapter;

    //扫描回调 api>21
    private final ScanCallback mScanCallback = new ScanCallback() {// 扫描Callback
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mDevices.size() > 0) {
                for (int i = 0; i < mDevices.size(); i++) {
                    BleDev rssiDevice = mDevices.get(i);
                    if (TextUtils.equals(rssiDevice.getDev().getAddress(), result.getDevice().getAddress())) {
                        if (rssiDevice.getRssi() != result.getRssi() && System.currentTimeMillis() - rssiDevice.getRssiUpdateTime() > 1000L) {
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(result.getRssi());
                            notifyItemChanged(i);
                        }
                        return;
                    }
                }
            }

            result.getScanRecord();//获取BLE广播数据
            result.getDevice().getName();//设备信息

            BleDev bleDev = new BleDev();
            bleDev.setDev(result.getDevice());
            bleDev.setRssi(result.getRssi());
            bleDev.setRssiUpdateTime(SystemClock.currentThreadTimeMillis());
            bleDev.setScanResult(result);
            if (!mDevices.contains(bleDev)) {
                mDevices.add(bleDev);
                notifyDataSetChanged();
                Log.i(TAG, "onScanResult: " + result); // result.getScanRecord() 获取BLE广播数据
                result.getScanRecord();
            }
        }
    };

    //扫描回调
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mDevices.size() > 0) {
                for (int i = 0; i < mDevices.size(); i++) {
                    BleDev rssiDevice = mDevices.get(i);
                    if (TextUtils.equals(rssiDevice.getDev().getAddress(), device.getAddress())) {
                        if (rssiDevice.getRssi() != rssi && System.currentTimeMillis() - rssiDevice.getRssiUpdateTime() > 1000L) {
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(rssi);
                            notifyItemChanged(i);
                        }
                        return;
                    }
                }
            }
            device.getName();//名称
            device.getAddress();//硬件地址
            device.getType();//设备类型
            device.getBondState();//绑定状态

            BleDev bleDev = new BleDev();
            bleDev.setDev(device);
            bleDev.setRssi(rssi);
            bleDev.setRssiUpdateTime(SystemClock.currentThreadTimeMillis());
            bleDev.setScanResult(null);
            if (!mDevices.contains(device)) {
                mDevices.add(bleDev);
                //在Android 5.0以下 可以通过rssi获取蓝牙设备距离手机的距离
//                UuidUtil.getDistance(rssi);
                notifyDataSetChanged();
                Log.e("mcy", "扫描到设备-->" + device.getName());
            }
        }
    };

    public BleDevAdapter(BluetoothAdapter bluetoothAdapter, Listener listener) {
        mBluetoothAdapter = bluetoothAdapter;
        mListener = listener;
        scanBle();//一创建就开始扫描
    }

    // 重新扫描
    public void reScan() {
        mDevices.clear();
        notifyDataSetChanged();
        scanBle();
    }

    // 扫描BLE蓝牙(不会扫描经典蓝牙)
    private void scanBle() {
        isScanning = true;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {//21
            if (mListener != null) {
                mListener.onScanning();
            }
            final ScanSettings scanSettings = new ScanSettings.Builder()//扫描设置
                    //Android8.0以上退到后台息屏后，为了保证省电等原因，如果不设置ScanFilters的话是默认扫不到设备的，
                    //退居到后台设置扫描模式为低功耗
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
            final List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder()//过滤器
                    //过滤扫描蓝牙设备的主服务，只扫描包含当前服务的设备
                    //.setServiceUuid(ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb"))
                    .build());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeScanner.startScan(filters,scanSettings,mScanCallback);//开始扫描
                    // Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，
                    // 而新API帮我们解析成ScanRecord类
                    bluetoothLeScanner.startScan(mScanCallback);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothLeScanner.stopScan(mScanCallback); //停止扫描
                            isScanning = false;
                            if (mListener != null) {
                                mListener.onScannSuccess();
                            }
                        }
                    }, 60000);
                }
            }, 0);
        } else {
            if (mListener != null) {
                mListener.onScanning();
            }
            mBluetoothAdapter.startLeScan(leScanCallback); //开始扫描
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(leScanCallback);//停止扫描
                    isScanning = false;
                    if (mListener != null) {
                        mListener.onScannSuccess();
                    }
                }
            }, 10000);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dev, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, int position) {
        BleDev dev = mDevices.get(position);
        String name = dev.getDev().getName();
        String address = dev.getDev().getAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("设备名称：");
        sb.append(name);
        sb.append("\n设备地址：");
        sb.append(address);
        sb.append("\n信号：");
        sb.append(dev.getRssi());
        sb.append("\n距离：");
        sb.append(Math.round(BluetoothUtil.getDistance(dev.getRssi())));
        sb.append(" m");
        holder.name.setText(sb.toString());
        if (dev.getScanResult() != null) {
            holder.address.setText(String.format("广播数据{%s}", dev.getScanResult().getScanRecord()));
        }
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;
        final TextView address;

        VH(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            Log.d(TAG, "onClick, getAdapterPosition=" + pos);
            if (pos >= 0 && pos < mDevices.size())
                mListener.onItemClick(mDevices.get(pos).getDev());
        }
    }

    public interface Listener {
        void onItemClick(BluetoothDevice dev);

        void onScanning();

        void onScannSuccess();
    }

}
