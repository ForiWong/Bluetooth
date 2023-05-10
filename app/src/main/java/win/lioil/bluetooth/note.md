## HC-02
HC-02蓝牙串口模块是基于蓝牙2.0并兼容BLE的双模蓝牙数传模块。

## 单模与双模
单模蓝牙芯片：单一传统蓝牙芯片，单一低功耗蓝牙芯片
双模蓝牙芯片：同时支持传统蓝牙跟低功耗蓝牙的芯片

蓝牙4.0开始就是包括蓝牙BLE了。蓝牙4.0是双模的，既包括经典蓝牙又包括低能耗蓝牙。经典蓝牙和蓝牙BLE虽然都是蓝牙，
但其实还是存在很大区别的。蓝牙BLE相比于经典蓝牙的优点是搜索、连接的速度更快，关键就是BLE(Bluetooth Low Energy)
低能耗，缺点呢就是传输的速度慢，传输的数据量也很小，每次只有20个字节。但是蓝牙BLE因为其低能耗的优点，在智能穿
戴设备和车载系统上的应用越来越广泛。


## 经典蓝牙和BLE的区别
Android手机蓝牙4.x都是双模蓝牙(既有经典蓝牙也有低功耗蓝牙)，而某些蓝牙设备为了省电是单模(只支持低功耗蓝牙)
开发者选经典蓝牙，还是BLE？

经典蓝牙：
1.传声音
如蓝牙耳机、蓝牙音箱。蓝牙设计的时候就是为了传声音的，所以是近距离的音频传输的不二选择。
现在也有基于WIFI的音频传输方案，例如Airplay等，但是WIFI功耗比蓝牙大很多，设备无法做到便携。
因此固定的音响有WIFI的，移动的如耳机、便携音箱清一色都是基于经典蓝牙协议的。

2.传大量数据
例如某些工控场景，使用Android或Linux主控，外挂蓝牙遥控设备的，
可以使用经典蓝牙里的SPP协议，当作一个无线串口使用。速度比BLE传输快多了。
这里要注意的是，iPhone没有开放

BLE蓝牙:
耗电低，数据量小，如遥控类(鼠标、键盘)，传感设备(心跳带、血压计、温度传感器、共享单车锁、智能锁、防丢器、室内定位)
是目前手机和智能硬件通信的性价比最高的手段，直线距离约50米，一节5号电池能用一年，传输模组成本10块钱，远比WIFI、4G等大数据量的通信协议更实用。
虽然蓝牙距离近了点，但胜在直连手机，价格超便宜。以室内定位为例，商场每家门店挂个蓝牙beacon，
就可以对手机做到精度10米级的室内定位，一个beacon的价格也就几十块钱而已

双模蓝牙:
如智能电视遥控器、降噪耳机等。很多智能电视配的遥控器带有语音识别，需要用经典蓝牙才能传输声音。
而如果做复杂的按键，例如原本键盘表上没有的功能，经典蓝牙的HID按键协议就不行了，得用BLE做私有协议。
包括很多降噪耳机上通过APP来调节降噪效果，也是通过BLE来实现的私有通信协议。


## 指示和通知的区别
通知和指示在作用上很相似，都是在客户端请求后，服务器发相应的数据给客户端。不同的地方在协议层，通知是没有安全性质
的发送，但是指示是有应答机制的。
notification(通知)：无应答，速度快。
indication(指示)：有应答，更安全。 
indication的方式在手机收到数据时会主动回一个ack回来。即有确认机制，只有收到这个ack你才能继续发送下一个数据。
这保证了数据的正确到达，也起到了流控的作用。所以在打开通知的时候，需要设置一下。


## 写请求和写命令区别在于:
1、写入请求(Write Request)用于向服务器请求写入属性的值，并在写入响应(Write Response)中确认这已实现。
2、写入响应Write Response)是作为对有效写入请求(Write Response)的回复而发送的，并且确认属性已成功写入。
3、写命令写入命令(WriteCommand)用于向服务器请求写入属性的值，没有确认机制。
4、read一般主动来读电量等，然后service主动将value赋值给回复给client。


## SPP 与 RFCOMM
Android蓝牙中涉及通用数据传输协议的有两种：
SPP协议、BLE(Bluetooth low energy)协议

