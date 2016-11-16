package com.ljw.device3x.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.ljw.device3x.R;
import com.ljw.device3x.Utils.NameIdMap;
import com.ljw.device3x.Utils.Utils;
import com.ljw.device3x.adapter.ForecastButtonAdapter;
import com.ljw.device3x.bean.WeatherInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/5/31 0031.
 */
public class WeatherAcivity extends Activity {

    private GridView forecastGridView;//天气预报
    private ForecastButtonAdapter forecastButtonAdapter;
    private TextView weatherLocCity;//定位城市
    private TextView humidity;//湿度
    private TextView windSpeed;//风速
    private TextView bigTmpTxt;//中间的温度大图
    private ImageView bigWeatherImg;//中间的天气大图
    private TextView bigWeatherInfo;//中间的天气描述
    private TextView currentDateText;//当前的日期
    private TextView AMOrPMText;//上午还是下午


    private LocationManager mlocation;//用系统的GPS获取当前位置信息

    private List<WeatherInfo> forecastList;
    private List<WeatherInfo> defaultList;
    private WeatherInfo nowDate;
    private Map<String, String> mapAllNameID;


    private static String locationUrl = "http://lbs.juhe.cn/api/getaddressbylngb?lngx=";//获取城市名称URL
    private static String weatherUrl = "http://cdn.weather.hao.360.cn/api_weather_info.php?app=hao360&_jsonp=data&code=";//根据城市获取天气信息URL
    private static final String ACTION_TIMEZONE_CHANGED = Intent.ACTION_TIME_TICK;//监听时区变化的广播
    private boolean isEnableClick = false;

