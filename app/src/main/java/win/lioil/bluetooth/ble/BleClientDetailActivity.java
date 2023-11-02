package win.lioil.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import win.lioil.bluetooth.R;
import win.lioil.bluetooth.app.APP;
import win.lioil.bluetooth.util.ByteUtil;
import win.lioil.bluetooth.util.HexUtils;
import win.lioil.bluetooth.util.UuidUtil;

public class BleClientDetailActivity extends AppCompatActivity {
    private static final String TAG = "BleClientDetailActivity";
    private EditText mWriteET;
    private TextView mTips;
    //BluetoothGatt作为中央来使用和处理数据；BluetoothGattCallback返回中央的状态和周边提供的数据。
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private Button btnWrite;
    private Button btnRead;
    private Button btnNotify;
    private HashMap<String, Map<String, BluetoothGattCharacteristic>> servicesMap = new HashMap<>();

    private BluetoothGattCharacteristic mNotifyCharacteristic1;//特征

    //根据具体硬件进行设置
//    public static String DEVICEA_UUID_SERVICE = "000001801-0000-1000-8000-00805f9b34fb";
//    public static String DEVICEA_UUID_CHARACTERISTIC = "00002a05-0000-1000-8000-00805f9b34fb";
//    //一般不用修改
//    public static String DEVICEA_UUID_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    public Queue<byte[]> dataInfoQueue = new LinkedList<>();
    private StringBuilder mBuilder;
    private final Object locker = new Object();
    public static final String EXTRA_TAG = "device";
    private TextView tvProperties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_client_detail);
        initViews();
        Intent intent = getIntent();
        if (intent.getParcelableExtra(EXTRA_TAG) != null) {
            BluetoothDevice dev = intent.getParcelableExtra(EXTRA_TAG);
            //连接connectGatt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//23
                //autoContent 是直接连接到远程设备（false）还是直接连接到 远程设备一可用就自动连接（true） BluetoothDevice设置蓝牙传输层模式，报133错误，
                //也可能传输层问题，设置不同的传输层模式解决
                mBluetoothGatt = dev.connectGatt(this, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = dev.connectGatt(this, false, mBluetoothGattCallback);
            }
            logTv(String.format("与[%s]开始连接............", dev));
        }
    }
    /**
     * 如何避免ble蓝牙连接出现133错误？
     *
     * Android 连接外围设备的数量有限，当不需要连接蓝牙设备的时候，必须调用 BluetoothGatt#close 方法释放资源；
     * 蓝牙 API 连接蓝牙设备的超时时间大概在 20s 左右，具体时间看系统实现。有时候某些设备进行蓝牙连接的时间会很长，大概十多秒。
     * 如果自己手动设置了连接超时时间在某些设备上可能会导致接下来几次的连接尝试都会在 BluetoothGattCallback#onConnectionStateChange
     * 返回 state == 133；
     *
     * 能否避免android设备与ble设备连接/断开时上报的133这类错误?
     * 1、在连接失败或者断开连接之后，调用 close 并刷新缓存
     * 2、尽量不要在startLeScan的时候尝试连接，先stopLeScan后再去连
     * 3、对同一设备断开后再次连接(连接失败重连)，哪怕调用完close，需要等待一段时间（400毫秒试了1次，结果不 行；1000毫秒则再没出现过问题）
     * 后再去connectGatt
     * 4、可以在连接前都startLeScan一下，成功率要高一点
     *
     * */

    private void initViews() {
        mWriteET = findViewById(R.id.et_write);
        mTips = findViewById(R.id.tv_tips);
        btnWrite = findViewById(R.id.btn_write);
        btnRead = findViewById(R.id.btn_read);
        btnNotify = findViewById(R.id.btn_notify);
        tvProperties = findViewById(R.id.tv_properties);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("详情信息");
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private Handler mHandler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            getBlueData();
        }
    };

    /**
     * 获取蓝牙数据
     */
    public void getBlueData() {
        if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
            if (dataInfoQueue.peek() != null) {
                //移除并返回队列头部元素
                byte[] bytes = dataInfoQueue.poll();
                for (byte byteChar : bytes) {
                    mBuilder.append(String.format("%d ", byteChar));
                }
                mBuilder.append(":").append(new String(bytes));
            }
            //检测还有数据，继续获取
            if (dataInfoQueue.peek() != null) {
                mHandler.post(runnable);
            }
        }
    }

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        //连接状态回调
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d("蓝牙", "onConnectionStateChange");
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                //todo 开启扫描服务:一般在设备连接成功后调用，扫描到设备服务后回调onServicesDiscovered()函数
                gatt.discoverServices();//发现远程设备及其提供的服务、特征和描述符。
            } else {
                isConnected = false;
                closeConn();
            }
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), dev));

        }

        /**
         * 每次发送的数据长度最大值为 MTU-3，早期低功耗蓝牙协议 MTU 固 定为 23（安卓 5.2 以前），所以每个数据包最多 20 个字节。
         * 之后的协议 MTU 可以修改，模块支持的 MTU 最大为 247，所以每个包最大的长度为 244 字节， 无论安卓手机还是苹果手机，都
         * 可以实现每个包传输 244 字节。
         * MTU 值模块 只能读取，不能设置，蓝牙未连接时 MTU 固定为 23，这个值没有任何意义。
         * 蓝牙连接后 MTU 值为模块和手机协商之后的值，通常是 247，可以用手机修 改 MTU 值。如果发现最长只能传输 20 个字节，
         * 说明手机端没有调用 setMTU 来设置 MTU 值。
         * 设置MTU的方法就是在回调函数中添加gatt.requestMtu(500)，500的意思就是设置每次可读取500个字节。
         * 我们还需要在回调BluetoothGattCallback中添加回调函数onMtuChanged()，程序调用过gatt.requestMtu(500)之后就会进入
         * onMtuChanged()回调函数。
         *
         * BLE蓝牙协议下数据的通信方式采用BluetoothGattService、BluetoothGattCharacteristic和BluetoothGattDescriptor三个主要的类实现通信。
         * （1）BluetoothGattService 简称服务，是构成BLE设备协议栈的组成单位，一个蓝牙设备协议栈一般由一个或者多个BluetoothGattService组成。
         * （2）BluetoothGattCharacteristic 简称特征，一个服务包含一个或者多个特征，特征作为数据的基本单元。
         * （3）一个BluetoothGattCharacteristic特征包含一个数据值和附加的关于特征的描述BluetoothGattDescriptor。
         *
         * BluetoothGattDescriptor用于描述特征的类，其同样包含一个value值。
         *
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("蓝牙", "onServicesDiscovered");
            //发现远程设备的服务，遍历各uuid
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) { // BLE服务发现成功
                //设置MTU，最大传输单元
                //gatt.requestMtu(500);

                //开启通知
                setNotify(null);

                // 遍历获取BLE服务Services/Characteristics/Descriptors的全部UUID
                for (BluetoothGattService service : gatt.getServices()) {//遍历服务
                    StringBuilder allUUIDs = new StringBuilder();
                    if (UuidUtil.attributes.get(service.getUuid().toString()) != null) {//服务类型
                        allUUIDs.append("UUIDs={\n").append(UuidUtil.attributes.get(service.getUuid().toString())).append(" Service：\n").append(service.getUuid().toString());
                    } else {
                        allUUIDs.append("UUIDs={\nUnKnow Service：\n").append(service.getUuid().toString());
                    }
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {//继续遍历服务的特征
                        if (UuidUtil.attributes.get(characteristic.getUuid().toString()) != null) {
                            allUUIDs.append(",\n").append(UuidUtil.attributes.get(characteristic.getUuid().toString())).append(" Characteristic：\n").append(characteristic.getUuid());
                        } else {
                            allUUIDs.append(",\nUnKnow Characteristic：\n").append(characteristic.getUuid());
                        }
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {//继续遍历特征的描述符
                            if (UuidUtil.attributes.get(descriptor.getUuid().toString()) != null) {
                                allUUIDs.append(",\n").append(UuidUtil.attributes.get(descriptor.getUuid().toString())).append(" Descriptor：\n").append(descriptor.getUuid());
                            } else {
                                allUUIDs.append(",\nUnKnow Descriptor：").append(descriptor.getUuid());
                            }
                        }
                    }
                    allUUIDs.append("}");
                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
                    logTv("发现服务" + allUUIDs);
                    //根据服务来获取每个UUID的值，每个属性都是通过UUID来确定的
                    String serviceUuid = service.getUuid().toString();
                    HashMap<String, BluetoothGattCharacteristic> charMap = new HashMap<>();
                    //获取所有特征服务的集合
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    //获取服务的所有特征集合
                    for (int j = 0; j < characteristics.size(); j++) {
                        charMap.put(characteristics.get(j).getUuid().toString(), characteristics.get(j));
                    }
                    servicesMap.put(serviceUuid, charMap);
                }
//                BluetoothGattCharacteristic bluetoothGattCharacteristic = getBluetoothGattCharacteristic(DEVICEA_UUID_SERVICE, DEVICEA_UUID_CHARACTERISTIC);
//                if (bluetoothGattCharacteristic == null) {
//                    return;
//                }
//                enableGattServicesNotification(bluetoothGattCharacteristic);

                //BLE蓝牙开发主要有负责通信的BluetoothGattService完成的。当且称为通信服务。通信服务通过硬件工程师提供的UUID获取。获取方式如下：
                //
                //BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(“蓝牙模块提供的负责通信UUID字符串”));
                //通信服务中包含负责读写的BluetoothGattCharacteristic，且分别称为notifyCharacteristic和writeCharacteristic。其中notifyCharacteristic负责开启监听，也就是启动收数据的通道，writeCharacteristic负责写入数据；
                //
//                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString("蓝牙模块提供的负责通信服务UUID字符串"));
//                // 例如形式如：49535343-fe7d-4ae5-8fa9-9fafd205e455
//                BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(UUID.fromString("notify uuid"));
//                BluetoothGattCharacteristic writeCharacteristic =  service.getCharacteristic(UUID.fromString("write uuid"));

            }
        }

        /**
         * 读取从设备中传过来的数据
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("蓝牙", "onCharacteristicRead");
            UUID uuid = characteristic.getUuid();
            byte[] value = characteristic.getValue();
            Log.d("蓝牙 onCharacteristicRead", HexUtils.bytesToHex(value));

            if (dataInfoQueue != null) {
                dataInfoQueue.clear();
                mBuilder = new StringBuilder();
                dataInfoQueue = ByteUtil.splitPacketFor20Byte(value); //todo 分包 对吗
            }
            getBlueData();//获取蓝牙数据
            String info = String.format("value: %s%s", "(0x)", ByteUtil.bytes2HexStr(characteristic.getValue()));//byte转为16进制
            String returnedPacket = mBuilder.toString().replace(" ", "");
            logTv("读取Characteristic[" + uuid + "]:\n" + info);
            Log.i(TAG, returnedPacket);
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, returnedPacket, status));
        }

        /**
         * 向特征设备中写入数据，特征中最多只能存放20个字节，超过需要循环接收或者发送
         * boolean 1个字节 char byte 1个字节 int float 4个字节 long double 8个字节
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("蓝牙", "onCharacteristicWrite");
            synchronized (locker) {
                UUID uuid = characteristic.getUuid();
                String valueStr = new String(characteristic.getValue());
                Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
                logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
            }
        }

        /**
         * 蓝牙返回的数据回调
         * 当订阅的Characteristic接收到消息时回调 连接成功回调
         * 当设备上某个特征发送改变的时候就需要通知APP，通过以下方法进行设置通知
         * 一旦接收到通知那么远程设备发生改变的时候就会回调 onCharacteristicChanged
         * when connected successfully will callback this method
         * this method can dealwith send password or data analyze
         * 不能够做耗时操作，否则会出现100%丢包
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("蓝牙", "onCharacteristicChanged");
            synchronized (locker) {
                UUID uuid = characteristic.getUuid();
                byte[] value = characteristic.getValue();
                if (dataInfoQueue != null) {
                    dataInfoQueue.clear();
                    mBuilder = new StringBuilder();
                    dataInfoQueue = ByteUtil.splitPacketFor20Byte(value);//分包
                }
                getBlueData();//
                String info = String.format("value: %s%s", "(0x)", ByteUtil.bytes2HexStr(characteristic.getValue()));
                String returnedPacket = mBuilder.toString().replace(" ", "");
                logTv("通知Characteristic[" + uuid + "]:\n" + returnedPacket + info);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("蓝牙", "onDescriptorRead");
            if (descriptor != null) {
                UUID uuid = descriptor.getUuid();
                String valueStr = Arrays.toString(descriptor.getValue());
                Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
                logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("蓝牙", "onDescriptorWrite");
            synchronized (locker) {//
                if (descriptor != null) {
                    UUID uuid = descriptor.getUuid();
                    String valueStr = Arrays.toString(descriptor.getValue());
                    Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
                    logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
                }

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    //开启监听成功，可以向设备写入命令了

                    Log.e(TAG, "开启监听成功");

                }
            }
        }

        //修改mtu回调
        /*
        单次写的数据大小有20字节限制，如何发送长数据？
        BLE单次写的数据量大小是有限制的，通常是20字节，可以尝试通过requestMTU增大，但不保证能成功。分包写是一种解决
        方案，需要定义分包协议，假设每个包大小20字节，分两种包，数据包和非数据包。对于数据包，头两个字节表示包的序号，
        剩下的都填充数据。对于非数据包，主要是发送一些控制信息。

        总体流程如下：
        （1）、定义通讯协议，如下(这里只是个举例，可以根据项目需求扩展)
        消息号(1个字节)功能(1个字节)子功能(1个字节)数据长度(2个字节)数据内容(N个字节)CRC校验(1个字节)  0101010000–2D

        （2）、封装通用发送数据接口(拆包)
        该接口根据会发送数据内容按最大字节数拆分(一般20字节)放入队列，拆分完后，依次从队列里取出发送

        （3）、封装通用接收数据接口(组包)
        该接口根据从接收的数据按协议里的定义解析数据长度判读是否完整包，不是的话把每条消息累加起来

        （4）、解析完整的数据包，进行业务逻辑处理

        （5）、协议还可以引入加密解密，需要注意的选算法参数的时候，加密后的长度最好跟原数据长度一致，这样不会影响拆包组包
        * */
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {//这里回调的mtu才是真正的
                Log.e("BLEService", "onMtuChanged success MTU = " + mtu);
                //我们一定要在回调函数onMtuChanged（）中重新设置特征值，否则你会发现你啥也收不到也发不出去。
                //displayGattServices(mBluetoothGatt.getServices());
            } else {
                Log.e("BLEService", "onMtuChanged fail ");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConn();
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            refreshGattCache(mBluetoothGatt);
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    //清理GATT层缓存
    public static boolean refreshGattCache(BluetoothGatt gatt) {
        boolean result = false;
        try {
            if (gatt != null) {
                Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    refresh.setAccessible(true);
                    result = (boolean) refresh.invoke(gatt, new Object[0]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //自定义测试使用 service 49535343-FE7D-4AE5-8FA9-9FAFD205E455
    String[] uuids = {
                                                    // read                  write
            "49535343-6daa-4d02-abf6-19569aca69fe", //000c000c0000000002     true
            "49535343-aca3-481c-91ec-d85e28a60318", //value 空               true
            "49535343-1e4d-4bd9-ba61-23c647249616", //false                  false          notify√
            "49535343-8841-43f4-a8d4-ecbe34729bb3", //false                  true =
            "49535343-aca3-481c-91ec-d85e28a60318" // value 空               TRUE
    };

    /**
     * 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
     * 读取数据成功会回调->onCharacteristicChanged()
     */
    public void read(View view) {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            UUID uuid = UUID.fromString(uuids[0]);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);//通过UUID获取可读的Characteristic
            boolean b = mBluetoothGatt.readCharacteristic(characteristic);
            Log.d("蓝牙", "读取数据" + b);
        }
    }

    /**
     * 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
     * 写入数据成功会回调->onCharacteristicWrite()
     *
     * 读写问题
     * 蓝牙的写入操作, 读取操作必须序列化进行. 写入数据和读取数据是不能同时进行的, 如果调用了写入数据的方法,
     * 马上调用又调用写入数据或者读取数据的方法,第二次调用的方法会立即返回 false, 代表当前无法进行操作；
     **/
    public void write(View view) {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            String text = mWriteET.getText().toString();

            UUID uuid = UUID.fromString(uuids[3]);

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);//通过UUID获取可写的Characteristic
            if (!TextUtils.isEmpty(text)) {
                characteristic.setValue(text.getBytes()); //单次最多20个字节 value一般为Hex格式指令，其内容由设备通信的蓝牙通信协议规定；
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                boolean isSuccess = mBluetoothGatt.writeCharacteristic(characteristic);//
                if (isSuccess) {
                    Log.d(TAG, " --------- writeCharacteristic --------- Success");
                } else {
                    Log.d(TAG, " --------- writeCharacteristic --------- Fail");
                }
            }
        }
    }

    public void clear(View view) {
        mTips.setText("");
    }

    /**
     * 接收通知消息（setCharacteristicNotification)
     *
     * 前期进行BLE开发或许很容易混淆读操作和接收通知消息，这里我按自己的理解粗糙的讲解一下。
     * 通知是BLE终端主动或是相关操作触发而发出的数据，任何一个用于权限的主机都可以读取到这个数据。
     * 而读操作时谁进行读数据操作，然后BLE终端才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
     *
     * 设置通知Characteristic变化会回调->onCharacteristicChanged()
     */
    public void setNotify(View view) {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            UUID uuid = UUID.fromString(uuids[2]);

/*//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
//            if(mBluetoothGatt.setCharacteristicNotification(characteristic, true)){
//                //获取到Notify当中的Descriptor通道 然后再进行注册
//                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
//                for (BluetoothGattDescriptor descriptor : descriptors) {
//                    boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    if (b1) {
//                        boolean b2 = mBluetoothGatt.writeDescriptor(descriptor);
//                        Log.d("蓝牙", "描述 UUID :" + descriptor.getUuid().toString());
//                    }
//                    Log.d("蓝牙", "startRead: " + "监听接收数据开始" + b1);
//                }
////                BluetoothGattDescriptor clientConfig = characteristic .getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
////                clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
////                mBluetoothGatt.writeDescriptor(clientConfig);
//            }*/

//            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可通知的Characteristic

            if (characteristic.getDescriptors().size() > 0) {
                //Filter descriptors based on the uuid of the descriptor
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    Log.d("descriptor:  " , descriptor.getUuid().toString());//00002902-0000-1000-8000-00805f9b34fb
                    if (descriptor != null) {
                        //Write the description value
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//这个
                        } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                            //两个都是通知的意思，notify和indication的区别在于，notify只是将你要发的数据发送给手机，没有确认机制，
                            //不会保证数据发送是否到达。而indication的方式在手机收到数据时会主动回一个ack回来。即有确认机制，只有收
                            //到这个ack你才能继续发送下一个数据。这保证了数据的正确到达，也起到了流控的作用。所以在打开通知的时候，需要设置一下。
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        }
                        mBluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            }

            //设置通知方式放在处理描述符后面，否则设置通知无法回调 onCharacteristicChanged 方法，放在描述符之前，调用readCharacteristic会直接调用
            //onCharacteristicRead 而不调用onCharacteristicChanged
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                //有活的特征通知，先清除，赋值为空，在重新设置通知获取
                if (mNotifyCharacteristic1 != null) {
                    setCharacteristicNotification(//
                            mNotifyCharacteristic1, false);
                    mNotifyCharacteristic1 = null;
                }
                readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic1 = characteristic;
                setCharacteristicNotification(//
                        characteristic, true);//这个位置有关系吗？
            }
            appearButton();
        }
    }

    private void appearButton() {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);
            int charaProp = characteristic.getProperties();
            StringBuilder builder = new StringBuilder();
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                builder.append("READ,");
                btnRead.setVisibility(View.VISIBLE);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                builder.append("WRITE,");
                btnWrite.setVisibility(View.VISIBLE);
                mWriteET.setVisibility(View.VISIBLE);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                builder.append("WRITE_NO_RESPONSE,");
                btnWrite.setVisibility(View.VISIBLE);
                mWriteET.setVisibility(View.VISIBLE);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                builder.append("NOTIFY,");
                btnNotify.setVisibility(View.VISIBLE);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                builder.append("INDICATE,");
                btnNotify.setVisibility(View.VISIBLE);
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
                tvProperties.setText(String.format("Properties: %s", builder.toString()));
            }
        }
    }

    // 获取Gatt服务
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("没有找到服务UUID=" + uuid, 0);
        return service;
    }

    // 输出日志
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


    /**
     * 读取特征值，会回调onCharacteristicRead方法
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        boolean isSuccess = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (isSuccess) {
            Log.d(TAG, " --------- setCharacteristicNotification --------- Success");
        } else {
            Log.d(TAG, " --------- setCharacteristicNotification --------- Fail");
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 点击返回图标事件
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    /**
//     * 根据服务UUID和特征UUID,获取一个特征{@link BluetoothGattCharacteristic}
//     *
//     * @param serviceUUID   服务UUID
//     * @param characterUUID 特征UUID
//     */
//    private BluetoothGattCharacteristic getBluetoothGattCharacteristic(String serviceUUID, String characterUUID) {
//        if (null == mBluetoothGatt) {
//            Log.e(TAG, "mBluetoothGatt is null");
//            return null;
//        }
//
//        //找服务
//        Map<String, BluetoothGattCharacteristic> bluetoothGattCharacteristicMap = servicesMap.get(serviceUUID);
//        if (null == bluetoothGattCharacteristicMap) {
//            Log.e(TAG, "Not found the serviceUUID!");
//            return null;
//        }
//
//        //找特征
//        Set<Map.Entry<String, BluetoothGattCharacteristic>> entries = bluetoothGattCharacteristicMap.entrySet();
//        BluetoothGattCharacteristic gattCharacteristic = null;
//        for (Map.Entry<String, BluetoothGattCharacteristic> entry : entries) {
//            if (characterUUID.equals(entry.getKey())) {
//                gattCharacteristic = entry.getValue();
//                break;
//            }
//        }
//        return gattCharacteristic;
//    }
//
//
//    private void enableGattServicesNotification(BluetoothGattCharacteristic gattCharacteristic) {
//        if (gattCharacteristic == null) return;
//        setNotify(gattCharacteristic);
//    }
//
//    private void setNotify(BluetoothGattCharacteristic characteristic) {
//
//        final int charaProp = characteristic.getProperties();
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            // If there is an active notification on a characteristic, clear
//            // it first so it doesn't update the data field on the user interface.
//            //有活的特征通知，先清除，赋值为空，在重新设置通知获取
//            if (mNotifyCharacteristic1 != null) {
//                setCharacteristicNotification(
//                        mNotifyCharacteristic1, false);
//                mNotifyCharacteristic1 = null;
//            }
//            readCharacteristic(characteristic);
//        }
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//            mNotifyCharacteristic1 = characteristic;
//            setCharacteristicNotification(
//                    characteristic, true);
//        }
//    }
}
