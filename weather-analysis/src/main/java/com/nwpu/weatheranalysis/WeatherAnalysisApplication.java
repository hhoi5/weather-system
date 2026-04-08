package com.nwpu.weatheranalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeatherAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherAnalysisApplication.class, args);
        System.out.println("=== 天气数据分析系统启动成功 ===");
    }
}