    private MsgRecieve mRecieve;//广播监听


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weatherlayout);
        initCityCodeTable();
        regisReceive();
        changeAMOrPM();
        getDefaultGridData();
        getCityName(getUrl(Utils.getLongitude(getApplicationContext()), Utils.getLatitude(getApplicationContext())));
    }

    /**
     * 没获取到网络数据时的死数据
     */
    private void getDefaultGridData() {
        WeatherInfo wi;
        defaultList = new ArrayList<WeatherInfo>();
        for (int i = 0; i < 5; ++i) {
            wi = new WeatherInfo();
            wi.setWhichDayOfWeek("星期天");
            wi.setWeatherTmp("25℃");
            wi.setCurrentDate("1月1日");
            wi.setWeatherWind("西南风");
            wi.setWeatherImg(R.mipmap.forecast_sun);
            wi.setWeatherInfo("晴天");
            defaultList.add(wi);
        }
        initGridView(defaultList);
    }

    /**
     * 初始化城市代码
     */
    private void initCityCodeTable() {
        mapAllNameID = new HashMap<String, String>();
        NameIdMap nameIDMap = new NameIdMap();
        mapAllNameID = nameIDMap.getMapAllNameID();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        forecastGridView = (GridView) findViewById(R.id.forecastgridview);
        weatherLocCity = (TextView) findViewById(R.id.weatherloccity);
        humidity = (TextView) findViewById(R.id.humiditytext);
        windSpeed = (TextView) findViewById(R.id.windspeedtext);
        bigTmpTxt = (TextView) findViewById(R.id.weathertmp);
        bigWeatherImg = (ImageView) findViewById(R.id.weatherimg);
        bigWeatherInfo = (TextView) findViewById(R.id.weatherinfo);
        currentDateText = (TextView) findViewById(R.id.currentdate);
        AMOrPMText = (TextView) findViewById(R.id.amOrpmtext);
        forecastList = new ArrayList<WeatherInfo>();
    }

    /**
     * 初始化天气预报的gridview数据
     */
    private void initGridView(final List<WeatherInfo> weatherlist) {
        forecastButtonAdapter = new ForecastButtonAdapter(this, weatherlist);
        forecastGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        forecastGridView.setAdapter(forecastButtonAdapter);
        forecastGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                notifyUpdateUI(weatherlist.get(position));
            }
        });
    }

    /**
     * 注册广播接收器并开启位置服务
     */
    private void regisReceive() {
        mRecieve = new MsgRecieve();
        IntentFilter locationFilter = new IntentFilter();
        locationFilter.addAction(ACTION_TIMEZONE_CHANGED);
        registerReceiver(mRecieve, locationFilter);
    }

    /**
     * 接收广播
     */
    public class MsgRecieve extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log_i("Action:" + action);
            if (ACTION_TIMEZONE_CHANGED.equals(action)) {
                log_i("本地时间已改变");
                changeAMOrPM();
            }
        }
    }

    /**
     * 改变时间的AM或者PM的显示(上午或者下午)
     */
    private void changeAMOrPM() {
        Calendar c = Calendar.getInstance();
        AMOrPMText.setText((c.get(Calendar.HOUR_OF_DAY)) > 12 ? "pm" : "am");
    }

    /**
     * 根据当前的位置获取城市名URL
     */
    private String getUrl(String lon, String lat) {
        return (("".equals(lon) && lon == null) || ("").equals(lat) && lat == null) ?
                locationUrl + Utils.QINGHUA_LON + "&lngy=" + Utils.QINGHUA_LAT :
                locationUrl + lon + "&lngy=" + lat;
    }


    /**
     * 根据当前位置来获取城市名，如果当前城市和默认城市是一样的，则不保存，否则保存为默认城市
     *
     * @parms 请求的URL
     */
    private void getCityName(String url) {
        HttpUtils mUtils = new HttpUtils();
        mUtils.send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
            @Override
            public void onFailure(HttpException e, String s) {
                log_i(e.toString());
                e.printStackTrace();
            }

            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                String res = responseInfo.result;
                try {
                    JSONObject jo = new JSONObject(res).getJSONObject("row")
                            .getJSONObject("result")
                            .getJSONObject("addressComponent");
                    String cityName = jo.getString("city");
                    log_i("当前的城市:" + cityName);
                    getWeatherInfoByCityCode(weatherUrl + mapAllNameID.get(cityName.substring(0, cityName.length() - 1)));
                    //若获取到当前的城市和本地储存的不一样则更新为当地的城市
//                    if (!Utils.getCity(getApplicationContext()).equals(cityName.substring(0, cityName.length() - 1)))
//                        Utils.setCity(getApplicationContext(), cityName.substring(0, cityName.length() - 1));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 根据当前的城市代码获取相应的天气信息
     */
    private void getWeatherInfoByCityCode(String url) {
        HttpUtils mUtils = new HttpUtils();
        log_i("城市代码的url:" + url);
        mUtils.send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                log_i("原始数据:" + responseInfo.result);
                String weatherinfo = cutJsonString(Utils.unicodeToString(Utils.formatJsonString(responseInfo.result)));
                log_i(weatherinfo);
                parseWeatherInfo(weatherinfo);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                log_i("onFailure" + s);
            }
        });
    }

    /**
     * 剪切字符串，这个url返回的JSON数据有个包头没用
     */
    private String cutJsonString(String info) {
        return info.substring(5, info.length() - 2);
    }


    /**
     * 解析更新天气信息
     */
    private void parseWeatherInfo(String info) {
        parseCityInfo(info);
        for (int i = 0; i < 5; ++i) {
            getAndAddForecastGridView(parseWhichDayForcastInfo(info, i));
        }
        initGridView(forecastList);
        notifyUpdateUI(forecastList.get(0));
    }

    /**
     * 解析更新城市
     */
    private void parseCityInfo(String info) {
        try {
            JSONObject jo = new JSONObject(info);
            JSONArray ja = jo.getJSONArray("area");
            JSONArray ja1 = (JSONArray) ja.get(2);
            log_i("解析的城市;" + ja1.get(0).toString());
            updateperCitys(ja1.get(0).toString());
        } catch (JSONException e) {
            log_i("解析出错" + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 取得第几天天气预报信息
     */
    private JSONObject parseWhichDayForcastInfo(String info, int which) {
        try {
            JSONObject jo = new JSONObject(info);
            JSONArray ja = jo.getJSONArray("weather");
            return (JSONObject) ja.get(which);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析并存入GridView的Item的数据
     */
    private void getAndAddForecastGridView(JSONObject jsonObject) {
        if (jsonObject == null)
            return;
        nowDate = new WeatherInfo();
        try {
            log_i("日期是:" + Utils.formatDate(jsonObject.get("date").toString()));
            JSONObject jsonObject1 = jsonObject.getJSONObject("info");
            JSONArray jsonArray = jsonObject1.has("day") ? jsonObject1.getJSONArray("day") : jsonObject1.getJSONArray("night");
            log_i("天气是:" + jsonArray.get(1).toString());
            log_i("温度是:" + jsonArray.get(2).toString());
            log_i("风向是:" + jsonArray.get(4).toString());
            log_i(Utils.getWeekDays(jsonObject.get("date").toString()));
            nowDate.setWeatherInfo(jsonArray.get(1).toString());
            nowDate.setWeatherImg(getWeathersmallImg(jsonArray.get(1).toString()));
            nowDate.setWeatherWind(jsonArray.get(4).toString());
            nowDate.setCurrentDate(Utils.formatDate(jsonObject.get("date").toString()));
            nowDate.setWeatherTmp(jsonArray.get(2).toString() + "℃");
            nowDate.setWhichDayOfWeek(Utils.getWeekDays(jsonObject.get("date").toString()));
            forecastList.add(nowDate);
        } catch (JSONException e) {
            log_i("解析错误:" + e.toString());
            e.printStackTrace();
        }
    }


    /**
     * 根据天气信息设置天气图片（大图）
     *
     * @param cond 天气信息
     * @return 对应的天气图片id
     */
    private int getWeatherImg(String cond) {
        int img = 0;
        int length = cond.length();
        if (cond.contains("晴"))
            img = R.mipmap.weather_sun;
        else if (cond.contains("多云"))
            img = R.mipmap.weather_cloud;
        else if (cond.contains("阴"))
            img = R.mipmap.weather_overcast;
        else if (cond.contains("雷"))
            img = R.mipmap.weather_thunderandrain;
        else if (cond.contains("雨")) {
            if (cond.contains("小雨"))
                img = R.mipmap.weather_smallrain;
            else if (cond.contains("中雨"))
                img = R.mipmap.weather_middlerain;
            else if (cond.contains("大雨"))
                img = R.mipmap.weather_bigrain;
            else if (cond.contains("雨夹雪"))
                img = R.mipmap.weather_rainandsnow;
            else if (cond.contains("暴雨"))
                img = R.mipmap.weather_stormrain;
            else
                img = R.mipmap.weather_smallrain;
        } else if (cond.contains("雪")) {
            if (cond.contains("小雪"))
                img = R.mipmap.weather_smallsnow;
            else if (cond.contains("中雪"))
                img = R.mipmap.weather_middlesnow;
            else
                img = R.mipmap.weather_smallsnow;
        } else
            img = R.mipmap.weather_sun;
        return img;
    }

    /**
     * 根据天气信息设置天气图片（小图）
     *
     * @param cond 天气信息
     * @return 对应的天气图片id
     */
    private int getWeathersmallImg(String cond) {
        int img = 0;
        int length = cond.length();
        if (cond.contains("晴") && length <= 2)
            img = R.mipmap.forecast_sun;
        else if (cond.contains("多云"))
            img = R.mipmap.forecast_cloud;
        else if (cond.contains("阴") && length <= 2)
            img = R.mipmap.forecast_overcast;
        else if (cond.contains("雷"))
            img = R.mipmap.forecast_thunderandrain;
        else if (cond.contains("雨")) {
            if (cond.contains("小雨"))
                img = R.mipmap.forecast_smallrain;
            else if (cond.contains("中雨"))
                img = R.mipmap.forecast_middlerain;
            else if (cond.contains("大雨"))
                img = R.mipmap.forecast_bigrain;
            else if (cond.contains("雨夹雪"))
                img = R.mipmap.forecast_rainandsnow;
            else if (cond.contains("暴雨"))
                img = R.mipmap.forecast_stormrain;
            else
                img = R.mipmap.forecast_smallrain;
        } else if (cond.contains("雪")) {
            if (cond.contains("小雪"))
                img = R.mipmap.forecast_smallsnow;
            else if (cond.contains("中雪"))
                img = R.mipmap.forecast_middlesnow;
            else
                img = R.mipmap.forecast_smallsnow;
        } else
            img = R.mipmap.forecast_sun;
        return img;
    }

    /**
     * 动态更新界面其他的UI元素
     */
    private void notifyUpdateUI(WeatherInfo weatherInfo) {
        windSpeed.setText(weatherInfo.getWeatherWind());
        bigWeatherImg.setImageResource(getWeatherImg(weatherInfo.getWeatherInfo()));
        bigTmpTxt.setText(weatherInfo.getWeatherTmp());
        bigWeatherInfo.setText(weatherInfo.getWeatherInfo());
        currentDateText.setText(weatherInfo.getCurrentDate() + " " + weatherInfo.getWhichDayOfWeek());

        bigWeatherImg.setImageResource(getWeatherImg(weatherInfo.getWeatherInfo()));
        bigWeatherInfo.setText(weatherInfo.getWeatherInfo());
        bigTmpTxt.setText(weatherInfo.getWeatherTmp());
    }

    /**
     * 更新城市UI
     */
    private void updateperCitys(String cityname) {
        weatherLocCity.setText(cityname);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mRecieve);
        log_i("onDestroy");
    }

    private void log_i(String s) {
        Log.i("ljwtest:", s);
    }
}
