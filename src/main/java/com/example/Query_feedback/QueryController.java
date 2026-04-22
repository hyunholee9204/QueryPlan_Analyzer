package com.example.Query_feedback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/analyze")
    public String analyze(@RequestBody Map<String, String> request) {
        String userQuery = request.get("query").trim();
        if (userQuery.endsWith(";")) {
            userQuery = userQuery.substring(0, userQuery.length() - 1);
        }

        try {
            long startTime = System.nanoTime();

            String explainSql = "EXPLAIN (ANALYZE, FORMAT JSON) " + userQuery;
            List<Map<String, Object>> result = jdbcTemplate.queryForList(explainSql);

            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;

            String rawJson = result.toString();

            StringBuilder feedback = new StringBuilder();
            feedback.append("실제 실행 소요 시간: **").append(String.format("%.2f", durationMs)).append(" ms**\n");
            feedback.append("*(1000ms = 1초)*\n\n");

            feedback.append("최적화 리포트\n\n");

            if (rawJson.contains("\"Node Type\": \"Seq Scan\"")) {
                feedback.append("**[경고] Sequential Scan(전체 스캔) 발생!**\n");
                feedback.append("- 인덱스 없이 500만 건 이상의 데이터를 전부 훑고 있어 매우 느립니다.\n");
                feedback.append("- **조치:** 조속히 WHERE 절 컬럼에 인덱스를 추가하여 성능을 개선하세요.\n\n");
            }

            if (rawJson.contains("\"Node Type\": \"Index Scan\"")) {
                feedback.append("**[우수] 인덱스 스캔 사용 중**\n");
                feedback.append("- 인덱스를 통해 필요한 데이터만 골라내어 속도가 매우 빠릅니다.\n\n");
            }

            if (rawJson.contains("\"Parallel Aware\": true") || rawJson.contains("Parallel")) {
                feedback.append("**[정보] 병렬 처리(Parallel) 모드**\n");
                feedback.append("- 시스템이 여러 개의 CPU 코어를 할당해 처리 속도를 높였습니다.\n\n");
            }

            return feedback.toString() + "\n--- 상세 실행 계획 (Raw JSON) ---\n" + rawJson;

        } catch (Exception e) {
            e.printStackTrace();
            return "DB 분석 에러: " + e.getMessage();
        }
    }
}