package com.troop.freedcam.sonyapi.sonystuff;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by troop on 02.02.2015.
 */
public class DataExtractor
{
    public static int commonHeaderlength = 1 + 1 + 2 + 4;
    public static int payloadHeaderlength = 128;
    private static String TAG = DataExtractor.class.getSimpleName();

    public byte[] jpegData;
    public byte[] paddingData;
    public List<FrameInfo> frameInfoList;
    public int jpegSize;
    public int paddingSize;
    public int frameCount;
    public String version;

    public int frameDataSize;
    public int singelFrameDataSize;

    public DataExtractor(InputStream mInputStream) throws IOException {
        ExtractData(mInputStream);
    }

    public CommonHeader commonHeader;
    public PayLoadHeader payLoadHeader;

    public void ExtractData(InputStream mInputStream) throws IOException
    {

        try {
            createHeader(mInputStream);
        }
        catch (IOException ex)
        {
            commonHeader = null;
        }


        /*commonHeader = new CommonHeader(SimpleLiveviewSlicer.readBytes(mInputStream, commonHeaderlength));
        if (commonHeader.PayloadType == 0x12)
        {
            int readLength = 4 + 3 + 1 + 2 + 118 + 4 + 4 + 24;
            commonHeader = null;
            SimpleLiveviewSlicer.readBytes(mInputStream, readLength);
        }
        payLoadHeader = new PayLoadHeader(SimpleLiveviewSlicer.readBytes(mInputStream, payloadHeaderlength));
        readData(mInputStream);
        paddingData = SimpleLiveviewSlicer.readBytes(mInputStream, paddingSize);*/
    }

    private void createHeader(InputStream mInputStream) throws IOException
    {
        commonHeader = new CommonHeader(SimpleLiveviewSlicer.readBytes(mInputStream, commonHeaderlength));
        if (commonHeader.PayloadType == 0x12)
        {
            int readLength = 4 + 3 + 1 + 2 + 118 + 4 + 4 + 24;
            commonHeader = null;
            SimpleLiveviewSlicer.readBytes(mInputStream, readLength);
        }
        else if (commonHeader.PayloadType == 0x11)
        {
            payLoadHeader = new PayLoadHeader(SimpleLiveviewSlicer.readBytes(mInputStream, payloadHeaderlength));
            readData(mInputStream);
            paddingData = SimpleLiveviewSlicer.readBytes(mInputStream, paddingSize);
        }
        else {
            payLoadHeader = new PayLoadHeader(SimpleLiveviewSlicer.readBytes(mInputStream, payloadHeaderlength));
            readData(mInputStream);
            paddingData = SimpleLiveviewSlicer.readBytes(mInputStream, paddingSize);
        }
    }

    private void readData(InputStream mInputStream) throws IOException {
        if (commonHeader.PayloadType == 1)
            jpegData = SimpleLiveviewSlicer.readBytes(mInputStream, jpegSize);


        if (commonHeader.PayloadType == 2)
        {
            frameInfoList = new ArrayList<FrameInfo>();
            //int framC = frameDataSize /singelFrameDataSize;
            for (int i = 0; i<frameCount; i++)
            {
                byte[] framebytes = SimpleLiveviewSlicer.readBytes(mInputStream, singelFrameDataSize);
                if (framebytes.length == singelFrameDataSize)
                    frameInfoList.add(new FrameInfo(framebytes));
            }
        }
    }


    public class CommonHeader
    {
        public int PayloadType;
        public int Sequencenumber;
        public CommonHeader(byte[] bytes) throws IOException {
            if (bytes == null || bytes.length != commonHeaderlength) {
                throw new IOException("Cannot read stream for common header.");
            }

            if (bytes[0] != (byte) 0xFF)
            {
                throw new IOException("Unexpected data format. (Start byte)");
            }
            PayloadType = bytes[1];
            Sequencenumber = SimpleLiveviewSlicer.bytesToInt(bytes,2,2);

        }
    }

    public class PayLoadHeader
    {
        public PayLoadHeader(byte[] bytes) throws IOException
        {
            if (bytes == null || bytes.length != payloadHeaderlength) {
                throw new IOException("Cannot read stream for payload header.");
            }
            if (bytes[0] != (byte) 0x24 || bytes[1] != (byte) 0x35
                    || bytes[2] != (byte) 0x68
                    || bytes[3] != (byte) 0x79) {
                throw new IOException("Unexpected data format. (Start code)");
            }
            if (commonHeader.PayloadType == 1)
                jpegSize = SimpleLiveviewSlicer.bytesToInt(bytes, 4, 3);
            paddingSize = SimpleLiveviewSlicer.bytesToInt(bytes, 7, 1);

            if (commonHeader.PayloadType == 2)
            {
                frameDataSize = SimpleLiveviewSlicer.bytesToInt(bytes, 4, 3);
                version = SimpleLiveviewSlicer.bytesToInt(bytes, 8, 1) + "." + SimpleLiveviewSlicer.bytesToInt(bytes, 9, 1);
                frameCount = SimpleLiveviewSlicer.bytesToInt(bytes, 10, 2);
                //Log.d(TAG, "FrameCount:" + frameCount);
                if (version.equals("1.0"))
                    singelFrameDataSize = 16;
                else
                    singelFrameDataSize = SimpleLiveviewSlicer.bytesToInt(bytes, 12,2);
            }

        }


    }
    public class FrameInfo
    {
        int Top,Left,Bottom,Right, Category, Status, AditionalStatus;
        public FrameInfo(byte[] bytes)
        {
            Left = SimpleLiveviewSlicer.bytesToInt(bytes,0,2);
            Top = SimpleLiveviewSlicer.bytesToInt(bytes,2,2);
            Right = SimpleLiveviewSlicer.bytesToInt(bytes,4,2);
            Bottom = SimpleLiveviewSlicer.bytesToInt(bytes,6,2);
            Category = SimpleLiveviewSlicer.bytesToInt(bytes, 8,1);
            Status = SimpleLiveviewSlicer.bytesToInt(bytes, 9,1);
            AditionalStatus = SimpleLiveviewSlicer.bytesToInt(bytes, 10,1);
            //Log.d(TAG, Top + ", "+Left+","+ Bottom+","+Right+"," +Category +"," + Status);
        }
    }
}








