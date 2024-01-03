package com.reactnativethermalprinter;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.util.Log;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.connection.usb.UsbConnection;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

@ReactModule(name = ThermalPrinterModule.NAME)
public class ThermalPrinterModule extends ReactContextBaseJavaModule {
  private static final String LOG_TAG = "RN_Thermal_Printer";
  public static final String NAME = "ThermalPrinterModule";
  private Promise jsPromise;
  private ArrayList<BluetoothConnection> btDevicesList = new ArrayList();

  private UsbManager usbManager;
  private UsbConnection usbConnection;

  private static final String ACTION_USB_PERMISSION = "com.reactnativethermalprinter.usb_permission";


  public ThermalPrinterModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void printTcp(String ipAddress, double port, String payload, boolean autoCut, boolean openCashbox, double mmFeedPaper, double printerDpi, double printerWidthMM, double printerNbrCharactersPerLine, double timeout, Promise promise) {
//
//        05-05-2021
//        https://reactnative.dev/docs/native-modules-android
//        The following types are currently supported but will not be supported in TurboModules. Please avoid using them:
//
//        Integer -> ?number
//        int -> number
//        Float -> ?number
//        float -> number
//
    this.jsPromise = promise;
    try {
      TcpConnection connection = new TcpConnection(ipAddress, (int) port, (int) timeout);
      this.printIt(connection, payload, autoCut, openCashbox, mmFeedPaper, printerDpi, printerWidthMM, printerNbrCharactersPerLine);
    } catch (Exception e) {
      this.jsPromise.reject("Connection Error", e.getMessage());
    }
  }

