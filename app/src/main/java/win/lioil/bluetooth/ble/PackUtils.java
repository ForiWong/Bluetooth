package win.lioil.bluetooth.ble;

//...
public class PackUtils {
    private BtBuffer btBuffer = new BtBuffer();//缓存数组
    private int pkgSize;//包长度

    private void append(byte[] value) {
        //开启notify之后，我们就可以在这里接收数据了。
        btBuffer.appendBuffer(value);//接到后面
        while (true) {
            boolean ret = subPackageOnce(btBuffer);
            if (false == ret) break;
        }
    }

    private boolean subPackageOnce(BtBuffer buffer) {
        if (null == buffer) return false;
        if (buffer.getBufferSize() >= 14) {
            byte[] rawBuffer = buffer.getBuffer();
            if (isHead(rawBuffer)) {//假如是帧头
                pkgSize = byteToInt(rawBuffer[2], rawBuffer[3]);//长度
            } else {
                pkgSize = -1;
                for (int i = 0; i < rawBuffer.length - 1; ++i) {
                    if (rawBuffer[i] == -2 && rawBuffer[i + 1] == 1) {
                        buffer.releaseFrontBuffer(i);//把i之后的舍去
                        return true;//
                    }
                }
                return false;
            }
            //剥离数据
            if (pkgSize > 0 && pkgSize <= buffer.getBufferSize()) {
                byte[] bufferData = buffer.getFrontBuffer(pkgSize);
                long time = System.currentTimeMillis();
                buffer.releaseFrontBuffer(pkgSize);
                //在这处理数据
                //deal something......
                return true;
            }
        }
        return false;
    }

    //判断是否为头部，一般使用多个byte组合为一个复杂的字符串即可。
    private boolean isHead(byte[] rawBuffer) {
        return  byteToInt(rawBuffer[0], rawBuffer[1])  == -1;
    }

    //获取长度
    private int byteToInt(byte b, byte b1) {
        return 0;
    }
}
