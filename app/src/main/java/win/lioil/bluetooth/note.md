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

## 参考 BLE掘金
https://juejin.cn/post/7281276425219817472#heading-37

##频段、距离、带宽
蓝牙是一种近距离无线通信技术，运行在2.4GHz免费频段，它的特性就是近距离通信，典型距离是 10 米以内，传输速度最高可达 24 Mbps，
支持多连接，安全性高，非常适合用智能设备上。
1、使用距离范围，蓝牙3.0的有效传输距离为10米左右，而蓝牙5.0有效工作距离可达300米。
2、数据传输速度，蓝牙3.0的数据传输率每秒大约在200k左右，而蓝牙5.0传输速度为2Mbps。
5.0版本蓝牙可以向下兼容3.0或2.0版本，
蓝牙5.0传输速度上限为24Mbps，低功耗模式传输速度是之前4.2LE版本的两倍，有效工作距离可达300米，是4.2LE版本的4倍。

Android中的蓝牙
目前最新的蓝牙协议是蓝牙5.3版本（截止到2023年9月22日）
Android 4.3 开始，开始支持BLE功能，但只支持Central（中心角色or主机）
Android 5.0开始，开始支持Peripheral（外设角色or从机）

中心模式和外设模式是什么意思？
Central Mode： Android端作为中心设备，连接其他外围设备。
Peripheral Mode：Android端作为外围设备，被其他中心设备连接。
在Android 5.0支持外设模式之后，才算实现了两台Android手机通过BLE进行相互通信。

##GAP
GAP（Generic Access Profile），它用来控制设备连接和广播。GAP 使你的设备被其他设备可见，并决定了你的设备是否可以或者
怎样与设备进行交互。例如 Beacon 设备就只是向外发送广播，不支持连接；小米手环就可以与中心设备建立连接。

##广播数据包
在 GAP 中蓝牙设备可以向外广播数据包，广播包分为两部分： Advertising Data Payload（广播数据）和 Scan Response Data 
Payload（扫描回复），每种数据最长可以包含 31 byte。这里广播数据是必需的，因为外设必需不停的向外广播，让中心设备知道它的
存在。扫描回复是可选的，中心设备可以向外设请求扫描回复，这里包含一些设备额外的信息，例如设备的名字。

##ScanRecord
ScanCallback --> onScanResult(ScanResult) --> ScanRecord
在 Android 5.0 也提供 ScanRecord 帮你解析，直接可以通过这个类获得有意义的数据。广播中可以有哪些数据类型呢？设备连接属性，
标识设备支持的 BLE 模式，这个是必须的。设备名字，设备包含的关键 GATT service，或者 Service data，厂商自定义数据等等。

##广播的时间间隔 能耗
外围设备会设定一个广播间隔，每个广播间隔中，它会重新发送自己的广播数据。广播间隔越长，越省电，同时也不太容易扫描到。

##交互方式
GAP决定了你的设备怎样与其他设备进行交互。答案是有2种方式：
##完全基于广播的方式 
也有些情况是不需要连接的，只要外设广播自己的数据即可。用这种方式主要目的是让外围设备，把自己的信息发送给多个中心设备。使用
广播这种方式最典型的应用就是苹果的 iBeacon。这是苹果公司定义的基于 BLE 广播实现的功能，可以实现广告推送和室内定位。这也
说明了，APP 使用 BLE，需要定位权限。
基于非连接的，这种应用就是依赖 BLE 的广播，也叫作 Beacon。这里有两个角色，发送广播的一方叫做 Broadcaster，监听广播的
一方叫 Observer。

##基于GATT连接的方式
大部分情况下，外设通过广播自己来让中心设备发现自己，并建立 GATT 连接，从而进行更多的数据交换。这里有且仅有两个角色，发起
连接的一方，叫做中心设备—Central，被连接的设备，叫做外设—Peripheral。
外围设备：这一般就是非常小或者简单的低功耗设备，用来提供数据，并连接到一个更加相对强大的中心设备，例如小米手环。
中心设备：中心设备相对比较强大，用来连接其他外围设备，例如手机等。
GATT 连接需要特别注意的是：GATT 连接是独占的。也就是一个 BLE 外设同时只能被一个中心设备连接。一旦外设被连接，它就会马
上停止广播，这样它就对其他设备不可见了。当设备断开，它又开始广播。中心设备和外设需要双向通信的话，唯一的方式就是建立 GATT 连接。
GATT 通信的双方是 C/S 关系。外设作为 GATT 服务端（Server），它维持了 ATT 的查找表以及 service 和 characteristic 
的定义。中心设备是 GATT 客户端（Client），它向 Server 发起请求。需要注意的是，所有的通信事件，都是由客户端发起，并且接收服务端的响应。

