package com.example.thermal_lite.camera;

import android.util.Log;

import com.energy.ac020library.IrcamEngine;
import com.energy.ac020library.IrcmdEngine;
import com.energy.ac020library.bean.IrcmdError;
import com.energy.commoncomponent.Const;
import com.energy.commonlibrary.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by fengjibo on 2023/3/29.
 * 设备控制类，应于APP同一生命周期
 */
public class DeviceIrcmdControlManager {

    private static final String TAG = "DeviceIrcmdControlManager";
    //图像交互类
    private IrcamEngine mIrcamEngine;
    //命令交互类
    private IrcmdEngine mIrcmdEngine;

    private boolean mSendFPGACommand = false;
    private boolean mSendISPCommand = false;

    //
    private String ispParamPath;

    private DeviceIrcmdControlManager() {

    }

    private static DeviceIrcmdControlManager mInstance;

    public static synchronized DeviceIrcmdControlManager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceIrcmdControlManager();
        }
        return mInstance;
    }

    public IrcmdEngine getIrcmdEngine() {
        return mIrcmdEngine;
    }

    public void setIrcmdEngine(IrcmdEngine ircmdEngine) {
        this.mIrcmdEngine = ircmdEngine;
    }

    public IrcamEngine getIrcamEngine() {
        return mIrcamEngine;
    }

    public void setIrcamEngine(IrcamEngine ircamEngine) {
        this.mIrcamEngine = ircamEngine;
    }

    public void setSendFPGACommand(boolean sendFPGACommand) {
        mSendFPGACommand = sendFPGACommand;
    }

    /**
     * 发送fpga算法参数指令
     */
    public void sendFPGAParam() {
        if (!mSendFPGACommand) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "sendFPGAParam");
                try {
                    //todo 暂时先一条一条指令发送
                    String fpga_param_path = Const.DATA_FILE_SAVE_PATH + File.separator + "fpga.json";
                    File file = new File(fpga_param_path);
                    if (!file.exists()) {
                        return;
                    }
                    String fpgaParams = FileUtil.getStringFromFile(fpga_param_path);
                    int firstAddress = 0x0096;

                    JSONArray jsonArray = new JSONArray(fpgaParams);
                    Log.d(TAG, "first jsonArray length : " + jsonArray.length());
//                    float[] params = new float[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        int[] params = new int[1];
                        String name = jsonObject.getString("name");
                        String address = jsonObject.getString("address");
                        double value = jsonObject.getDouble("value");
                        params[0] = (int) value;
                        Log.d(TAG, "first params value : " + params[0]);
//                        if (i == 0) {
//                            Log.d(TAG, "first address string : " + address);
//                            firstAddress = Integer.parseInt(address.substring(2), 16);
//                            Log.d(TAG, "first address int : " + firstAddress);
//                        }
                        int reAddress = Integer.parseInt(address.substring(2), 16);
                        Log.d(TAG, "first address string : " + reAddress);
                        if (mIrcmdEngine != null) {
                            IrcmdError algorithmParametersWriteGet = mIrcmdEngine
                                    .advAlgorithmParametersWrite(reAddress, params);
                            Log.d(TAG, "algorithmParametersWriteGet result = " + algorithmParametersWriteGet);

                            //获取FPGA算法参数读取 PASS
                            int[] algorithmParametersReadData = new int[1];
                            IrcmdError algorithmParametersReadGet = mIrcmdEngine
                                    .advAlgorithmParametersRead(reAddress, algorithmParametersReadData);

                            Log.d(TAG, "algorithmParametersReadGet result = " + algorithmParametersReadGet);

                            for (int j = 0; j < algorithmParametersReadData.length; j++) {
                                Log.d(TAG, "algorithmParametersReadGet value = " + algorithmParametersReadData[j]);
                            }
                        }
                    }