SPP协议是Android 2.0引入的API，是通过Socket的形式来实现数据传输及交互，有分客户端和服务端，手机一般以
客户端的角色主动连接SPP协议设备。
BLE协议是Android 4.3引入的API，但手机厂商大部份在Android 4.4上才支持BLE，即低功耗蓝牙，一般我们开发的
话是使用中央（BluetoothGatt）或者外围（BluetoothGattServer）来进行开发的，手机正常情况下当作中央设备
来接收信息，而蓝牙模块当作是外围设备发送数据。

服务端的Socket建立与客户端差不多，需注意以下两个步骤：
创建监听listen（蓝牙没有此监听，但需要通过whlie（true）死循环来一直监听的）
通过accept()，如果有客户端连接，会创建一个新的Socket，体现出并发性，可以同时与多个socket通讯）
总之：SPP的连接和操作相对比较简单，考虑的事情也少。需要注意的是频繁数据发送（间隔时间短）的情况下，APP
收到的数据并不是按原先约定的TLV一包一包数据，可能会被拆包，应对的办法就是启动线程接收完数据，然后去一包一包取。

串行端口配置文件(Serial Port Profile, SPP)
SPP定义了如何设置虚拟串行端口及如何连接两个蓝牙设备.SPP基于ETSI TS 07.10规格,使用RFCOMM协议提供串行商品仿真.
SPP提供了以无线方式替代现有的RS-232串行通信应用程序和控制信号的方法.SPP为DUN,FAX,HSP和LAN配置文件提供了基础.
此配置文件可以支持最高128kb/s的数据率.SPP依赖于GAP.

RFCOMM协议就是在L2CAP上进行串口（RS-232 9针）仿真。
spp是rfcomm的上层协议，那么上层应用为什么不是直接调用rfcomm的服务，还要经过中间的spp做什么呢？
配合电脑等有串口协议层来的，还有就是和单片机连接的时候，用SPP替代串口线会更适合。比如BF10蓝牙模块采用SPP协议，
那么对于单片机来说就非常的简单，不用去管RFCOMM的一些东西。

## NRF
NRF52832是一款由挪威无线通信芯片制造商Nordic Semiconductor开发的低功耗蓝牙（BLE）系统芯片。

## Service和Characteristic、Property
相对来说，Service是服务，Characteristic则是特征值。一般来说，蓝牙里面有多个Service，一个Service里面包括多个Characteristic。

一个蓝牙协议里面包含的Service和Characteristic是比较多的 ，那么这么多的同名属性用什么来区分呢？就是UUID，每个Service或者
Characteristic都有一个 128 bit 的UUID来标识。Service可以理解为一个功能集合，而Characteristic比较重要，蓝牙设备正是通过
Characteristic来进行设备间的交互的，这些Characteristic又包含一些属性Property，如读、写、订阅等操作。

## UUID
UUID （Universally Unique Identifier）用于标识蓝牙服务以及特征访问属性，不同的蓝牙服务和属性使用不同的访问方法，找到正确的UUID，
才能使用正确的功能。


## 蓝牙基础协议
想了解蓝牙通信之前，需要先了解蓝牙两个最基本的协议：GAP 和 GATT。 

## GAP（Generic Access Profile）简介
GAP是【通用访问配置文件】的首字母缩写，主要控制蓝牙连接和广播。GAP使蓝牙设备对外界可见，并决定设备是否可以或者怎样与其他设备进行交互。
GAP定义了多种角色，但主要的两个是：中心设备 和 外围设备。

中心设备：可以扫描并连接多个外围设备,从外设中获取信息。

外围设备：小型，低功耗，资源有限的设备。可以连接到功能更强大的中心设备，并为其提供数据。

GAP广播数据

GAP 中外围设备通过两种方式向外广播数据：广播数据 和 扫描回复( 每种数据最长可以包含 31 byte。)。

广播数据是必需的，因为外设必需不停的向外广播，让中心设备知道它的存在。而扫描回复是可选的，中心设备可以向外设请求扫描回复，
这里包含一些设备额外的信息。

外围设备会设定一个广播间隔。每个广播间隔中，它会重新发送自己的广播数据。广播间隔越长，越省电，同时也不太容易扫描到。

## 广播的网络拓扑结构

