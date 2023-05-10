package win.lioil.bluetooth.util;

/**
 * 0、1、2、3、4、5、6、7、8、9 A、B、C、D、E、F
 * 一个十六进制数（Hex），正好为4个二进制位。一个字节（byte）为8个二进制位。
 * 因此，我们可以将一个byte用两个Hex表示，同理，我们也可以将两个Hex转换为一个byte。
 */
public class HexUtils {

    public static void main(String[] args) {
        String hex = "AA000501";
        String crc = getCRC(hexToByteArray(hex));
        System.out.println(hex + crc); //AA000507 636e
        byte[] bytes = hexToByteArray(hex + crc);//[-86, 0, 5, 7, 99, 110]
        String all = bytesToHex(bytes);//aa000507636e
    }

    /**
     * 字节转十六进制
     * @param b 需要进行转换的byte字节
     * @return  转换后的Hex字符串
     */
    public static String byteToHex(byte b){
        String hex = Integer.toHexString(b & 0xFF);
        if(hex.length() < 2){
            hex = "0" + hex;
        }
        return hex;
    }

    /**
     * 字节数组转16进制
     * @param bytes 需要转换的byte数组
     * @return  转换后的Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() < 2){
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Hex字符串转byte
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte
     */
    public static byte hexToByte(String inHex){
        return (byte)Integer.parseInt(inHex,16);
    }

    /**
     * hex字符串转byte数组
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte数组结果
     */
    public static byte[] hexToByteArray(String inHex){
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1){
            //奇数
            hexlen++;
            result = new byte[(hexlen/2)];
            inHex="0"+inHex;
        }else {
            //偶数
            result = new byte[(hexlen/2)];
        }
        int j=0;
        for (int i = 0; i < hexlen; i+=2){
            result[j]=hexToByte(inHex.substring(i,i+2));
            j++;
        }
        return result;
    }


    /**
     * 计算CRC16校验码
     *
     * @param bytes 字节数组
     * @return {@link String} 校验码
     * @since 1.0
     *
     *  ModBus 通信协议的 CRC ( 冗余循环校验码含2个字节, 即 16 位二进制数。
     *  CRC 码由发送设备计算, 放置于所发送信息帧的尾部。
     *  接收信息设备再重新计算所接收信息 (除 CRC 之外的部分）的 CRC,
     *  比较计算得到的 CRC 是否与接收到CRC相符, 如果两者不相符, 则认为数据出错。
     */
    public static String getCRC(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        CRC = ( (CRC & 0x0000FF00) >> 8) | ( (CRC & 0x000000FF ) << 8);
        return Integer.toHexString(CRC);
    }
}
