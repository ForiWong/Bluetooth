package win.lioil.bluetooth.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.UUID;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.app.APP;
import win.lioil.bluetooth.util.AssistStatic;

/**
 （1）manifest.xml权限与配置：设置android.hardware.bluetooth_le = true。
 （2）检查是否支持蓝牙、是否开启蓝牙、是否支持BLE
 // 检查蓝牙开关
 BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
 if (adapter == null) // 本机没有找到蓝牙硬件或驱动

 if (!adapter.isEnabled()) //直接开启蓝牙

 if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) //本机不支持低功耗蓝牙！

 （3）BLE服务端(也叫从机/外围设备/peripheral)

 UUID
 Advertiser BLE广播
 BluetoothGattServer BLE服务端

 BluetoothManager //蓝牙管理
 BluetoothAdapter //蓝牙适配器

 BLE广播Callback AdvertiseCallback{
 //广播开启成功
 //广播开启失败
 }

 BLE服务端Callback BluetoothGattServerCallback {
    //连接状态改变回调
    //添加服务回调
 //特征读请求回调 //响应客户端
 //特征写请求回调 //收到数据
 //Descriptor 属性读请求
 //Descriptor 属性写请求
 //执行写
 //发送通知
 //MTU改变
 }

 mBluetoothGattServer.sendResponse()//响应客户端

 BluetoothAdvertiser：服务器广播，用于开启服务器广播以供Client端扫描发现。
 AdvertiseSetting：这个类的创建需要用到Builder，其作用是设置Advertiser属性的，例如广播的Mode，连接时
 间，连接可用等等。
 AdvertiseData：这个类的作用就是配置广播的数据的，例如设置服务的UUID，传输的等级TxPowerLevel，还有设
 置设备名是否包含在传输数据里面。

 //广播设置
 AdvertiseSettings settings = new AdvertiseSettings.Builder()
 .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
 .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
 .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
 .setTimeout(3000)//设置超时显示，值为0将禁用时间限制
 .build();

 //广播数据(必须，广播启动就会发送)
 AdvertiseData advertiseData = new AdvertiseData.Builder()
 .setIncludeDeviceName(true) //包含蓝牙名称
 .setIncludeTxPowerLevel(true) //包含发射功率级别
 .addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
 .build();

 //扫描响应数据(可选，当客户端扫描时才发送)
 AdvertiseData scanResponse = new AdvertiseData.Builder()
 .addManufacturerData(2, new byte[]{66, 66}) //设备厂商数据，自定义
 .addServiceUuid(new ParcelUuid(UUID_SERVICE)) //服务UUID
 .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2}) //服务数据，自定义
 .build();

 //开始广播
 mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
 mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);

 BluetoothGattService //主服务
 服务：主服务《特征可读、通知 < 属性>》
 addCharacteristic

 BluetoothManager 开启服务端 openGattServer --> mBluetoothGattServer

 mBluetoothGattServer 添加服务 addService()

 总结流程：
 （1）清单文件配置权限、硬件开关；
 （2）蓝牙管理器、蓝牙适配器
 （3）检查是否支持蓝牙、是否开启蓝牙、是否支持BLE
 （4）蓝牙适配器，获取广播器；开始广播，参数（广播设置、广播数据、响应数据、广播回调）；
 （5）准备服务对象：服务里有添加特征，特征里边添加属性；
 （5）蓝牙管理器开启服务端gattServer；
 （6）服务端里边添加准备的服务对象，添加成功即可使用了。
 **/

/**
 * 手机作为服务端，连接的设备作为客户端
 * BLE服务端(从机/外围设备/peripheral)
 * https://juejin.im/post/5cdbd083e51d453ce606dbd0
 * 在蓝牙开发中，有些情况是不需要连接的，只要外设广播自己的数据即可，例如苹果的ibeacon。
 * 自Android 5.0更新蓝牙API后，手机可以作为外设广播数据。
 * 广播包有两种：
 *  广播包（Advertising Data）
 *  响应包（Scan Response）
 * 其中广播包是每个外设都必须广播的，而响应包是可选的。每个广播包的长度必须是31个字节，如果不到31个字节 ，则剩下的全用0填充 补全，这部分的数据是无效的
 */
public class BleServerActivity extends Activity {
    public static final UUID UUID_SERVICE = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"); //服务UUID

    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");//read
    public static final UUID UUID_DESC_NOTITY =      UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");//notify
    public static final UUID UUID_CHAR_WRITE =       UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");//write

