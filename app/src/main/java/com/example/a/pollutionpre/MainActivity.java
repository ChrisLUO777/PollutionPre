package com.example.a.pollutionpre;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private Button showinfo;
    private TextView tv_01;
    private Button restore;
    private Button sate;
    private Button norm;
    private Button start;
    private Button pause;
    private Wind nowwind=new Wind(0,10,-273.15);
    private List<GasPoint> ListStartPoint=new ArrayList<GasPoint>();
    private List<LatLng> ListSuffPoint=new ArrayList<LatLng>();

    private final Object lock = new Object();
    private boolean isPause = false;
    private boolean isFirstRun=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        initView();
        initMap();

    }

    private void initMap() {
        //获取地图控件引用
        mBaiduMap = mMapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        //默认显示普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //开启交通图
        //mBaiduMap.setTrafficEnabled(true);
        //开启热力图
        //mBaiduMap.setBaiduHeatMapEnabled(true);
        // 开启定位图层
        //mBaiduMap.setMyLocationEnabled(true);

        initLocation(39.90923,116.447428);
        getWeather("beijing");
        initStartPoints();
        tv_01 = (TextView)this.findViewById(R.id.product_tag);
        drawStartPoints(ListStartPoint);
        initSuffPoints();

    }
    public void initStartPoints(){
        ListStartPoint.clear();
        MySQLiteOpenHelper dbHelper = new MySQLiteOpenHelper(MainActivity.this,"gaspoint.db",null,1);
        //得到一个可写的数据库
        SQLiteDatabase db =dbHelper.getReadableDatabase();
        //参数1：表名
        //参数2：要想显示的列
        //参数3：where子句
        //参数4：where子句对应的条件值
        //参数5：分组方式
        //参数6：having条件
        //参数7：排序方式
        Cursor cursor = db.query("gaspoints", new String[]{"lat","lng","thick"}, null, null, null, null, null);
        while(cursor.moveToNext()){
            GasPoint inputgaspoint=new GasPoint(cursor.getDouble(cursor.getColumnIndex("lat")),
                    cursor.getDouble(cursor.getColumnIndex("lng")),
                    cursor.getInt(cursor.getColumnIndex("thick")));
            ListStartPoint.add(inputgaspoint);
        }
        //关闭数据库
        db.close();
    }

    public void drawStartPoints(List<GasPoint> Listpoint){
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();
        for(int i=0;i<Listpoint.size();i++) {
            //定义Maker坐标点
            LatLng point = new LatLng(Listpoint.get(i).getLat(), Listpoint.get(i).getLng());
            BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.redmarker);
            //构建MarkerOption，用于在地图上添加Marker
            OverlayOptions option = new MarkerOptions()
                    .position(point)
                    .icon(bitmap);
            options.add(option);
        }
        //在地图上添加Marker，并显示
        mBaiduMap.addOverlays(options);
    }

    public void initSuffPoints(){
        ListSuffPoint.clear();
        for(int i=0;i<ListStartPoint.size();i++){
            int thick=ListStartPoint.get(i).getThick()*5;
            for(int j=0;j<thick;j++){
                LatLng point=new LatLng(ListStartPoint.get(i).getLat(),ListStartPoint.get(i).getLng());
                ListSuffPoint.add(point);
            }
        }
    }
    public void drawSuffPoints(List<LatLng> Listpoint){
        //清空原有点
        mBaiduMap.clear();
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();
        for(int i=0;i<Listpoint.size();i++) {
            //定义Maker坐标点
            LatLng point = Listpoint.get(i);
            BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.greenmarker);
            //构建MarkerOption，用于在地图上添加Marker
            OverlayOptions option = new MarkerOptions()
                    .position(point)
                    .icon(bitmap);
            options.add(option);
        }

        //在地图上添加Marker，并显示
        mBaiduMap.addOverlays(options);
    }

    public void getWeather(final String city_str){
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HttpClient httpClient=new DefaultHttpClient();
                    HttpClientParams.setCookiePolicy(httpClient.getParams(), CookiePolicy.BROWSER_COMPATIBILITY);
                    String path = "https://free-api.heweather.com/s6/weather/now?location="
                            + city_str + "&key=a583d8b5e45645f09456c1190104d15a&lang=en";
                    HttpGet httpGet = new HttpGet(path);
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity entity = httpResponse.getEntity();
                    String result= EntityUtils.toString(entity,"utf-8");
                    JSONAnalysis(result);
                    //winddeg:0 winspd:36
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * JSON解析方法,将结果注入Wind实例中
     */
    protected void JSONAnalysis(String string) {
        try {
            JSONObject object = new JSONObject(string);
            JSONArray jsonArray = object.getJSONArray("HeWeather6");
            JSONObject ObjectInfo = jsonArray.getJSONObject(0).getJSONObject("now");
            int nowdeg = ObjectInfo.optInt("wind_deg");
            int nowspd = (int) ObjectInfo.optInt("wind_spd")*36/10;//转化为m/s
            double nowkel=(double)ObjectInfo.optInt("tmp")+273.15;
            nowwind.setDeg(nowdeg);
            nowwind.setSpd(nowspd);
            nowwind.setKel(nowkel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void update(List<LatLng> Listpoint,Wind wind){
        double zoomLevel = mBaiduMap.getMapStatus().zoom;
        int measurescale=measurescale(zoomLevel);
        //v=sqrt(8RT/(PI*M)) --Maxwell's Velocity distribution law
        double distance=Math.sqrt(8*8.314*nowwind.getKel()/(Math.PI*28));//28 is the gmol of CO
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        double xRealDpcm=displayMetrics.xdpi/2.54;
        double yRealDpcm=displayMetrics.ydpi/2.54;
        for(int i=0;i<Listpoint.size();i++) {
            LatLng point=Listpoint.get(i);
            double curlng = point.longitude;
            double curlat = point.latitude;
            double curx = 0;
            double cury = 0;
            double movedeg=2*Math.PI * Math.random();
            LatLng curlatlng = new LatLng(curlat, curlng);
            //latlng to xy
            Point curpoint = mBaiduMap.getProjection().toScreenLocation(curlatlng);
            curx = curpoint.x;
            cury = curpoint.y;
            curx = curx + distance * xRealDpcm * Math.cos(movedeg)/ measurescale + wind.getSpd() * xRealDpcm * Math.cos((wind.getDeg()-90) * Math.PI/ 180 ) * Math.random()/ measurescale ;
            cury = cury + distance * yRealDpcm * Math.sin(movedeg)/ measurescale + wind.getSpd() * yRealDpcm * Math.sin((wind.getDeg()-90) * Math.PI/ 180 ) * Math.random()/ measurescale ;
            curpoint.set((int) curx, (int) cury);
            //xy to latlng
            LatLng newpoint = mBaiduMap.getProjection().fromScreenLocation(curpoint);
            Listpoint.set(i,newpoint);
        }
    }

    public int measurescale(double zoom){
        int inputzoom=(int)zoom;
        int scale=0;
        switch (inputzoom){
            case 4:
                scale=1000000;
                break;
            case 5:
                scale=500000;
                break;
            case 6:
                scale=200000;
                break;
            case 7:
                scale=100000;
                break;
            case 8:
                scale=50000;
                break;
            case 9:
                scale=25000;
                break;
            case 10:
                scale=20000;
                break;
            case 11:
                scale=10000;
                break;
            case 12:
                scale=5000;
                break;
            case 13:
                scale=2000;
                break;
            case 14:
                scale=1000;
                break;
            case 15:
                scale=500;
                break;
            case 16:
                scale=200;
                break;
            case 17:
                scale=100;
                break;
            case 18:
                scale=50;
                break;
            case 19:
                scale=20;
                break;
            case 20:
                scale=10;
                break;
            case 21:
                scale=5;
                break;
        }
        return scale;
    }

    //配置定位SDK参数
    private void initLocation(double lat,double lng) {
        LatLng cenpt = new LatLng(lat,lng); //设定中心点坐标

        MapStatus mMapStatus = new MapStatus.Builder()//定义地图状态
                .target(cenpt)
                .zoom(15)
                .build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
    }



    private void initView() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        showinfo = (Button) findViewById(R.id.showinfo);
        showinfo.setOnClickListener(this);
        restore = (Button) findViewById(R.id.restore);
        restore.setOnClickListener(this);
        sate = (Button) findViewById(R.id.sate);
        sate.setOnClickListener(this);
        norm = (Button) findViewById(R.id.norm);
        norm.setOnClickListener(this);
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);
        pause = (Button) findViewById(R.id.pause);
        pause.setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    public void moveView(){
        new Thread(moveThread).start();
        isFirstRun=false;
    }

    private final Runnable moveThread = new Runnable() {

        @Override
        public void run() {

            while(true){
                while (isPause) {
                    Pausing();
                }
                update(ListSuffPoint,nowwind);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                drawSuffPoints(ListSuffPoint);
            }
        }
    };
    /**
     * 调用这个方法实现暂停线程
     */
    void pauseThread() {
        isPause = true;
    }
    /**
     * 调用这个方法实现恢复线程的运行
     */
    void resumeThread() {
        isPause = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }
    /**
     * 注意：这个方法只能在run方法里调用，不然会阻塞主线程，导致页面无响应
     */
    void Pausing() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.showinfo:
                if(tv_01.getVisibility()==View.INVISIBLE){
                    String info="风向:"+nowwind.getDeg()+"° 风速:"+nowwind.getSpd()+"m/s"+'\n';
                    for(int i=0;i<ListStartPoint.size();i++){
                        info=info+"释放点"+(i+1)+"浓度:"+ListStartPoint.get(i).getThick()+"mol/m^3"+'\n';
                    }
                    tv_01.setText(info);
                    tv_01.setVisibility(View.VISIBLE);}
                else{
                    tv_01.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.restore:
                pauseThread();
                mBaiduMap.clear();
                initSuffPoints();
                drawStartPoints(ListStartPoint);
                LatLng cenpt = new LatLng(39.90923,116.447428); //设定中心点坐标
                MapStatus mMapStatus = new MapStatus.Builder()//定义地图状态
                        .target(cenpt)
                        .zoom(15)
                        .build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                break;
            case R.id.sate:
                //卫星地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.norm:
                //普通地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
            case R.id.start:
                if(isFirstRun){
                    moveView();
                }
                else{
                    resumeThread();
                }
                break;
            case R.id.pause:
                pauseThread();
                break;
        }
    }

}