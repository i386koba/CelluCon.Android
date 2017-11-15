package com.cirlution.i386koba.droidrone;

        import android.accounts.AccountManager;
        import android.app.Activity;
        import android.app.AlertDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.pm.PackageInfo;
        import android.content.pm.PackageManager;
        import android.hardware.GeomagneticField;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.location.Location;
        import android.location.LocationManager;
        import android.os.Build;
        import android.os.Bundle;
        import android.support.annotation.NonNull;
        import android.telephony.PhoneStateListener;
        import android.telephony.SignalStrength;
        import android.telephony.TelephonyManager;
        import android.text.method.ScrollingMovementMethod;
        import android.util.Log;
        import android.view.KeyEvent;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;

        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.api.GoogleApiClient;
        import com.google.android.gms.location.LocationListener;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationServices;
        import com.google.api.client.extensions.android.http.AndroidHttp;
        import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.json.gson.GsonFactory;
        import com.google.api.services.drive.Drive;
        import com.google.api.services.drive.DriveScopes;
        //import com.google.gson.Gson;

        import java.io.IOException;
        import java.lang.reflect.InvocationTargetException;
        import java.lang.reflect.Method;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Collections;
        import java.util.Date;
        import java.util.List;
        import java.util.Locale;
        import java.util.Set;
        import java.util.UUID;
        import java.util.regex.Pattern;

        import io.skyway.Peer.Browser.Canvas;
        import io.skyway.Peer.Browser.MediaConstraints;
        import io.skyway.Peer.Browser.MediaStream;
        import io.skyway.Peer.Browser.Navigator;
        import io.skyway.Peer.CallOption;
        import io.skyway.Peer.DataConnection;
        import io.skyway.Peer.MediaConnection;
        import io.skyway.Peer.OnCallback;
        import io.skyway.Peer.Peer;
        import io.skyway.Peer.PeerError;
        import io.skyway.Peer.PeerOption;

//import android.support.v7.app.AppCompatActivity;
//import android.view.WindowManager;

//まめ　AndroidStudio　Control + Y 行削除　Control + D　行コピー
//AndroidStudioでプロジェクト名・プロジェクトフォルダを変更する
//http://qiita.com/le_skamba/items/f838fe51a3396c26a262

// Android StudioとBitbucketを使ったGitHub flowの流れ
// http://qiita.com/araiyusuke/items/1d141756f53cf3634c9a#%E3%83%96%E3%83%A9%E3%83%B3%E3%83%81%E4%BD%9C%E6%88%90

// Android 4.0.3 API Level15 Ice Cream Sandwich MR1
// https://github.com/nttcom/SkyWay-Android-SDK

// Skyway　app/libs にSDKコピーして　以下を Build｡gradle に追加
// https://github.com/nttcom/SkyWay-Android-Sample/blob/master/app/build.gradle

//Android アプリで Google Drive API を使う http://vividcode.hatenablog.com/entry/20130908/1378613811
//上記から変更　グーグルのAPIを使うときに欠かせないGoogle OAuthの作り方と使い方 (3/3)
//http://www.atmarkit.co.jp/ait/articles/1509/15/news017_3.html

//Change LocationClient to GoogleApiClient http://edywrite.blogspot.jp/2015/03/change-locationclient-to-googleapiclient.html
//[Android] FusedLocationProviderApi を使って位置情報を取得 https://akira-watson.com/android/fusedlocationproviderapi.html

//Freetel MIYABI adbドライバーのインストール方法(Windows 7) http://bengallight.hatenablog.com/entry/2015/10/17/163929