    private static final String TAG = BleServerActivity.class.getSimpleName();
    private TextView mTips;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser; // BLE广播
    private BluetoothGattServer mBluetoothGattServer; // BLE服务端

    // BLE广播Callback mBluetoothLeAdvertiser.startAdvertising 开启广播的回调
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            logTv("BLE广播开启成功");
        }

        @Override
        public void onStartFailure(int errorCode) {
            logTv("BLE广播开启失败,错误码:" + errorCode);
        }
    };

    private String getSendStr(){
        String str = ((EditText)findViewById(R.id.et_send)).getText().toString();
        return str.isEmpty() ? "abc" : str;
    }

    // BLE服务端Callback 定义Gat回调，当中心设备连接该手机外设后，中心设备修改特征值，手机端读取特征值等情况，
    // 会得到相应的特征值回调
    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        //连接状态回调
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", device.getName(), device.getAddress(), status, newState));
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), device));
        }

        //添加服务回调 mBluetoothGattServer.addService(service);
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.i(TAG, String.format("onServiceAdded:%s,%s", status, service.getUuid()));
            logTv(String.format(status == 0 ? "添加服务[%s]成功" : "添加服务[%s]失败,错误码:" + status, service.getUuid()));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
//            String response = "CHAR_" + (int) (Math.random() * 100); //模拟数据
            String response = getSendStr();
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// 响应客户端 发送响应
            logTv("客户端读取Characteristic[" + characteristic.getUuid() + "]:\n" + response);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            // 获取客户端发过来的数据
            String requestStr = new String(requestBytes);
            Log.i(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, requestStr));
            String response = requestStr + ":" + getSendStr();
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// 响应客户端
            logTv("客户端写入Characteristic[" + characteristic.getUuid() + "]:\n" + response);
        }

        // 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.i(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
