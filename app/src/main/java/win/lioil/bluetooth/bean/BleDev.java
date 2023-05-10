package win.lioil.bluetooth.bean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import java.util.Objects;

public class BleDev {
    private BluetoothDevice dev;
    private ScanResult scanResult;
    //信号的强弱可以得到相距的距离 @BluetoothUtil
    private int rssi;//接收信号强度
    private long rssiUpdateTime;

    public BluetoothDevice getDev() {
        return dev;
    }

    public void setDev(BluetoothDevice dev) {
        this.dev = dev;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getRssiUpdateTime() {
        return rssiUpdateTime;
    }

    public void setRssiUpdateTime(long rssiUpdateTime) {
        this.rssiUpdateTime = rssiUpdateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BleDev bleDev = (BleDev) o;
        return Objects.equals(dev, bleDev.dev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dev);
    }
}