public class MainActivity extends Activity implements SensorEventListener, LineReceiveListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];
    private float[] orientationValues = new float[3];
    //private float[] gyroscopeValues = new float[3];

    List<Sensor> listMag;
    List<Sensor> listAcc;
    //List<Sensor> listGyro;
    private SensorManager sensorManager;
    //方位補正
    private float geoMag = 0;

    //ミリ秒データでセンサー更新 //GPS,BT通信間隔
    private long currentTimeMillis = System.currentTimeMillis();
    //private long lastGpsUpTimeMillis = currentTimeMillis;
    private long lastSensorUpTimeMillis = currentTimeMillis;
    private long lastBtUpTimeMillis = 0;
    private long lastPeerCloseTimeMillis = 0;

    //GPS文字列
    private long num = 0;
    private String tNum = "0";
    private String tLat = "\"NoData\"";
    private String tLng = "\"NoData\"";
    private String tAltitude = "0";
    private String tAccuracy = "0";

    //Unique UUID for SPP application
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Bluetooth blue;
    private String msgReceived; // 受信したメッセージを表示するまで一時的に保持する領域
    private String btMsgReceived = ""; // 受信したメッセージを送信するまで一時的に保持する領域
    //private String lastBtMsgReceived = "";
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;

    //SkyWay
    //nttcom/SkyWay-Android-Sample/app/src/main/java/io/skyway/testpeerjava/DataActivity.java
    //https://github.com/nttcom/SkyWay-Android-Sample/blob/master/app/src/main/java/io/skyway/testpeerjava/DataActivity.java
    private String _id;
    private Peer _peer;
    private DataConnection _data ;
    //https://github.com/nttcom/SkyWay-Android-Sample/blob/master/app/src/main/java/io/skyway/testpeerjava/MediaActivity.java
    //nttcom/SkyWay-Android-Sample/app/src/main/java/io/skyway/testpeerjava/MediaActivity.java
    private MediaConnection _media;
    private MediaStream _msLocal;
    //private Canvas canvasBackCamera;
    //private MediaStream msFront;
    private String peerRes = "";
    private TextView sensorView;
    private TextView pidView;
    private TextView peerView;
    private TextView btView;
    private String gpsStr = "GPS Reception waiting.";
    private Boolean _bConnecting = false;
    //private Boolean _bCalling = false;
    private int peerConnErrCount = 0;
    //Drive REST API を使用
    //https://developers.google.com/drive/v2/reference/
    //Drive REST API Android Quickstart
    //https://developers.google.com/drive/v2/web/quickstart/android#step_3_create_a_new_android_project
    //グーグルのAPIを使うときに欠かせないGoogle OAuthの作り方と使い方 (3/3)
    //http://www.atmarkit.co.jp/ait/articles/1509/15/news017_3.html
    private static final int REQUEST_ACCOUNT_CHOOSER = 1;
    public static final int REQUEST_AUTHORIZATION_FROM_DRIVE = 2;
    private GoogleAccountCredential mCredential;
    private Drive mDrive;
    private String activityResult = "";
    private String batLevel;
    private String batTemp;
    private String lteSignalStrength;

    //LocationClientは廃止され、代わりにGoogleApiClientを使うことになりました。
    //http://ja.stackoverflow.com/questions/1748/google-play-service-sdk-6-5%E3%81%A7%E3%81%AElocationclient%E3%81%AE%E4%BB%A3%E3%82%8F%E3%82%8A%E3%81%AE%E5%AE%9F%E8%A3%85%E3%81%AF
    //Change LocationClient to GoogleApiClient http://edywrite.blogspot.jp/2015/03/change-locationclient-to-googleapiclient.html
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private TelephonyManager telephonyManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // フルスクリーン指定
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // (1)各種センサーの用意
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        listMag = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        listAcc = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        //listGyro = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Bluetooth
        blue = new Bluetooth(); // ブルートゥースの生成
        blue.setReceiveListener(this); // 受信データのハンドラを登録
        //テキスト表示
        //Viewの階層構造をあやつる  http://ichitcltk.hustle.ne.jp/gudon2/index.php?pageType=file&id=Android059_ViewTree
        //View view = getLayoutInflater().inflate(R.layout.activity_droidrone, null);
        //LayoutInflaterでViewGroupをnullにすると警告出るけど同等のView.inflate()でやると出ないよってか
        //addContentView(view, new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
        setContentView(R.layout.activity_droidrone);
        //ボタン操作
        btn1 = (Button) findViewById(R.id.button1);//接続
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btConnect();
            }
        });
        btn2 = (Button) findViewById(R.id.button2);//接続解除
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btDisConnect();
            }
        });
        btn3 = (Button) findViewById(R.id.button3);//debug rps
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btMsgReceived = "RPS:1";     //ホールセンサー Debugボタン
            }
        });
        //getUserMedia後のインカメラ/アウトカメラ切り替え https://html5experts.jp/sho-y/17863/
        btn4 = (Button) findViewById(R.id.button4); //camraSW
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean result = _msLocal.switchCamera();
                if ( result )  {
                    //Success
                    Log.e(getTag(), "switchCamera Success.");
                } else {
                    //Failed
                    Log.e(getTag(), "switchCamera Failed.");
                }
            }
        });
        // メッセージ表示用ビューを取得
        sensorView = (TextView) findViewById(R.id.sensorText);
        pidView = (TextView) findViewById(R.id.pidText);
        // ScrollView を使わないで TextView に スクロールバーを表示する
        // http://kokufu.blogspot.jp/2012/12/scrollview-textview.html
        peerView = (TextView) findViewById(R.id.peerText);
        peerView.setMovementMethod(ScrollingMovementMethod.getInstance());
        btView = (TextView) findViewById(R.id.btText);
        btView.setMovementMethod(ScrollingMovementMethod.getInstance());


        //スリープするとエラーが出て終了する問題。結局、画面消灯で画面が縦になるので、画面横固定にしていると画面回転でActivityを破棄される問題だった。
        //回転時にActivityを破棄させない方法  http://kobegdg.blogspot.jp/2012/11/activity.html

        // Keep screen on  画面をスリープ状態にさせないためには http://www.adakoda.com/android/000207.html
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //上記は電源を押されるとスリープされてしまうのでダメ
        //画面を ON のままにする方法      http://android.keicode.com/basics/how-to-keep-screen-on.php
        //電源ボタン押されても起動したままにしたかったのですが、これも電源ボタン押されれば画面消灯してしまいます。

        //2016.11.15 結局、縦画面固定にした。

        //sensorManager用意
        if (listMag.size() > 0) {
            sensorManager.registerListener(this, listMag.get(0), SensorManager.SENSOR_DELAY_UI);
        }
        if (listAcc.size() > 0) {
            sensorManager.registerListener(this, listAcc.get(0), SensorManager.SENSOR_DELAY_UI);
        }
        //if (listGyro.size() > 0) {
            //sensorManager.registerListener(this, listGyro.get(0), SensorManager.SENSOR_DELAY_UI);
        //}

        // GPSサービス取得（locationManager　から　LocationClient to GoogleApiClientに変更 2016.06）
        //Change LocationClient to GoogleApiClient http://edywrite.blogspot.jp/2015/03/change-locationclient-to-googleapiclient.html
        //現在位置取得（GPS起動）していないと終了時にエラーになる
        //起動時に現在位置取得（GPS起動）のダイアログ（2016・07）
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //GPSセンサーが利用可能か？
        if ( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Not start GPS.\n Starting？").setCancelable(false)
                    //GPS設定画面起動用ボタンとイベントの定義
                    .setPositiveButton("GPS Setting", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id) {
                            Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(callGPSSettingIntent);
                        }
                    });
            //キャンセルボタン処理
            alertDialogBuilder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id){
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            // 設定画面へ移動するかの問い合わせダイアログを表示
            alert.show();
        }
        connectGooglePlayServices();
        //Signal Strengthの取得方法 http://www.tknology.org/?p=28
        //SignalStrengthListener ps = new SignalStrengthListener(this);
        telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    }

    // GooglePlayServicesに接続する
    protected void connectGooglePlayServices() {
        mLocationRequest = LocationRequest.create();
        // 10秒おきに位置情報を取得する
        mLocationRequest.setInterval(10000);
        // 精度優先
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // 最短で5秒おきに位置情報を取得する
        // mLocationRequest.setFastestInterval(5000);
        // mLocationClient = new LocationClient(getActivity().getApplicationContext(), connectionCallbacks, onConnectionFailedListener);
        // mLocationClient.connect();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        // GoogleApiClient start
        mGoogleApiClient.connect();
    }

    // GooglePlayServicesを切断する
    protected void disConnectGooglePlayServices() {
        if (mGoogleApiClient.isConnected()) { //GPS起動していないとエラー
            // 位置情報の取得を停止
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            //mGoogleApiClient.removeLocationUpdates(locationListener);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //ACCESS_FINE_LOCATION  は、Android 6.0 Runtime Permissionに該当,
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //onCreate と onStart と onResume の違い http://qiita.com/centum7/items/ac0d61e884519a4c61bb
    @Override
    public void onStart() {
        super.onStart();
    }
    Method getLteSignalStrength;
    @Override
    protected void onResume() {
        super.onResume();
        //バッテリ情報 受信を開始
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
        //Google APIs Client Library for JavaからGoogle Drive APIを使用する
        //http://qiita.com/kubotaku1119/items/9df79c568e100c0c7623
        if (mCredential == null) { //Google account 選択
            //Collection<String> scopes = Arrays.asList(DriveScopes.DRIVE_READONLY,PlusScopes.USERINFO_PROFILE);
            //public class DriveScopes Available OAuth 2.0 scopes for use with the Drive API.
            //https://developers.google.com/resources/api-libraries/documentation/drive/v2/java/latest/com/google/api/services/drive/DriveScopes.html
            Collection<String> scopes = Collections.singletonList(DriveScopes.DRIVE_FILE);
            mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), scopes);
            //Drive REST API
            //https://developers.google.com/drive/v2/web/quickstart/android#step_5_setup_the_sample
            //mCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(SCOPES));
            startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_CHOOSER);
            // Debug
            //mCredential.getGoogleAccountManager();

            //パッケージ情報の取得（PackageInfo, getPackageManager()）
            //http://androidgamepark.blogspot.jp/2013/02/packageinfo-getpackagemanager.html
            try {
                //PackageInfoの取得
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
                //パッケージのバージョンネームの取得   //String versionName    = packageInfo.versionName;
                //パッケージのバージョンコードの取得   //int versionCode       = packageInfo.versionCode;
                //最初にインストールした日付           //long firstInstallTime = packageInfo.firstInstallTime;
                //最後にインストールした日付          //long lastUpdateTime   = packageInfo.lastUpdateTime;
                //他のアプリと共有する場合のLinuxID   //String sharedUserId   = packageInfo.sharedUserId;
                Date lastUpdateTime = new Date(packageInfo.lastUpdateTime);
                addTextView(peerView, lastUpdateTime.toString());
                //Log.e("lastUpdateTime", lastUpdateTime.toString());
                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy'年'MM'月'dd'日'kk'時'mm'分'ss'秒'");
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(getTag(), "NameNotFoundException StackTrace", e);
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        //Android onSignalStrengthsChanged LTE measurement
        //http://www.truiton.com/2014/08/android-onsignalstrengthschanged-lte-strength-measurement/
        try {
            Method[] methods = android.telephony.SignalStrength.class.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getLteSignalStrength")
                    //|| method.getName().equals("getLteRsrp")
                    //|| method.getName().equals("getLteRsrq")
                    //|| method.getName().equals("getLteRssnr")
                    //|| method.getName().equals("getLteCqi")
                ) {
                    getLteSignalStrength = method;
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    // (4)センサー値の反映
    float lastPitch = 0;
    float setPitch = 0;
    float kP = 17.0F;
    float kI = 1.5F;
    float kD = 2.0F;
    float pwmW = 300F;
    float kPN = 1F;
    int servoCenter = 1500;
    float gPowerI = 0;
    boolean pidEnable = false;
    String[] pidParams = {"pid", "c", "p", "i", "d"};

    ArrayList<Float> mDirection = new ArrayList<>();
    ArrayList<Float> mPitch = new ArrayList<>();
    ArrayList<Float> mRoll = new ArrayList<>();
    ArrayList<Float> lst;
    int sampleCount = 9;//サンプリング数
    int sampleNum = 5;//サンプリングした値の使用値のインデックス
    String pwmStr;
    String pidViewStr;

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                break;
            //case  Sensor.TYPE_GYROSCOPE:
                //gyroscopeValues = event.values.clone();
        }
        //加速度センサーでの移動距離推定  http://makeunuseful.blog.so-net.ne.jp/2014-01-12　無理。
        if ( magneticValues != null && accelerometerValues != null) {// && gyroscopeValues != null) {
            long cTimeMillis = System.currentTimeMillis();
            //event.timestampはナノ秒とのこと。
            //0.1秒間隔で GPS, Sensor データ送信
            if (cTimeMillis > lastSensorUpTimeMillis + 100L ) {
                long defMills = cTimeMillis - lastSensorUpTimeMillis;
                lastSensorUpTimeMillis = cTimeMillis;
                float[] inR = new float[16];
                float[] outR = new float[16];
                float[] I = new float[16];
                SensorManager.getRotationMatrix(inR, I, accelerometerValues, magneticValues);
                //Activityの表示が縦固定の場合。横向きになる場合、修正が必要です　http://techbooster.jpn.org/andriod/ui/443/
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
                SensorManager.getOrientation(outR, orientationValues);
                // radianToDegree(orientationValues[0]) //Z軸方向,azmuth
                float pitch = (float) (Math.toDegrees(orientationValues[1]));//横向き補正 * -1); //X軸方向,pitch
                float roll = (float) (Math.toDegrees(orientationValues[2]));//横向き補正 + 90); //Y軸方向,roll
                //http://techbooster.jpn.org/andriod/ui/443/ センサを使ってAndroid端末の傾きを知る
                float direction = (float) Math.toDegrees(orientationValues[0]) + geoMag;
                //180～360°が　0～ -180° なので0~360°に補正 http://wp.developapp.net/?p=3394
                if ( direction < 0 ) {
                    direction = 360 + direction;
                }
                //磁気センサーにかけるフィルタクラスのメモ メディアンフィルタとローパスフィルタ
                //http://qiita.com/R_28/items/9e89a647671abe393324
                //サンプリング数の追加
                mDirection.add(direction);
                mPitch.add(pitch);
                mRoll.add(roll);

                //必要なサンプリング数に達したら
                if ( mDirection.size() == sampleCount ) {
                    //メディアンフィルタ(サンプリング数をソートして中央値を使用)かけて値を取得
                    //その値にさらにローパスフィルタをかける
                    //lst = new ArrayList<> mDirection.clone(); //未チェックキャストの警告について https://teratail.com/questions/620
                    lst = new ArrayList<>(mDirection);
                    Collections.sort(lst);
                    direction =( direction * 0.9f ) + lst.get(sampleNum) * 0.1f;

                    //lst = (ArrayList<Float>) mPitch.clone();
                    lst = new ArrayList<>(mPitch);
                    Collections.sort(lst);
                    pitch = ( pitch * 0.9f ) + lst.get(sampleNum) * 0.1f;

                    //lst = (ArrayList<Float>) mRoll.clone();
                    lst = new ArrayList<>(mRoll);
                    Collections.sort(lst);
                    roll = ( roll * 0.9f ) + lst.get(sampleNum) * 0.1f;
                    //lst.clear();

                    //一番最初の値を削除
                    mDirection.remove(0);
                    mPitch.remove(0);
                    mRoll.remove(0);
                }
                //Android センサー (1) 光センサーと加速度センサー http://blog.goo.ne.jp/marunomarunogoo/e/91687f6fac91a184ae078f743b9262c8
                //rad/s to deg/s   rad * (360/2PI) = deg（degree per second、°/秒）http://ednjapan.com/edn/articles/1406/09/news014.html
                //将来的参考　Androidの加速度センサー(Accelerometer)から重力の影響を省いて値を取得する http://tomoima525.hatenablog.com/entry/2014/01/13/152559
                float pitchGyro = (pitch - lastPitch) / ((float) defMills / 1000F);
                lastPitch = pitch;
                //以下　なぜか取得できず <-　安いスマフォはジャイロセンサーがないぞ。
                // 　(float) (Math.toDegrees(gyroscopeValues[1]));

                // 小数点表記の多国語対応 http://itinfo.main.jp/tan/?p=92
                String tRota = String.format(Locale.getDefault(), "%3.0f", direction);
                String tPitch = String.format(Locale.getDefault(), "%3.1f", pitch);//String.valueOf(pitch);
                String tRoll = String.format(Locale.getDefault(), "%3.0f", roll);//String.valueOf(roll);
                String tPitchGyro = String.format(Locale.getDefault(), "%3.0f", pitchGyro);
                String sensorViewStr = "Direc: " + tRota + ",Pitch: " + tPitch + ",Roll: " + tRoll + ",Gy: " + tPitchGyro + "\n" + gpsStr;
                sensorView.setText(sensorViewStr);
                //BT受信文字列 text表示D
                if (!btMsgReceived.equals("") && btMsgReceived.length() > 4) {
                    //BTレスポンスが同じ時もあるので前回レスポンスと比較しなくてOK
                    //if ( !lastBtMsgReceived.equals(btMsgReceived) ) {
                        //lastBtMsgReceived = btMsgReceived;
                        btMsgReceived = btMsgReceived.replaceAll("\r\n|\r|\n", ""); //改行削除
                        String btHead = btMsgReceived.substring(0, 4);
                        if (!btHead.equals("RPS:") && !btHead.equals("BAT:")) {
                            addTextView(btView, btMsgReceived);
                        }
                    //}
                }
                //Bt接続していない時、PeerからのBT接続指令
                if (lastBtUpTimeMillis == 0 && peerRes.equals("btConnect")) {
                    addTextView(btView, "\nWeb BT connect.");
                    peerRes = "";
                    btConnect();
                }
                //カメラスイッチ切り替え
                if (_msLocal != null && peerRes.equals("switchCamera")) {
                    addTextView(btView, "\n switchCamera.");
                    peerRes = "";
                    Boolean result = _msLocal.switchCamera();
                    if (result) {
                        //Success
                        Log.e(getTag(), "switchCamera Success.");
                        btn4.setEnabled(true); // カメラ切り替えボタンの許可
                    } else {
                        //Failed
                        Log.e(getTag(), "switchCamera Failed.");
                    }
                }
                //PID 制御
                if (!pidEnable && peerRes.equals("startPID")) {
                    pidEnable = true;
                    peerRes = "";
                    gPowerI = 0;
                    setPitch = pitch;
                }
                if (pidEnable) {
                    String btStr = pidAna(pitch, pitchGyro);
                    peerRes = "BTC:" + btStr + "1500m";// = 15001500m
                    //Web PID制御
                    if (peerRes.equals("stopPID")) {
                        pidEnable = false;
                        peerRes = "";
                    }
                }

                //WebからPID定数設定
                if (!peerRes.equals("") && peerRes.substring(0, 4).equals("pid,")) {
                    //文字列を分割する(split) http://www.javadrive.jp/start/string_class/index5.html
                    pidParams = peerRes.split(",", 0);
                    //文字列から数値への変換  http://www.javadrive.jp/start/wrapper_class/index5.html
                    servoCenter = Integer.parseInt(pidParams[1]);
                    kP = Float.parseFloat(pidParams[2]);
                    kI = Float.parseFloat(pidParams[3]);
                    kD = Float.parseFloat(pidParams[4]);
                    pwmW = Float.parseFloat(pidParams[5]);
                    kPN = Float.parseFloat(pidParams[6]);
                    peerRes = "";
                }

                //Bt接続中　Peerからの文字列、あればBT送信 (Bluetoothコマンドに限定)
                if (lastBtUpTimeMillis != 0 && !peerRes.equals("") && peerRes.length() > 5 && peerRes.substring(0, 4).equals("BTC:")) {
                    btMsgReceived = "Peer[" + peerRes + "]";//Btに送信したことをWebで表示するため
                    //Peerからの司令文字列をBT文字列に変換して送信
                    String btCommand = peerRes.substring(4) + "\r\n";
                    peerRes = "";
                    blue.sendData(btCommand.getBytes(), btCommand.length());
                    //Log.d("main", "Bt Send command : " + btCommand);
                    lastBtUpTimeMillis = cTimeMillis;
                }

                if ( _bConnecting ) {
                    String jData = "{'no':" + tNum + ",'lat':" + tLat + ",'lng':" + tLng + ",'alti':" + tAltitude
                            + ",'rota':" + tRota + ",'time':" + cTimeMillis + ",'pitch':" + tPitch + ",'roll':" + tRoll
                            + ",'accuracy':" + tAccuracy + ",'batLevel':" + batLevel + ",'batTemp':" + batTemp + ",'lte':" + lteSignalStrength
                            + ",'btr':" + '"' + btMsgReceived + '"';
                    if (pidEnable) {
                        jData += ",'pitchGyro':" + tPitchGyro + ",'pwm':" + '"' +  pwmStr + '"' + ",'pid':" + '"' + pidViewStr + '"';
                    }
                    jData += "}\n"; //改行で配列にする
                    jData = jData.replaceAll("'", "\""); //シングルクオートを置換

                    //peerDataConnを毎回チェックして、peerDataConnなければ再接続
                    boolean Result = true;
                    if ( peerConnErrCount < 10 ) { //だいたい12回くらいエラーを出すとアプリ停止するので10回まで再試行
                        Result = _data.send(jData);
                        btMsgReceived = "";
               
                    } else { //Peer再接続処理
                        Log.e(getTag(), "peerDataConn count err over 10.");
                        //_bConnecting = false;
                        peerConnErrCount = 0;
                        //ToDo: Peer再接続
                        //peerDestroy();
                        //[Q&A] peer.disconnect と peer.destroy https://groups.google.com/forum/#!topic/skywayjs/_45s2_fGLso
                        //[Q&A] AndroidSDK Peerのdisconnectとdestroyの違いについて https://groups.google.com/forum/#!topic/skywayjs/N4KmEQyBlTk
                        //[Q&A] peer.open後のネットワーク切断とスリープでの挙動について　https://groups.google.com/forum/#!topic/skywayjs/1P0t641t4zY
                    }
                    if (!Result) {
                        peerConnErrCount++;//Peer送信失敗回数カウントアップ
                        Log.e(getTag(), "peerDataConn err count : " + peerConnErrCount);
                    }
                }
            }
            //根拠は分からないが、PeerDestoyから1秒立ってから再接続で成功する。
            //Web再読込時はPeerID作成しないほうが良いのでは？　30秒にする
            if ( lastPeerCloseTimeMillis != 0 && cTimeMillis > lastPeerCloseTimeMillis + 30000L ) {
                Log.e(getTag(), "Peer Reconnect. Peer Close から　30秒経過. 再接続.");
                peerGetID();
                lastPeerCloseTimeMillis = 0;
            }

            //1秒以上BT通信の要求がない場合サーボデータ読み込み
            // (Arduinoは1.1秒以上BTシリアル通信がなければサーボをニュートラル1500に戻すため)
            if ( lastBtUpTimeMillis != 0 && cTimeMillis > lastBtUpTimeMillis + 1000L ) {
                String btCommand = "r\r\n";
                //bt送信
                if ( blue.sendData(btCommand.getBytes(), btCommand.length()) ) {
                    lastBtUpTimeMillis = cTimeMillis;
                } else {//BT送信エラー
                    //BT再接続
                    btMsgReceived = "blue.sendData Err.";
                    //Bt切断
                    btDisConnect();
                }
            }
            //TODO:　Android 自動運転 (2017.4.28現在、Webで実装中）
            // https://developers.google.com/maps/documentation/android-api/utility/
            //TODO:　スマフォのライトをリモートON
        }
    }
    //PID計算テスト
    private String pidAna(float pitch, float pitchGyro) {
        //もう一つの倒立振子（デジタル版） http://www.instructables.com/id/Another-Easier-Inverted-Pendulum-in-Japanese/step5/%E3%83%97%E3%83%AD%E3%82%B0%E3%83%A9%E3%83%A0%E3%81%AE%E6%9B%B8%E3%81%8D%E8%BE%BC%E3%81%BF/
        // invertedRobot_v20d_noTimer.ino https://gist.github.com/i386koba/5bc955d2ff768a005139b231c914732f
        // How to Build a Self-Balancing Autonomous Arduino Bot http://makezine.com/projects/arduroller-self-balancing-robot/
        // 倒立振子を作ってみた。http://qiita.com/Qikoro/items/d24057b434c44fcdf74e

        float gPowerP = (pitch - setPitch) / 90F;    // P成分：傾き-90～90度 → -1～1
        gPowerI += gPowerP;     // I成分：傾きの積算。
        float gPowerD = pitchGyro / 250F;   // D成分：角速度-250～250dps → -1～1

        // この数字は試行錯誤で調整。gPowerP * 17.0 + gPowerI *  1.5 + gPowerD *  2.0;
        float power = gPowerP * kP + gPowerI * kI + gPowerD * kD;
        power = kPN * (Math.max(-1, Math.min(1, power))); // → -1～1
        // powerをモーター駆動PWMに変換。0～1 → V_MIN～V_MAX
        //float pwmMAX = servoCenter + 200F;
        //float pwmMIN = servoCenter - 200F;
        //int servo = (int) ((pwmMAX - pwmMIN) * Math.abs(power) + pwmMIN);
        int servo =  servoCenter + (int) (power * pwmW);
        pwmStr = String.format(Locale.getDefault(), "%3.0f", power * pwmW) + "," + String.format(Locale.getDefault(), "%1.2f", pitch - setPitch);
        // デバッグ用。
        pidViewStr = "Pow:" + String.format(Locale.getDefault(), "%1.2f", power)
                + "=gP:" + String.format(Locale.getDefault(), "%1.2f", gPowerP) //+ "*" + pidParams[2]
                + "+gI:" + String.format(Locale.getDefault(), "%1.2f", gPowerI) //+ "*" + pidParams[3]
                + "+gD" + String.format(Locale.getDefault(), "%1.2f", gPowerD)  //+ "*" + pidParams[4];
                + ",setPitch:" + String.format(Locale.getDefault(), "%3.1f", setPitch);
        pidView.setText(pidViewStr);

        // 倒れたらモーター停止。
        if (80 < Math.abs(pitch)) {
            servo = servoCenter;
            gPowerI = 0;
        }
        return String.format(Locale.getDefault(), "%04d", servo);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(getTag(), "onActivityResult: requestCode:" + requestCode + ", resultCode:" + resultCode);
        switch (requestCode) {
            case REQUEST_ACCOUNT_CHOOSER:
                if ( resultCode == RESULT_OK && data != null ) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.e(getTag(), "ACCOUNT_CHOOSER: Account Name:" + accountName);
                    if ( accountName != null ) {
                        mCredential.setSelectedAccountName(accountName);
                        Log.e(getTag(), "Google Login:" + accountName);
                        //addTextView(peerView, "Google Login:" + accountName);
                        activityResult = "Google Login:" + accountName;

                    }
                } else {
                    // エラー処理
                    Log.e(getTag(), "Google Login:miss");
                    //addTextView(peerView, "アカウント名を取得失敗");
                    activityResult = "";
                }
                peerGetID();
                break;
            case REQUEST_AUTHORIZATION_FROM_DRIVE:
                if ( resultCode == RESULT_OK ) {
                    Log.e(getTag(),  "REQUEST_AUTHORIZATION_FROM_DRIVE : Login success.");

                    //addTextView(peerView, " : Login success.");
                    activityResult += " : Login success.";

                } else {
                    // エラー処理
                    Log.e(getTag(), "REQUEST_AUTHORIZATION_FROM_DRIVE : Login Miss!.");
                    //addTextView(peerView, " 認証に失敗");
                    activityResult = "";
                }
                break;
            default:
                // エラー処理
                Log.e(getTag(), "onActivityResult requestCode :" + requestCode + " is default.");
        }

    }
    // アクティビティがポーズ状態(バックグラウンド）になったら、
    @Override
    protected void onPause() {
        super.onPause();
    }

    //アクティビティのライフサイクル http://www.javadrive.jp/android/activity/index2.html
    @Override
    public void onDestroy() {
        super.onDestroy();
        //電話の状態をモニタリング http://computerexpert.web.fc2.com/android/recipe4-2.html
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        //サーボOff 送信
        String val = "d\r\n";
        blue.sendData(val.getBytes(), val.length());
        blue.close(); // bt接続を切
        //受信を停止
        unregisterReceiver(batteryReceiver);
        sensorManager.unregisterListener(this);
        disConnectGooglePlayServices();
        destroyPeer();
        Log.e(getTag(), "Finish app.");
        finish(); // アプリを終了する
    }

    private void destroyPeer()  {

        if (null != _data) {
            _data.close();
            _bConnecting = false;
        }

        if (null != _data)  {
            _data.on(DataConnection.DataEventEnum.OPEN, null);
            _data.on(DataConnection.DataEventEnum.DATA, null);
            _data.on(DataConnection.DataEventEnum.CLOSE, null);
            _data.on(DataConnection.DataEventEnum.ERROR, null);
            _data = null;
        }

        if (null != _media) {
            _media.close();
            //_bCalling = false;
        }

//リモート映像（未使用）
//        if (null != _msRemote)  {
//            Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
//            canvas.removeSrc(_msRemote, 0);
//        https://groups.google.com/forum/#!msg/skywayjs/B4os4NRRltY/tCsGQARTAQAJ
//            _msRemote.close();
//            _msRemote = null;
//        }

        if (null != _msLocal) {
            Canvas canvas = (Canvas) findViewById(R.id.svSecondary);
            canvas.removeSrc(_msLocal, 0);
            //https://groups.google.com/forum/#!msg/skywayjs/B4os4NRRltY/tCsGQARTAQAJ
            if (null != _msLocal) { //nullチェックを直前に追加
                _msLocal.close();
                _msLocal = null;
            }
        }

        if (null != _media)  {
            if (_media.isOpen)   {
                _media.close();
            }
            _media.on(MediaConnection.MediaEventEnum.STREAM, null);
            _media.on(MediaConnection.MediaEventEnum.CLOSE, null);
            _media.on(MediaConnection.MediaEventEnum.ERROR, null);
            _media = null;
        }

        Navigator.terminate();

        if (null != _peer) {
            _peer.on(Peer.PeerEventEnum.OPEN, null);
            _peer.on(Peer.PeerEventEnum.CONNECTION, null);
            _peer.on(Peer.PeerEventEnum.CALL, null);
            _peer.on(Peer.PeerEventEnum.CLOSE, null);
            _peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
            _peer.on(Peer.PeerEventEnum.ERROR, null);

            if (!_peer.isDisconnected)  {
                _peer.disconnect();
            }

            if (!_peer.isDestroyed)  {
                _peer.destroy();
            }
            _peer = null;
        }
        Log.e(getTag(), "peer : destroy");
    }
    //戻るボタンを押した時に戻るか確認するダイアログを出す。
    //http://dorodoro.info/tip/%E6%88%BB%E3%82%8B%E3%83%9C%E3%82%BF%E3%83%B3%E3%82%92%E6%8A%BC%E3%81%97%E3%81%9F%E6%99%82%E3%81%AB%E6%88%BB%E3%82%8B%E3%81%8B%E7%A2%BA%E8%AA%8D%E3%81%99%E3%82%8B%E3%83%80%E3%82%A4%E3%82%A2%E3%83%AD/
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //http://blog.e0418.net/2012/dec/12/android_tricks1/
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            //戻る,電源ボタンボタンを押した時
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Droidrone");
                alertDialogBuilder.setMessage("終了します？");
                alertDialogBuilder.setPositiveButton("終了",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                alertDialogBuilder.setNegativeButton("キャンセル",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                alertDialogBuilder.setCancelable(true);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    //[Q&A]1つのPeerに複数のコネクションを持たせた時、コネクションのcloseを行うとエラーが発生することがある
    //1つのPeerに複数のコネクションを保持できるものと見受けられます https://groups.google.com/forum/#!topic/skywayjs/a0_S7qTZuPE
    private void peerGetID() {
        if (activityResult.equals("")) {
            addTextView(peerView, "Google login err!");
            return;
        }
        PeerOption peerOptions = new PeerOption();
        peerOptions.key = "30fa6fbf-0cce-45c1-9ef6-2b6191881109";
        peerOptions.domain = "www.cirlution.com";
        peerOptions.debug = Peer.DebugLevelEnum.ALL_LOGS;
        //Data connection Peer
        _peer = new Peer(getApplicationContext(), peerOptions);
        //MyID　生成  // !!!: Event/Open
        _peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                //PeerEvent/OPEN
                if (object instanceof String) {
                    _id = (String) object;
                    addTextView(peerView, activityResult);
                    addTextView(peerView, " My peer ID:" + _id);
                    //PeerIDをGoogleDriveに保存
                    if (mDrive == null) {
                        HttpTransport transport = AndroidHttp.newCompatibleTransport();
                        GsonFactory factory = new GsonFactory();
                        mDrive = new Drive.Builder(transport, factory, mCredential).build();
                    }
                    new setDrivePeerIdTask(peerView).execute(mDrive, _id, this);

                    //AsyncTask非同期処理後のコールバック機能
                    //https://qiita.com/a_nishimura/items/1548e02b96bebd0d43e4
                    //startActivityForResult(mIntent, REQUEST_AUTHORIZATION_FROM_DRIVE);
                }
            }
        });
        // !!!: Event/Error
        _peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                //DataEvent/ERROR
                PeerError error = (PeerError) object;
                Log.e(getTag(), "peer: " + error.message);
                addTextView(peerView, "err:" + error.message);
            }
        });

        // !!!: Event Data Connection
        _peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                //PeerEvent/CONNECTION
                if ((object instanceof DataConnection)) {
                    _data = (DataConnection) object;
                    _data.on(DataConnection.DataEventEnum.OPEN, new OnCallback() {
                        @Override
                        public void onCallback(Object object) {
                            //DataEvent/OPEN
                            String destPeerId = _data.peer;
                            addTextView(peerView, "DestPeerID:" + destPeerId);
                            Log.e(getTag(), "DataConnDestID:" + destPeerId);
                            //初回Androidデータを送る
                            //Androidのシステム情報を取得する http://techbooster.jpn.org/andriod/device/1330/
                            boolean bResult = _data.send(Build.BRAND + "/" + Build.ID + "\n");
                            if (!bResult) {
                                //送信失敗
                                Log.e(getTag(), "peer bResult Build.BRAND false.");
                            } else {
                                //送信成功 ログ送信スタート
                                _bConnecting = true;
                                peerMediaConnect(destPeerId);
                                //Toast.makeText(getApplicationContext(), "Send Android Data.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    _data.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
                        public void onCallback(Object object) {
                            if (object instanceof String) {
                                peerRes = (String) object;
                                //Log.e("peerDataConn", "DataConnection.DataEventEnum.DATA");
                            }
                        }
                    });
                    // !!!: Event/Error
                    _data.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
                        @Override
                        public void onCallback(Object object) {
                            _bConnecting = false;
                            //DataEvent/ERROR
                            PeerError error = (PeerError) object;
                            Log.e(getTag(), "peerDataConn [Error]" + error.message);
                            //String strMessage
                            addTextView(peerView, "peerDataConn Err:" + error.message);
                        }
                    });
                    // Event/Close
                    _data.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
                        @Override
                        public void onCallback(Object object) {
                            _bConnecting = false;
                            Log.e(getTag(), "peerDataConn [Close]");
                            lastPeerCloseTimeMillis = System.currentTimeMillis();
                        }
                    });
                }
            }
        });