//            String response = "DESC_" + (int) (Math.random() * 100); //模拟数据
            String response = getSendStr();
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes()); // 响应客户端
            logTv("客户端读取Descriptor[" + descriptor.getUuid() + "]:\n" + response);
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // 获取客户端发过来的数据
            String valueStr = Arrays.toString(value);
            Log.i(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
                    preparedWrite, responseNeeded, offset, valueStr));
            String response = getSendStr();
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// 响应客户端
            logTv("客户端写入Descriptor[" + descriptor.getUuid() + "]:\n" + valueStr + response);

            // 简单模拟通知客户端Characteristic变化
            if (Arrays.toString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE).equals(valueStr)) { //是否开启通知
                final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //通知5次
                        for (int i = 0; i < 5; i++) {
                            SystemClock.sleep(3000);
//                            String response = "CHAR_" + (int) (Math.random() * 100); //模拟数据
                            String response = getSendStr();
                            characteristic.setValue(response);
                            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                            logTv("通知客户端改变Characteristic[" + characteristic.getUuid() + "]:\n" + response);
                        }
                    }
                }).start();
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.i(TAG, String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.i(TAG, String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Log.i(TAG, String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleserver);
        mTips = findViewById(R.id.tv_tips);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName("bleName");//设置设备蓝牙名称

        // ============启动BLE蓝牙广播(广告) =================================================================================
        //广播设置(必须)
        /**
        通过AdvertiseSettings.Builder#setAdvertiseMode() 设置广播模式。其中有3种模式：

        在均衡电源模式下执行蓝牙LE广播:AdvertiseSettings#ADVERTISE_MODE_BALANCED
        在低延迟，高功率模式下执行蓝牙LE广播: AdvertiseSettings#ADVERTISE_MODE_LOW_LATENCY
        在低功耗模式下执行蓝牙LE广播:AdvertiseSettings#ADVERTISE_MODE_LOW_POWER

        （2）、通过AdvertiseSettings.Builder#setAdvertiseMode() 设置广播发射功率。共有4种功率模式：

        使用高TX功率级别进行广播：AdvertiseSettings#ADVERTISE_TX_POWER_HIGH
        使用低TX功率级别进行广播：AdvertiseSettings#ADVERTISE_TX_POWER_LOW
        使用中等TX功率级别进行广播：AdvertiseSettings#ADVERTISE_TX_POWER_MEDIUM
        使用最低传输（TX）功率级别进行广播：AdvertiseSettings#ADVERTISE_TX_POWER_ULTRA_LOW

        （3）、通过AdvertiseSettings.Builder#setTimeout()设置持续广播的时间，单位为毫秒。最多180000毫秒。
         当值为0则无时间限制，持续广播，除非调用BluetoothLeAdvertiser#stopAdvertising()停止广播。
        （4）、通过AdvertiseSettings.Builder#setConnectable()设置该广播是否可以连接的。
         **/
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
                .setTimeout(100000)//设置超时显示，值为0将禁用时间限制
                //设置持续广播的时间，单位为毫秒。最多180000毫秒。当值为0则无时间限制，持续广播，除非调
                .build();
        //广播数据(必须，广播启动就会发送)
        /**
        （1）、AdvertiseData.Builder#setIncludeDeviceName()方法，可以设置广播包中是否包含蓝牙的名称。
        （2）、AdvertiseData.Builder#setIncludeTxPowerLevel()方法，可以设置广播包中是否包含蓝牙的发射功率。
        （3）、AdvertiseData.Builder#addServiceUUID(ParcelUUID)方法，可以设置特定的UUID在广播包中。
        （4）、AdvertiseData.Builder#addServiceData(ParcelUUID，byte[])方法，可以设置特定的UUID和其数据在广播包中。
        （5）、AdvertiseData.Builder#addManufacturerData(int，byte[])方法，可以设置特定厂商Id和其数据在广播包中。
               从AdvertiseData.Builder的设置中可以看出，如果一个外设需要在不连接的情况下对外广播数据，其数据可以存储在UUID对应的数据中，
               也可以存储在厂商数据中。但由于厂商ID是需要由Bluetooth SIG进行分配的，厂商间一般都将数据设置在厂商数据。
         **/
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true) //包含蓝牙名称
                .setIncludeTxPowerLevel(true) //包含发射功率级别
                .addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
                .build();
        //扫描响应数据(可选，当客户端扫描时才发送)
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                //隐藏广播设备名称
                .setIncludeDeviceName(false)
                //隐藏发射功率级别
                .setIncludeDeviceName(false)
                .addManufacturerData(2, new byte[]{66, 66}) //设备厂商数据，自定义
                .addServiceUuid(new ParcelUuid(UUID_SERVICE)) //服务UUID  //可以添加多个服务吗
                .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2}) //服务数据，自定义
                .build();

        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothAdapter.isEnabled()) {
            if (mBluetoothLeAdvertiser != null) {
                //开启广播
                mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);
            } else {
                Log.d("yif", "该手机不支持ble广播");
            }
        } else {
            Log.d("yif", "手机蓝牙未开启");
        }

        // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
        // =============启动BLE蓝牙服务端=====================================================================================
        //SERVICE_TYPE_PRIMARY 主服务 SERVICE_TYPE_SECONDARY 次服务（存在主服务中）
        //可以添加多个服务吗？
        BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //创建初始化特征值例子
//        BluetoothGattCharacteristic mGattCharacteristic = new BluetoothGattCharacteristic(UUID_CHAR_READ_NOTIFY,
//                BluetoothGattCharacteristic.PROPERTY_WRITE|
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY|
//                        BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_WRITE|
//                        BluetoothGattCharacteristic.PERMISSION_READ);

        //添加可读+通知 characteristic，通过characteristic进行读写操作来通信 特征值支持写，支持读，支持通知
        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(UUID_CHAR_READ_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        characteristicRead.addDescriptor(new BluetoothGattDescriptor(UUID_DESC_NOTITY, BluetoothGattCharacteristic.PERMISSION_WRITE));
        service.addCharacteristic(characteristicRead);//添加特征

        //添加可写characteristic
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);

        if (bluetoothManager != null)
            mBluetoothGattServer = bluetoothManager.openGattServer(this, mBluetoothGattServerCallback);

        boolean result = mBluetoothGattServer.addService(service);
        if (result) {
            AssistStatic.showToast(BleServerActivity.this, "添加服务成功");
        } else {
            AssistStatic.showToast(BleServerActivity.this, "添加服务失败");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeAdvertiser != null)
            //停止广播
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        if (mBluetoothGattServer != null)
            //关闭服务
            mBluetoothGattServer.close();
    }

    private void logTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
                mTips.append(msg + "\n\n");
            }
        });
    }
}