外设通过广播自己让中心设备发现自己，并建立 GATT 连接，从而进行更多的数据交换。但有些情况是不需要连接的，只要外设广播自己的数据即可。
目的是让外围设备，把自己的信息发送给多个中心设备。
##因为基于 GATT 连接的方式的，只能是一个外设连接一个中心设备。

## GATT（Generic Attribute Profile）简介
GATT配置文件是一个通用规范，用于在BLE链路上发送和接收被称为“属性”的数据块。目前所有的BLE应用都基于GATT。

BLE设备通过叫做 Service 和 Characteristic 的东西进行通信
GATT使用了 ATT（Attribute Protocol）协议，ATT 协议把 Service, Characteristic对应的数据保存在一个查询表中，
次查找表使用 16 bit ID 作为每一项的索引。 GATT 连接是独占的。
##也就是一个 BLE 外设同时只能被一个中心设备连接。一旦外设被连接，它就会马上停止广播，这样它就对其他设备不可见了。
当外设与中心设备断开，外设又开始广播，让其他中心设备感知该外设的存在。而中心设备可同时与多个外设进行连接。

GATT 通信
中心设备和外设需要双向通信的话，唯一的方式就是建立 GATT 连接。
GATT 通信的双方是 C/S 关系。外设作为 GATT 服务端（Server），它维持了 ATT 的查找表以及 service 和 characteristic 的定义。
中心设备是 GATT 客户端（Client），它向 外设（Server） 发起请求来获取数据。

## ble 的 OTA 升级（over-the-air，又称空中升级、DFU 升级等）
进行固件升级
其实，整个流程也很简单，因为 Nordic Semiconductor 这家公司已经帮我们提供好了 DFU 升级的库了，只需要学会怎么用就好了，
感兴趣的同学也可以去看看源码，最后附上传送门：https://github.com/NordicSemiconductor/Android-DFU-Library。

其实 DFU 的升级还是要移动端和硬件共同完成，不同的产品会有不同的协议商定，但从根本上看都是一样的，换汤不换药，就是
先让 ble 设备进入 DFU 模式，然后把固件升级包发送给固件进行升级。

## 分包 粘包
在android中，BLE的特征一次读写最大长度20字节。
对于长时间连续发送的大于20字节的帧结构，如果安卓终端进行接收的话，就需要我们进行重新组帧（即如何处理粘包和丢包问题）。对于如何处理这个问题，首先需要在帧结构上做好设计。
可以通过设置，增大MTU的值： gatt.requestMtu(512);

尝试通过requestMTU增大，但不保证能成功。
分包写是一种解决方案，需要定义分包协议。
简单的协议：假设每个包大小20字节，分两种包，数据包和非数据包。对于数据包，头两个字节表示包的序号，剩下的都填充数据。对于非数据包，主要是发送一些控制信息。
复杂的协议：一个比较完整的帧，应包含帧头，帧长度，帧序号，数据部分以及帧尾CRC，通过这些信息来做判断是否丢帧和重新组帧。

1.粘包的过程
    1）先校验每帧的合法性
    2）再通过帧序号进行排序
    3）最后组合起来就行。
2.分包发送的过程
就是将内容与帧头等，拼一起后，再均分成MTU单元，逐个发送。
参考：可以看看FastBle的分包发送源码

int size = 168；
byte[] bytes = s.getBytes();
byte[] value = {0x4a, 0x58, 0x57, 0x40, 0x41, (byte) (bytes.length & 0xff)};
byte[] concat = concat(value, bytes);
List<byte[]> wifiInfos = new ArrayList<>();//存放分包
if (concat.length > size) {
    for (int i = 0; i < concat.length; i += size) {
        if (i + size < concat.length) {
            byte[] infoData = new byte[size];
            System.arraycopy(concat, i, infoData, 0, infoData.length);
            wifiInfos.add(infoData);
        } else {
            byte[] infoData = new byte[concat.length - i];
            System.arraycopy(concat, i, infoData, 0, infoData.length);
            wifiInfos.add(infoData);
        }
    }
} else {
    wifiInfos.add(concat);
}

//逐个发送
for (int i = 0; i < wifiInfos.size(); i++) {
    byte[] concat1 = wifiInfos.get(i);
    integrationData(concat1);
    BluetoothHelper.getInstance(context).writeData(concat1, UUID_write);
    try {
        Thread.sleep(8);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