  @ReactMethod
  public void printBluetooth(String macAddress, String payload, boolean autoCut, boolean openCashbox, double mmFeedPaper, double printerDpi, double printerWidthMM, double printerNbrCharactersPerLine, Promise promise) {
    this.jsPromise = promise;
    BluetoothConnection btPrinter;

    if (TextUtils.isEmpty(macAddress)) {
      btPrinter = BluetoothPrintersConnections.selectFirstPaired();
    } else {
      btPrinter = getBluetoothConnectionWithMacAddress(macAddress);
    }

    if (btPrinter == null) {
      this.jsPromise.reject("Connection Error", "Bluetooth Device Not Found");
    }

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH}, 1);
    } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
    } else {
      try {
        this.printIt(btPrinter.connect(), payload, autoCut, openCashbox, mmFeedPaper, printerDpi, printerWidthMM, printerNbrCharactersPerLine);
      } catch (Exception e) {
        this.jsPromise.reject("Connection Error", e.getMessage());
      }
    }
  }

  @ReactMethod
  public void getBluetoothDeviceList(Promise promise) {
    this.jsPromise = promise;
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH}, 1);
    } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
    } else {
      try {
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        WritableArray rnArray = new WritableNativeArray();
        if (pairedDevices.size() > 0) {
          int index = 0;
          for (BluetoothDevice device : pairedDevices) {
            btDevicesList.add(new BluetoothConnection(device));
            JSONObject jsonObj = new JSONObject();

            String deviceName = device.getName();
            String macAddress = device.getAddress();

            jsonObj.put("deviceName", deviceName);
            jsonObj.put("macAddress", macAddress);
            WritableMap wmap = convertJsonToMap(jsonObj);
            rnArray.pushMap(wmap);
          }
        }
        jsPromise.resolve(rnArray);


      } catch (Exception e) {
        this.jsPromise.reject("Bluetooth Error", e.getMessage());
      }
    }
  }

  /*==============================================================================================
  ===========================================USB PART=============================================
  ==============================================================================================*/

  private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      String payload = intent.getStringExtra("payload");
      Boolean autoCut = intent.getBooleanExtra("autoCut", false);
      Boolean openCashbox = intent.getBooleanExtra("openCashbox", false);
      Double mmFeedPaper = intent.getDoubleExtra("mmFeedPaper", 20);
      Double printerDpi = intent.getDoubleExtra("printerDpi", 203);
      Double printerWidthMM = intent.getDoubleExtra("printerWidthMM", 80);
      Double printerNbrCharactersPerLine = intent.getDoubleExtra("printerNbrCharactersPerLine", 42);

      if (ThermalPrinterModule.ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
          UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (usbManager != null && usbDevice != null) {
              UsbConnection printerConnection = new UsbConnection(usbManager, usbDevice);
              printIt(printerConnection, payload, autoCut, openCashbox, mmFeedPaper, printerDpi, printerWidthMM, printerNbrCharactersPerLine);
            }
          }
        }
      }
    }
  };


  @ReactMethod
  public void printUsb(String payload, boolean autoCut, boolean openCashbox, double mmFeedPaper, double printerDpi, double printerWidthMM, double printerNbrCharactersPerLine, Promise promise) {
    this.jsPromise = promise;
    Log.i("DEBUG", "LIST DEVICES....");

    this.usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    this.usbConnection = UsbPrintersConnections.selectFirstConnected(getReactApplicationContext());
    Log.i("DEBUG USB CONNECTION", String.valueOf(this.usbConnection));

    if(this.usbConnection != null && usbManager != null) {
      Log.i("DEBUG", "THERE IS USB CONNECTION AND USB MANAGER");
      Intent intent = new Intent();
      intent.setAction(ThermalPrinterModule.ACTION_USB_PERMISSION);
      intent.putExtra("payload", payload);
      intent.putExtra("autoCut", autoCut);
      intent.putExtra("openCashbox", openCashbox);
      intent.putExtra("mmFeedPaper", mmFeedPaper);
      intent.putExtra("printerDpi", printerDpi);
      intent.putExtra("printerWidthMM", printerWidthMM);
      intent.putExtra("printerNbrCharactersPerLine", printerNbrCharactersPerLine);

      PendingIntent permissionIntent = PendingIntent.getBroadcast(
        getReactApplicationContext(),
        0,
        intent,
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
      );
      IntentFilter filter = new IntentFilter(ThermalPrinterModule.ACTION_USB_PERMISSION);


      getCurrentActivity().registerReceiver(usbReceiver, filter);
      usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }


    Log.i("DEBUG", String.valueOf(this.usbManager.getDeviceList()));
  }

  private Bitmap getBitmapFromUrl(String url) {
    try {
      Bitmap bitmap = Glide
        .with(getCurrentActivity())
        .asBitmap()
        .load(url)
        .submit()
        .get();
      return bitmap;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Synchronous printing
   */

  private String preprocessImgTag(EscPosPrinter printer, String text) {

    Pattern p = Pattern.compile("(?<=\\<img\\>)(.*)(?=\\<\\/img\\>)");
    Matcher m = p.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String firstGroup = m.group(1);
      m.appendReplacement(sb, PrinterTextParserImg.bitmapToHexadecimalString(printer, getBitmapFromUrl(firstGroup)));
    }
    m.appendTail(sb);

    return sb.toString();
  }

  private void printIt(DeviceConnection printerConnection, String payload, boolean autoCut, boolean openCashbox, double mmFeedPaper, double printerDpi, double printerWidthMM, double printerNbrCharactersPerLine) {
    try {
      EscPosPrinter printer = new EscPosPrinter(printerConnection, (int) printerDpi, (float) printerWidthMM, (int) printerNbrCharactersPerLine);
      String processedPayload = preprocessImgTag(printer, payload);

      if (openCashbox) {
        printer.printFormattedTextAndOpenCashBox(processedPayload, (float) mmFeedPaper);
      } else if (autoCut) {
        printer.printFormattedTextAndCut(processedPayload, (float) mmFeedPaper);
      } else {
        printer.printFormattedText(processedPayload, (float) mmFeedPaper);
      }

      printer.disconnectPrinter();
      this.jsPromise.resolve(true);
    } catch (EscPosConnectionException e) {
      this.jsPromise.reject("Broken connection", e.getMessage());
    } catch (EscPosParserException e) {
      this.jsPromise.reject("Invalid formatted text", e.getMessage());
    } catch (EscPosEncodingException e) {
      this.jsPromise.reject("Bad selected encoding", e.getMessage());
    } catch (EscPosBarcodeException e) {
      this.jsPromise.reject("Invalid barcode", e.getMessage());
    } catch (Exception e) {
      this.jsPromise.reject("ERROR", e.getMessage());
    }
  }

  private BluetoothConnection getBluetoothConnectionWithMacAddress(String macAddress) {
    for (BluetoothConnection device : btDevicesList) {
      if (device.getDevice().getAddress().contentEquals(macAddress))
        return device;
    }
    return null;
  }

  private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
    WritableMap map = new WritableNativeMap();

    Iterator<String> iterator = jsonObject.keys();
    while (iterator.hasNext()) {
      String key = iterator.next();
      Object value = jsonObject.get(key);
      if (value instanceof JSONObject) {
        map.putMap(key, convertJsonToMap((JSONObject) value));
      } else if (value instanceof Boolean) {
        map.putBoolean(key, (Boolean) value);
      } else if (value instanceof Integer) {
        map.putInt(key, (Integer) value);
      } else if (value instanceof Double) {
        map.putDouble(key, (Double) value);
      } else if (value instanceof String) {
        map.putString(key, (String) value);
      } else {
        map.putString(key, value.toString());
      }
    }
    return map;
  }
}