//
//        //応答(未使用）
//        peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
//            @Override
//            public void onCallback(Object object) {
//                //  PeerEvent/CONNECTION
//                if ((object instanceof MediaConnection)) {
//                    MediaConnection webMedia = (MediaConnection) object;
//                    webMedia.answer(msBack);
//                    addTextView(peerView, "Call from:" + webMedia.peer);
//                    Log.e("peer", "Call Media." + webMedia.peer);
//                }
//            }
//        });
    }

    // WebRTC media Calling
    public void peerMediaConnect(String destPeerId) {
        if ( _msLocal == null ) {
//            msBack.close();
//            canvasBackCamera.removeSrc(msBack, 0);
//            //canvasFrontCamera.removeSrc(msFront, 1);
//            Log.e("canvasBackCamera", "canvasBackCamera.remove");
//            //addTextView(peerView, "canvasBackCamera.remove");

            //Android Stream 取得
            Navigator.initialize(_peer);
            //Android Stream 準備
            MediaConstraints constraints = new MediaConstraints();
            //constraints.audioFlag = false;
            constraints.cameraPosition = MediaConstraints.CameraPositionEnum.BACK;
            //maxWidth int        横ピクセル上限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは640となります。
            //minWidth int        横ピクセル下限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは0となります。
            //maxHeight int        縦ピクセル上限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは640となります。
            //minHeight int        縦ピクセル下限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは0となります。
            //maxFrameRate int        フレームレート上限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは10となります。
            //minFrameRate int        フレームレート下限を設定します0を指定すると WebRTCエンジン依存となります。デフォルトは0となります。
            _msLocal = Navigator.getUserMedia(constraints);
            //private Canvas canvasFrontCamera;
            Canvas canvas = (Canvas) findViewById(R.id.svSecondary);
            try {
                canvas.addSrc(_msLocal, 0);
            } catch (NullPointerException e) {
                Log.e(getTag(), "canvasBackCamera [Error]", e);
            }
        }

        CallOption option = new CallOption();
        _media = _peer.call(destPeerId, _msLocal, option);
        // Media接続成功　
        if (_media.isOpen) {
            //Toast.makeText(getApplicationContext(), "Calling to " + media.peer, Toast.LENGTH_LONG).show();
            addTextView(peerView, "Call to:" + _media.peer);
            Log.e(getTag(), "peer Call to: " + _media.peer);
            //_bCalling = true;
        }
        // err
        _media.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                // DataEvent/ERROR
                PeerError error = (PeerError) object;
                Log.e(getTag(), "Calling [Error]" + error.message);
                //String strMessage
                addTextView(peerView, "Calling Err:" + error.message);
                //_bCalling = false;
            }
        });
        // Event Close
        _media.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
            public void onCallback(Object object) {
                Log.e(getTag(), "MediaConnection [Close]");
                addTextView(peerView, "peerMediaConn: [Close]");
                lastPeerCloseTimeMillis = System.currentTimeMillis();
                //_bCalling = false;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        float lat = (float) location.getLatitude();
        float lng = (float) location.getLongitude();
        float alt = (float) location.getAltitude();
        float acc = location.getAccuracy();
        //float spd = location.getSpeed();
        //float ber = location.getBearing();
        long gpsTime = location.getTime();
        //磁気偏角を計算
        GeomagneticField geomagneticField = new GeomagneticField(lat, lng, alt, gpsTime);
        // 求まった方位角をラジアンから度に変換する
        // 磁補正 : geoMag
        geoMag = geomagneticField.getDeclination();
        tNum = String.valueOf(++num);
        tLat = String.valueOf(lat);
        tLng = String.valueOf(lng);
        tAltitude = String.format(Locale.getDefault(), "%3.0f", alt);
        tAccuracy = String.format(Locale.getDefault(), "%3.0f", acc);
        //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        //String tTime = df.format(gpsTime);
        //String tTime = DateFormat.getDateTimeInstance().format(gpsTime);
        //TextView 表示用
        gpsStr = "Lat:" + tLat + ",Lng:" + tLng + ",Alt:" + tAltitude + ",Acc:" + tAccuracy;
    }
    //http://techbooster.jpn.org/andriod/device/1551/
    public PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        //https://developer.android.com/reference/android/telephony/PhoneStateListener.html#onSignalStrengthsChanged(android.telephony.SignalStrength)
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            //https://developer.android.com/reference/android/telephony/SignalStrength.html
            //Log.e("onSignalStrengthChanged",  "getCdmaDbm(): " +  String.valueOf(signalStrength.getLteCqi()) );//cdma
            //Log.e("onSignalStrengthChanged",  "getEvdoDbm(): " +  String.valueOf(signalStrength.getEvdoDbm()) );//CDMA2000
            //Log.e("ReflectionUtils", ReflectionUtils.dumpClass(SignalStrength.class, signalStrength));
            //Log.e("onSignalStrengthChanged",  "toString(): " +  String.valueOf(signalStrength.toString()) );

            //Android onSignalStrengthsChanged LTE measurement
            //http://www.truiton.com/2014/08/android-onsignalstrengthschanged-lte-strength-measurement/
            try {
                lteSignalStrength = getLteSignalStrength.invoke(signalStrength).toString();
                //Log.e("onSignalStrengthChanged", "getLteSignalStrength: " + lteSignalStrength );
                //Java7では複数例外catchを簡単に記述することが出来ます。 http://d.hatena.ne.jp/zephiransas/20110720/1311151021
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException |SecurityException e) {
                e.printStackTrace();
            }
            //Added in API level 17 CellSignalStrengthLte  https://developer.android.com/reference/android/telephony/CellSignalStrengthLte.html
        }
    };
    //ブロードキャストレシーバ　バッテリー情報の受信機
    public BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                // 電池残量の最大値  batScale = intent.getIntExtra("scale", 0);
                // 電池残量
                batLevel = String.valueOf(intent.getIntExtra("level", 0));
                // 温度
                batTemp = String.valueOf(intent.getIntExtra("temperature", 0));
            }
        }
    };

    //Activity#runOnUiThread(Runnable)の実装を読む注意点 http://visible-true.blogspot.jp/2011/11/activityrunonuithreadrunnable.html
    public void addTextView(final TextView textView , final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Android TextView : “Do not concatenate text displayed with setText” http://stackoverflow.com/questions/33164886/android-textview-do-not-concatenate-text-displayed-with-settext
                //追加した行を一番上に表示させる。
                String addMsg = msg + "\n" + textView.getText();
                textView.setText(addMsg);
                // textView.append(addMsg);
                //EditText/TextViewで文末まで自動スクロールしたい http://shironiji.sblo.jp/article/43573373.html
                //textView.requestFocus(); // 最初は別のEditTextにフォーカスがあるため目的のEditTextに移動
                //textView.setSelection(textView.getText().length());
            }
        });
    }

    //ブルートゥースの接続
    // BLE対応　http://qiita.com/miyatay/items/3f43bc8348b0e1914214
    //　Bluetooth LE (5) Android 4.3 で Bluetooth LE 機器を使う　http://blog.fenrir-inc.com/jp/2013/10/bluetooth-le-android.html

    public void btConnect() {
        //TODO:　リスト表示　Or　起動時自動接続
        Log.e(getTag(), "BT 接続start.");
        addTextView(btView, "デバイス検索開始.");
        //接続可能な Android内のBluetooth を検出
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //BT有効切り替え
        //    enable/disableの強制指定ではなく、ユーザにダイアログ表示して選択させる実装が望ましいが、
        // 開始ボタンはユーザーの選択なので、今回は表示しない
        //    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        bluetoothAdapter.enable();
//        try {
//            Thread.sleep(500); //500ミリ秒Sleepする
//        } catch (InterruptedException e) {
//            Log.e("BT", "接続待機エラー.", e);
//        }
        //TODO:　BT接続一回目に接続履歴リストにデバイスが表示されない　（待機してもダメだった）
        //BluetoothAdapterから、接続履歴のあるデバイスの情報を取得
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        //ArrayList<型> 変数名 = new ArrayList<型>();
        final List<String> devNameArray = new ArrayList<>();
        final List<BluetoothDevice> devArray = new ArrayList<>();
        //devNameArray.add("none");
        //devArray.add(null);
        // foreach（拡張for文）
        for ( BluetoothDevice device : pairedDevices ) {
            devNameArray.add(device.getName());
            devArray.add(device);
        }
        //String[] array = (String[]) list.toArray(new String[list.size()]);
        final String[] devNames = devNameArray.toArray(new String[devNameArray.size()]);
        final int[] select_device = {0};
        //radio btn alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("BT Connect List.");
        builder.setSingleChoiceItems(devNames, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int witch) {
                select_device[0] = witch;
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.e(getTag(), "SingleChoiceItems Cancel.");
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String deviceName = devNames[select_device[0]];
                btMsgReceived = "BT Try connect:" + deviceName;
                addTextView(btView, btMsgReceived);
                btn1.setEnabled(false); // 接続ボタンの禁止
                btn2.setEnabled(true); // 切断ボタンの許可
                btn3.setEnabled(false); // Debugボタン禁止
                BluetoothDevice bluetoothDevice = devArray.get(select_device[0]);
                BluetoothSocket bluetoothSock = null;
                if (bluetoothDevice != null) {
                    Log.e(getTag(), "bluetoothSock create Socket.");
                    try {
                        bluetoothSock = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                    } catch (IOException e) {
                        Log.e(getTag(), "bluetoothSock create socket failed.", e);
                        btDisConnect();
                    }
                }
                if (bluetoothSock != null) {
                    try {
                        bluetoothSock.connect();
                        if ( blue.connect(bluetoothSock) ) { // もし、接続が成功なら
                            //addTextView(btView,"BtConnected :" + );
                            btMsgReceived = "BtConnected :" + deviceName;
                            Log.e(getTag(), "BT: " +deviceName + " 接続成功。");
                            //サーボアタッチ
                            String val = "a\r\n";
                            blue.sendData(val.getBytes(), val.length());
                            lastBtUpTimeMillis = System.currentTimeMillis();
                        } else { // 接続が失敗なら  //addTextView(btView, "BT Failed: " + DEVICE_NAME);
                            btDisConnect();
                            btMsgReceived = "BT Failed: " + deviceName;
                            addTextView(btView, btMsgReceived);
                            Log.e(getTag(), "BT: " + deviceName + " 接続失敗。");
                            btDisConnect();
                        }
                    }  catch (IOException e) {
                        Log.e(getTag(), "bluetoothSock connect Socket err:", e);
                        btMsgReceived = "can not connect: " + deviceName;
                        addTextView(btView, btMsgReceived);
                        Log.e(getTag(), "BT: "+ deviceName + " 接続不可能。");
                        btDisConnect();
                    }
                    addTextView(btView, btMsgReceived);
                }
                //Bluetoothに接続可能なデバイスを検出し、リストにして並べる　http://techbooster.jpn.org/andriod/device/5535/
                //検出されたデバイスからのブロードキャストを受ける http://blog.zaq.ne.jp/oboe2uran/article/746/
                /*
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(DeviceFoundReceiver, filter);
                //接続可能なデバイスを検出
                if( bluetoothAdapter.isDiscovering() ){
                    //検索中の場合は検出をキャンセルする
                    bluetoothAdapter.cancelDiscovery();
                }
                //デバイスを検索する
                //一定時間の間検出を行う
                bluetoothAdapter.startDiscovery();
                */
            }
        });
        builder.show();
    }

    // ブルートゥースの切断
    public void btDisConnect() {
        blue.close(); // 切断する
        btn1.setEnabled(true); // 接続ボタンの許可
        btn2.setEnabled(false); // 切断ボタンの禁止
        lastBtUpTimeMillis = 0;
        //addTextView(btView, "BT Disconnected.");
        btMsgReceived = "BT Disconnected.";
        addTextView(btView, btMsgReceived);
        //Log.e("BT", btMsgReceived);
    }

    //* Bluetooth.javaで、データ受信時に呼び出されるメソッド 受信スレッドとして実行されるので、直接GUIを操作できない
    public void lineReceived(String line) {
        if (line != null && line.length() > 0 ) {    // データがあれば、メッセージに追加する
            msgReceived += line;
        }
        String t = msgReceived.substring(msgReceived.length() - 1);    // 最後の文字を得る
        if ((t.equals("\r")) || (t.equals("\n"))) {    // 終端記号か？
            msgReceived = msgReceived.trim();        // 余計な記号を削除
            if (msgReceived.length() > 0) {    // データがあるか？
                //btMsgReceived += "Received [" + msgReceived + "]";
                btMsgReceived = msgReceived;
                //Log.e("BT", "受信" + btMsgReceived);
                msgReceived = "";   // メッセージをエンプティにする
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    //GoogleIDの保存
//アプリケーションの設定を保存する / GETTING STARTED
//http://techbooster.org/android/application/11103/
//SharedPreferences.Editor
//http://developer.android.com/reference/android/content/SharedPreferences.Editor.html
//    private String getSavedAccountName() {
//        SharedPreferences prefs = getSharedPreferences("prefs_name", Context.MODE_PRIVATE);
//        return prefs.getString("key_account_name", "");
//    }
//
//    private void saveAccountName(final String accountName) {
//        SharedPreferences prefs = getSharedPreferences("prefs_name", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString("key_account_name", accountName);
//        //editor.commit();
//        editor.apply();
//    }
//    // https://developers.google.com/drive/v2/reference/files/get より
//    private static InputStream downloadFile(Drive service, File file) {
//        if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
//            try {
//                HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
//                return resp.getContent();
//            } catch (IOException e) {
//                // An error occurred.
//                Log.e("downloadFile", "StackTrace", e);
//                //e.printStackTrace();
//                return null;
//            }
//        } else {
//            // The file doesn't have any content stored on Drive.
//            return null;
//        }
//    }

    //https://blog.isao.co.jp/android%E3%81%AElogcat%E3%81%AEtag%E3%81%AB%E3%82%AF%E3%83%A9%E3%82%B9%E5%90%8D%E3%80%81%E3%83%A1%E3%82%BD%E3%83%83%E3%83%89%E5%90%8D%E3%80%81%E8%A1%8C%E7%95%AA%E5%8F%B7%E3%82%92%E8%A1%A8%E7%A4%BA/
    private static String getTag() {
        final StackTraceElement trace = Thread.currentThread().getStackTrace()[4];
        final String cla = trace.getClassName();
        Pattern pattern = Pattern.compile("[.]+");
        final String[] splitStr = pattern.split(cla);
        final String simpleClass = splitStr[splitStr.length - 1];
        final String mthd = trace.getMethodName();
        final int line = trace.getLineNumber();
        //final String tag = simpleClass + "# " + mthd + ": " + line;
        return simpleClass + "# " + mthd + ": " + line;
    }

}
