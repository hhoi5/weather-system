package com.nwpu.weatheranalysis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "weather_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"city", "date"}))
public class WeatherData {
    // 数据主键 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 城市名称
    private String city;
    // 纬度
    private Double latitude;
    // 经度
    private Double longitude;
    // 日期（历史数据的日期）
    private LocalDate date;
    // 最高温度（摄氏度）
    private Double tempMax;
    // 最低温度（摄氏度）
    private Double tempMin;
    // 平均温度（摄氏度）
    private Double tempAvg;
    // 湿度（百分比）
    private Double humidity;
    // 降水量（毫米）
    private Double precipitation;
    // 最大风速（米/秒）
    private Double windSpeedMax;
    // 气压（百帕）
    private Double pressure;
    // 数据采集时间
    private LocalDateTime createTime;
}
