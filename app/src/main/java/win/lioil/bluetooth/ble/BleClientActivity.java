package win.lioil.bluetooth.ble;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.LinearInterpolator;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.app.APP;
import win.lioil.bluetooth.util.AssistStatic;

/**
 （1）BLE设备地址是动态变化(每隔一段时间都会变化),而经典蓝牙设备是出厂就固定不变了。
 BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
 bluetoothLeScanner.startScan(mScanCallback); //开始扫描（扫描结果回调） --> ScanResult
 bluetoothLeScanner.stopScan(mScanCallback);//停止扫描

 （2）获取扫描设备，建立连接
 closeConn();
 BluetoothDevice dev = result.getDevice()
 mBluetoothGatt = dev.connectGatt(BleClientActivity.this, false, mBluetoothGattCallback); // 连接蓝牙设备

 // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133

 连接Callback
 BluetoothGattCallback{
     //连接状态变化
     //发现服务回调
     //特征读
     //特征写
     //特征改变
     //描述读
     //描述写
 }

 （3）获取Gatt服务，进行读写操作
 BluetoothGattService service = mBluetoothGatt.getService(uuid);

 Characteristic 读操作
 1.每次读写数据最多20个字节，如果超过，只能分包
 2.连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
 // 读取数据成功会回调->onCharacteristicChanged()
 BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可读的Characteristic
 mBluetoothGatt.readCharacteristic(characteristic);

 Characteristic 写操作
 String text = mWriteET.getText().toString();
 BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
 characteristic.setValue(text.getBytes()); //单次最多20个字节
 mBluetoothGatt.writeCharacteristic(characteristic);

 （4）设置通知,实时监听Characteristic变化
 // 设置Characteristic通知
 BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可通知的Characteristic
 mBluetoothGatt.setCharacteristicNotification(characteristic, true);

 // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
 BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleServerActivity.UUID_DESC_NOTITY);
 // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知类似,但服务端不主动发数据,只指示客户端读取数据
 descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
 mBluetoothGatt.writeDescriptor(descriptor);

 (1)扫描
 (2）建立连接，得到客户端mBluetoothGatt（连接回调）
 (3) 获取服务，开始读、写、通知
 **/

/**
 * BLE客户端(主机/中心设备/Central)
 * 蓝牙4.0 结构
 * BLE是基于GATT实现的，BLE分为三个部分Service服务、Characteristic特征、Descriptor描述符，每个部分都拥有不同的 UUID来标识。
 * 一个BLE设备可以拥有多个Service，一个Service可以包含多个Characteristic， 一个Characteristic包含一个Value和多个Descriptor，
 * 一个Descriptor包含一个Value。 通信数据一般存储在Characteristic内，目前一个Characteristic中存储的数据最大为20 byte。
 * 与Characteristic相关的权限字段主要有READ、WRITE、WRITE_NO_RESPONSE、NOTIFY。 Characteristic具有的权限属性可以有一个或者多个。
 * BLE4.0蓝牙发送数据，单次最大传输20个byte,如果是一般的协议命令，如：开关灯、前进左右等等，是不需要
 * 分包的，如果是需要发送如：图片、BIN文档、音乐等大数据量的文件，则必须进行分包发送，BLE库中已经提
 * 供了发送大数据包的接口。
 */
public class BleClientActivity extends Activity {
    private static final String TAG = BleClientActivity.class.getSimpleName();

    private BleDevAdapter mBleDevAdapter;//列表适配器
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private ObjectAnimator animator;//动画
    private FloatingActionButton floatingActionButton;//悬浮按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);
        RecyclerView rv = findViewById(R.id.rv_ble);
        floatingActionButton = findViewById(R.id.float_button);
        rv.setLayoutManager(new LinearLayoutManager(this));

        initBlueTooth();

        mBleDevAdapter = new BleDevAdapter(mBluetoothAdapter, new BleDevAdapter.Listener() {
            @Override
            public void onItemClick(BluetoothDevice dev) {
                Intent intent = new Intent(BleClientActivity.this, BleClientDetailActivity.class);
                intent.putExtra(BleClientDetailActivity.EXTRA_TAG, dev);
                startActivity(intent);
            }

            @Override
            public void onScanning() {
                AssistStatic.showToast(BleClientActivity.this, "扫描中");
                startBannerLoadingAnim();
            }

            @Override
            public void onScannSuccess() {
                AssistStatic.showToast(BleClientActivity.this, "扫描完成");
                stopBannerLoadingAnim();
            }
        });
        rv.setAdapter(mBleDevAdapter);
    }

    /**
     * 初始化蓝牙适配器
     */
    private void initBlueTooth() {
        // Android从4.3开始增加支持BLE技术（即蓝牙4.0及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //从系统服务中获取蓝牙管理器
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) {
                mBluetoothAdapter = manager.getAdapter();
            }
        } else {
            //获取系统默认的蓝牙适配器
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }

    public void reScan(View view) {
        if (mBleDevAdapter.isScanning) {
            APP.toast("正在扫描...", 0);
        } else {
            mBleDevAdapter.reScan();//扫描
        }
    }

    public void startBannerLoadingAnim() {//扫描中的动画
        floatingActionButton.setImageResource(R.drawable.ic_loading);
        animator = ObjectAnimator.ofFloat(floatingActionButton, "rotation", 0, 360);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(800);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    public void stopBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp);
        animator.cancel();
        floatingActionButton.setRotation(0);
    }
}