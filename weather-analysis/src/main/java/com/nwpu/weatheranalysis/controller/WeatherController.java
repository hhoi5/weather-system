package com.nwpu.weatheranalysis.controller;

import com.nwpu.weatheranalysis.entity.WeatherData;
import com.nwpu.weatheranalysis.service.WeatherDataCollectorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherDataCollectorService weatherDataCollectorService;

    public WeatherController(WeatherDataCollectorService weatherDataCollectorService) {
        this.weatherDataCollectorService = weatherDataCollectorService;
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public String test() {
        return "西北工业大学 - 王逸豪 - 天气数据分析系统运行成功 ✅";
    }

    /**
     * 采集并获取所有城市的天气数据
     * 作用：
     * 1. 调用 API 采集最新数据
     * 2. 保存到数据库
     * 3. 返回给前端用于可视化展示
     */
    @GetMapping("/collect")
    public Map<String, Object> collectWeatherData() {
        List<Map<String, Object>> allCityData = weatherDataCollectorService.collectAndSaveAllCities();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "数据采集成功");
        result.put("totalCities", allCityData.size());
        result.put("data", allCityData);
        return result;
    }

    /**
     * 从数据库查询历史天气数据
     * 作用：支持用户按城市、时间范围查询已存储的历史数据
     * 参数：
     * - city: 城市名称
     * - startDate: 开始日期（可选，默认 7 天前）
     * - endDate: 结束日期（可选，默认今天）
     */
    @GetMapping("/history")
    public Map<String, Object> getHistoryData(
            @RequestParam String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 如果未指定日期范围，默认查询最近 7 天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<WeatherData> dataList = weatherDataCollectorService.getHistoricalData(city, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "查询成功");
        result.put("city", city);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("count", dataList.size());
        result.put("data", dataList);
        return result;
    }

    /**
     * 获取可用的城市列表
     */
    @GetMapping("/cities")
    public Map<String, Object> getCities() {
        List<String> cities = weatherDataCollectorService.getAvailableCities();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("cities", cities);
        return result;
    }
}
