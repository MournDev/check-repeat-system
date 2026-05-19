package com.abin.checkrepeatsystem.admin.service;

import java.util.List;
import java.util.Map;

public interface AdminSchoolService {

    Map<String, Object> getSchoolOverview();

    Map<String, Object> getCollegeDistribution();

    List<Map<String, Object>> getMonthlyTrend();

    Map<String, Object> getSimilarityDistribution();

    Map<String, Object> getRealtimeStats();
}
