package com.nwpu.weatheranalysis.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nwpu.weatheranalysis.entity.WeatherData;
import com.nwpu.weatheranalysis.repository.WeatherDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WeatherDataCollectorService {

    private final WeatherDataRepository weatherDataRepository;

    public WeatherDataCollectorService(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
    }

    @Value("${weather.cities}")
    private String cities;

    /**
     * 每 6 小时自动采集一次天气数据
     */
    @Scheduled(fixedRate = 21600000)
    @Transactional
    public void collectWeatherData() {
        System.out.println("=== 开始定时采集天气数据 ===");
        saveAllCitiesWeather();
    }

    /**
     * 手动触发采集所有城市天气数据并存储到数据库
     */
    @Transactional
    public List<Map<String, Object>> collectAndSaveAllCities() {
        System.out.println("=== 手动触发天气数据采集 ===");
        return saveAllCitiesWeather();
    }
    
    /**
     * 核心方法：采集并保存所有城市天气数据
     * 作用：
     * 1. 遍历配置的城市列表
     * 2. 调用 Open-Meteo API 获取历史天气数据
     * 3. 解析并转换数据格式
     * 4. 存储到 MySQL 数据库（去重：存在则更新，不存在则插入）
     * 5. 返回处理后的数据（供前端展示）
     */
    private List<Map<String, Object>> saveAllCitiesWeather() {
        String[][] cityLocations = {
                {"西安", "34.2644", "108.9398"},
                {"北京", "39.9042", "116.4074"},
                {"上海", "31.2304", "121.4737"},
                {"广州", "23.1291", "113.2644"}
        };

        List<Map<String, Object>> allCityData = new ArrayList<>();
        int pastDays = 7;
        int forecastDays = 0;

        for (String[] city : cityLocations) {
            String cityName = city[0];
            String lat = city[1];
            String lon = city[2];

            try {
                // 构建 API 请求 URL
                // 作用：获取指定城市过去 7 天的历史天气数据
                // 包含：最高温、最低温、平均湿度、降水量、最大风速
                String url = String.format(
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=%s&longitude=%s" +
                                "&daily=temperature_2m_max,temperature_2m_min,relative_humidity_2m_mean,precipitation_sum,wind_speed_10m_max" +
                                "&past_days=%d&forecast_days=%d" +
                                "&timezone=Asia%%2FShanghai",
                        lat, lon, pastDays, forecastDays
                );

                // 发送 HTTP 请求
                // 作用：禁用 GZIP 压缩，防止解压错误；设置超时时间
                HttpResponse response = HttpRequest.get(url)
                        .header("Accept-Encoding", "")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .execute();

                String responseBody = response.body();

                if (responseBody == null || responseBody.trim().isEmpty()) {
                    System.err.println("采集城市 " + cityName + " 失败：返回数据为空");
                    continue;
                }

                // 解析 JSON 响应
                JSONObject json = JSONUtil.parseObj(responseBody);

                if (!json.containsKey("daily")) {
                    System.err.println("采集城市 " + cityName + " 失败：响应格式不正确");
                    continue;
                }

                JSONObject daily = json.getJSONObject("daily");

                // 提取各气象要素数组
                JSONArray time = daily.getJSONArray("time");
                JSONArray tempMax = daily.getJSONArray("temperature_2m_max");
                JSONArray tempMin = daily.getJSONArray("temperature_2m_min");
                JSONArray humidity = daily.getJSONArray("relative_humidity_2m_mean");
                JSONArray precipitation = daily.getJSONArray("precipitation_sum");
                JSONArray windSpeed = daily.getJSONArray("wind_speed_10m_max");

                System.out.println("\n===== 采集城市：" + cityName + " 过去 7 天历史天气 =====");

                // 转换为 WeatherData 对象列表并存储（去重处理）
                List<WeatherData> dataList = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (int i = 0; i < time.size(); i++) {
                    String dateStr = time.getStr(i);
                    LocalDate date = LocalDate.parse(dateStr, formatter);

                    double max = tempMax.getDouble(i);
                    double min = tempMin.getDouble(i);
                    double humi = humidity.getDouble(i);
                    double rain = precipitation.getDouble(i);
                    double wind = windSpeed.getDouble(i);

                    // 创建天气数据对象
                    WeatherData data = new WeatherData();
                    data.setCity(cityName);
                    data.setLatitude(Double.parseDouble(lat));
                    data.setLongitude(Double.parseDouble(lon));
                    data.setDate(date);
                    data.setTempMax(max);
                    data.setTempMin(min);
                    data.setTempAvg((max + min) / 2);
                    data.setHumidity(humi);
                    data.setPrecipitation(rain);
                    data.setWindSpeedMax(wind);
                    data.setPressure(1013.0); // 默认值，API 未提供
                    data.setCreateTime(LocalDateTime.now());

                    // 去重处理：检查是否已存在
                    Optional<WeatherData> existingData = weatherDataRepository.findByCityAndDate(cityName, date);

                    if (existingData.isPresent()) {
                        // 数据已存在，更新现有记录
                        WeatherData existing = existingData.get();
                        existing.setTempMax(max);
                        existing.setTempMin(min);
                        existing.setTempAvg((max + min) / 2);
                        existing.setHumidity(humi);
                        existing.setPrecipitation(rain);
                        existing.setWindSpeedMax(wind);
                        existing.setPressure(1013.0);
                        existing.setCreateTime(LocalDateTime.now());
                        dataList.add(existing);
                        System.out.println("✓ 更新数据：" + dateStr);
                    } else {
                        // 数据不存在，新增记录
                        dataList.add(data);
                        System.out.println("✓ 新增数据：" + dateStr);
                    }

                    // 输出采集结果
                    System.out.printf(
                            "%s → 最高温：%.1f℃ | 最低温：%.1f℃ | 平均温：%.1f℃ | 湿度：%.0f%% | 降水量：%.1fmm | 风速：%.1fkm/h%n",
                            dateStr, max, min, data.getTempAvg(), humi, rain, wind
                    );
                }

                // 批量保存到数据库
                // 作用：将采集的数据持久化存储，支持后续分析和查询
                weatherDataRepository.saveAll(dataList);
                System.out.println("✓ 城市 " + cityName + " 数据已保存，共 " + dataList.size() + " 条记录");

                // 准备返回给前端的数据结构
                Map<String, Object> cityResult = new HashMap<>();
                cityResult.put("city", cityName);
                cityResult.put("latitude", lat);
                cityResult.put("longitude", lon);
                cityResult.put("past7Days", dataList);
                allCityData.add(cityResult);

            } catch (Exception e) {
                System.err.println("采集城市 " + cityName + " 失败");
                e.printStackTrace();
            }
        }

        System.out.println("\n=== 所有城市天气数据采集完成 ===");
        return allCityData;
    }

    /**
     * 从数据库查询指定城市的天气数据
     * 作用：支持前端按城市和时间范围查询历史数据
     */
    public List<WeatherData> getHistoricalData(String city, LocalDate startDate, LocalDate endDate) {
        return weatherDataRepository.findByCityAndDateBetween(city, startDate, endDate);
    }

    /**
     * 获取所有已采集的城市列表
     */
    public List<String> getAvailableCities() {
        return weatherDataRepository.findAllCities();
    }
}
