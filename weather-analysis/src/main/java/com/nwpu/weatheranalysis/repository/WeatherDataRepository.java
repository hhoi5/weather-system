package com.nwpu.weatheranalysis.repository;

import com.nwpu.weatheranalysis.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    // 作用：按城市和日期范围查询天气数据
    List<WeatherData> findByCityAndDateBetween(String city, LocalDate startDate, LocalDate endDate);

    // 作用：按城市查询，按日期倒序排列
    List<WeatherData> findByCityOrderByDateDesc(String city);

    // 作用：查询所有已存储的城市名称
    @Query("SELECT DISTINCT w.city FROM WeatherData w")
    List<String> findAllCities();

    // 作用：根据城市和日期查询单条数据（用于去重）
    Optional<WeatherData> findByCityAndDate(String city, LocalDate date);
}