//                    if (mIrcmdEngine != null) {
//                        IrcmdError algorithmParametersWriteGet = mIrcmdEngine
//                                .advAlgorithmParametersWrite(firstAddress, params);
//                        Log.d(TAG, "algorithmParametersWriteGet result = " + algorithmParametersWriteGet);
//
//                        //获取FPGA算法参数读取 PASS
//                        float[] algorithmParametersReadData = new float[jsonArray.length()];
//                        IrcmdError algorithmParametersReadGet = mIrcmdEngine
//                                .advAlgorithmParametersRead(firstAddress, algorithmParametersReadData);
//
//                        Log.d(TAG, "algorithmParametersReadGet result = " + algorithmParametersReadGet);
//
//                        for (int i = 0; i < algorithmParametersReadData.length; i ++) {
//                            Log.d(TAG, "algorithmParametersReadGet value = " + algorithmParametersReadData[i]);
//                        }
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSendFPGACommand = false;
            }
        }).start();
    }

    /**
     * 大端模式转换
     * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。  和bytesToInt2（）配套使用
     */
    public static byte[] intToBytes2(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public static byte[] intToBytes2(long value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 大端模式转换
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value =
                (((src[offset] & 0xFF) << 24) | ((src[offset + 1] & 0xFF) << 16) | ((src[offset + 2] & 0xFF) << 8) | (src[offset + 3] & 0xFF));
        return value;
    }

    /**
     * 获取isp读取到的值
     *
     * @param name
     * @param ispParamReadByteArray
     * @param byteWidth
     * @param begin
     * @param end
     * @return
     */
    public static String getReadValue(String name, byte[] ispParamReadByteArray, int byteWidth, int begin, int end) {
        // 读取出来的一个int，共四个字节的值
        StringBuilder ispParamReadByteArrStr = new StringBuilder();
        // 00000111 00000000 00000000 00000000
        for (int i = 0; i < ispParamReadByteArray.length; i++) {
            ispParamReadByteArrStr.append(String.format("%8s",
                    Integer.toBinaryString(ispParamReadByteArray[i] & 0xFF)).replace(' ', '0'));
        }
        Log.i(TAG, "name = " + name + " ispParamReadByteArrStr = " + ispParamReadByteArrStr.toString() +
                " ispParamReadByteArrStrInt = " + Long.parseLong(ispParamReadByteArrStr.toString(), 2));

        String orgValue = ispParamReadByteArrStr.substring(byteWidth * 8 - end - 1, byteWidth * 8 - begin);
        Log.i(TAG, "name = " + name + " orgValue = " + orgValue +
                " orgValueInt = " + Long.parseLong(orgValue, 2));

        //01110000   00000000 00001010 00000000 00000001
        return String.valueOf(Long.parseLong(orgValue, 2));
    }

    /**
     * int类型的byte数组，转为二进制字符串，然后根据传入的值做替换，最终输出拼装好的值
     *
     * @param name
     * @param ispParamReadByteArray
     * @param byteWidth
     * @param begin
     * @param end
     * @param valueArray
     * @return
     */
    public static long byteArrToBinStr(String name, byte[] ispParamReadByteArray, int byteWidth, int begin, int end,
                                       byte[] valueArray) {
        // 读取出来的一个int，共四个字节的值
        StringBuilder ispParamReadByteArrStr = new StringBuilder();
        // 00000111 00000000 00000000 00000000
        for (int i = 0; i < ispParamReadByteArray.length; i++) {
            ispParamReadByteArrStr.append(String.format("%8s",
                    Integer.toBinaryString(ispParamReadByteArray[i] & 0xFF)).replace(' ', '0'));
        }
        Log.i(TAG, "name = " + name + " ispParamReadByteArrStr = " + ispParamReadByteArrStr.toString() +
                " ispParamReadByteArrStrInt = " + Long.parseLong(ispParamReadByteArrStr.toString(), 2));

        // 要写入的值，以int类型给出，共四个字节  0000000000000001 00000000 10001001
        StringBuilder valueArrStr = new StringBuilder();
        // 00000111 00000000 00000000 00000000
        for (int i = 0; i < valueArray.length; i++) {
            valueArrStr.append(String.format("%8s", Integer.toBinaryString(valueArray[i] & 0xFF)).replace(' ', '0'));
        }
        Log.i(TAG, "name = " + name + " valueArrStr = " + valueArrStr.toString());


        String orgValue = ispParamReadByteArrStr.substring(byteWidth * 8 - end - 1, byteWidth * 8 - begin);
        Log.i(TAG, "name = " + name + " orgValue = " + orgValue +
                " orgValueInt = " + Long.parseLong(orgValue, 2));

        // 需要根据begin和end来截取要传入的值,然后替换读取出来的值
        String valueStr = ispParamReadByteArrStr.replace(byteWidth * 8 - end - 1, byteWidth * 8 - begin,
                valueArrStr.substring(byteWidth * 8 - end - 1, byteWidth * 8 - begin)).toString();

        Log.i(TAG, "name = " + name + " valueStr = " + valueArrStr.toString() + " valueStr = " + valueStr +
                " valueStrInt = " + Long.parseLong(valueStr, 2));

        //01110000   00000000 00001010 00000000 00000001
        return Long.parseLong(valueStr, 2);
    }

    /**
     * 设置ISP算法需要修改的文件路径
     * 会在重新回到预览页面的时候调用sendISPParam方法来设置参数
     *
     * @param param_path
     */
    public void setISPChangePath(String param_path) {
        ispParamPath = param_path;
        mSendISPCommand = true;
    }

    /**
     * 会在重新回到预览页面的时候调用sendISPParam方法来设置参数
     * <p>
     * ISP参数设置后，停图后不会保存参数，固件会重新下发
     *
     * @throws IllegalArgumentException
     */
    public void sendISPParam() {
        if (!mSendISPCommand) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "sendISPParam");
                try {
                    if (ispParamPath == null || ispParamPath.isEmpty()) {
                        return;
                    }
                    File file = new File(ispParamPath);
                    if (!file.exists()) {
                        return;
                    }
                    String fpgaParams = FileUtil.getStringFromFile(ispParamPath);

                    JSONArray jsonArray = new JSONArray(fpgaParams);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        long[] ispParamWriteData = new long[1];
                        String name = jsonObject.getString("name");
                        String address = jsonObject.getString("address");
                        if (address.startsWith("0x") || address.startsWith("0X")) {
                            address = address.substring(2);
                        }
                        int begin = jsonObject.getInt("begin");
                        int end = jsonObject.getInt("end");
                        int value = jsonObject.getInt("value");
//                        Log.i(TAG, "name = " + name + " address = " + address + " begin = " + begin + " end = " +
//                                end + " value = " + value);
                        int reAddress = Integer.parseInt(address, 16);
                        if (mIrcmdEngine != null) {
                            // 需要先把该地址的值读出来
                            long[] ispParamReadData = new long[1];
                            if (IrcmdError.IRCMD_SUCCESS != mIrcmdEngine.advISPParamRead(reAddress, ispParamReadData)) {
                                throw new IllegalArgumentException("The method advISPParamRead execute fail.");
                            }
                            // 然后单独修改begin到end之前的值，重新写入进去
                            ispParamWriteData[0] = byteArrToBinStr(name, intToBytes2(ispParamReadData[0]), 4,
                                    begin, end, intToBytes2(value));
//                            if (IrcmdError.IRCMD_SUCCESS != mIrcmdEngine.advISPParamWrite(reAddress,
//                                    ispParamWriteData)) {
//                                throw new IllegalArgumentException("The method advISPParamWrite execute fail.");
//                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSendISPCommand = false;
                ispParamPath = null;
            }
        }).start();
    }
}