## BLE通信基础
BLE通信的基础有两个重要的概念，ATT和GATT。 

ATT
全称 attribute protocol，中文名“属性协议”。它是 BLE 通信的基础。 
ATT 把数据封装，向外暴露为“属性”，提供“属性”的为服务端，获取“属性”的为客户端。
ATT 是专门为低功耗蓝牙设计的，结构非常简单，数据长度很短。 

GATT
全称 Generic Attribute Profile， 中文名“通用属性配置文件”。它是在ATT 的基础上，
对 ATT 进行的进一步逻辑封装，定义数据的交互方式和含义。GATT是我们做 BLE 开发的时候直接接触的概念。

GATT 层级
GATT按照层级定义了4个概念：配置文件（Profile）、服务（Service）、特征（Characteristic）和描述（Descriptor）。
## 他们的关系是这样的：Profile 就是定义了一个实际的应用场景，一个 Profile包含若干个 Service，
一个 Service 包含若干个 Characteristic，一个 Characteristic 可以包含若干 Descriptor。

Profile
Profile 并不是实际存在于 BLE 外设上的，它只是一个被 Bluetooth SIG 或者外设设计者预先定义的 Service 的集合。例如心率Profile
（Heart Rate Profile）就是结合了 Heart Rate Service 和 Device Information Service。所有官方通过 GATT Profile 的列表可以从这里找到。

Service
Service 是把数据分成一个个的独立逻辑项，它包含一个或者多个 Characteristic。每个 Service 有一个 UUID 唯一标识。 UUID 
有 16 bit 的，或者 128 bit 的。16 bit 的 UUID 是官方通过认证的，需要花钱购买，128 bit 是自定义的，这个就可以自己随便设置。
官方通过了一些标准 Service，完整列表在这里。以 Heart Rate Service为例，可以看到它的官方通过 16 bit UUID 是 0x180D，包含
3 个 Characteristic：Heart Rate Measurement, Body Sensor Location 和 Heart Rate Control Point，并且定义了只有第
一个是必须的，它是可选实现的。

Characteristic
需要重点提一下Characteristic， 它定义了数值和操作，包含一个Characteristic声明、Characteristic属性、值、值的描述(Optional)。
通常我们讲的 BLE 通信，其实就是对 Characteristic 的读写或者订阅通知。比如在实际操作过程中，我对某一个Characteristic进行读，就是获
取这个Characteristic的value。

UUID
Service、Characteristic 和 Descriptor 都是使用 UUID 唯一标示的。
UUID 是全局唯一标识，它是 128bit 的值，为了便于识别和阅读，
一般以 “8位-4位-4位-4位-12位”的16进制标示，比如“12345678-abcd-1000-8000-123456000000”。

但是，128bit的UUID 太长，考虑到在低功耗蓝牙中，数据长度非常受限的情况，蓝牙又使用了所谓的 16 bit 或者 32 bit 的 UUID，
形式如下：“0000XXXX-0000-1000-8000-00805F9B34FB”。除了 “XXXX” 那几位以外，其他都是固定，
所以说，其实 16 bit UUID 是对应了一个 128 bit 的 UUID。
这样一来，UUID 就大幅减少了，例如 16 bit UUID只有有限的 65536（16的四次方） 个。
与此同时，因为数量有限，所以 16 bit UUID 并不能随便使用。蓝牙技术联盟已经预先定义了一些 UUID，我们可以直接使用，
比如“00001011-0000-1000-8000-00805F9B34FB”就一个是常见于BLE设备中的UUID。当然也可以花钱定制自定义的UUID。

## todo 电脑通过插入HLK-B40蓝牙透传模块，获得蓝牙BLE连接能力



