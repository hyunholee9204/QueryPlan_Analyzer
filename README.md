# QueryPlan-Analyzer

**SQL 실행 계획 분석 및 쿼리 최적화 가이드 도구**

PostgreSQL의 `EXPLAIN ANALYZE`를 활용하여 520만 건 대용량 데이터 환경에서의 쿼리 성능 병목을 진단하고 실시간 최적화 리포트를 제공하는 프로젝트입니다.

## 데이터

이 프로젝트는 실질적인 쿼리 튜닝 성능 차이를 증명하기 위해 대용량 데이터셋을 활용하였습니다.

* **Dataset**: PostgreSQL 5,200,000+ Row 기반 `orders` 테이블
* **Data Source**: 본인이 직접 구축한 [PostgreSQL 500만 건 데이터 성능 최적화 실습 프로젝트](https://github.com/hyunholee9204/SQL_Tuning)의 데이터셋을 연동하여 사용하였습니다.
* **학습 연계성**: 이전 프로젝트에서 구축한 대용량 환경을 바탕으로, 이를 실시간으로 분석하고 리포팅하는 '분석 엔진'을 이번 프로젝트에서 구현하였습니다.

## 주요 기능
* **실시간 실행 계획 분석**: `Node Type` 탐지를 통해 인덱스 활용 여부 진단
* **정밀한 성능 측정**: 쿼리 비용(Cost) 및 실제 실행 시간(ms) 시각화
* **DBA 자동 리포트**: 초보 개발자도 이해할 수 있는 최적화 처방전 제공
* **대용량 데이터 환경**: 520만 건 이상의 실데이터 기반 튜닝 테스트 환경 구축

## 분석 엔진

사용자가 입력한 쿼리의 실행 계획을 읽어 조언을 생성합니다.  
PostgreSQL의 실행 계획 JSON 데이터를 파싱하여 Node Type에 따른 성능 병목을 자동 감지합니다.  
Seq Scan과 Index Scan 등 핵심 지표를 식별하여 사용자에게 즉각적인 최적화 가이드를 제공하는 프로젝트의 핵심 브레인입니다.  

```java
if (rawJson.contains("\"Node Type\": \"Seq Scan\"")) {
    feedback.append("**[경고] Sequential Scan(전체 스캔) 발생!**\n");
    feedback.append("- 인덱스 없이 500만 건 이상의 데이터를 전부 읽고 있습니다.\n");
    feedback.append("- **조치:** WHERE 절 컬럼에 인덱스를 추가하여 성능을 개선하세요.\n");
}

if (rawJson.contains("\"Node Type\": \"Index Scan\"")) {
    feedback.append("**[우수] 인덱스 스캔 사용 중**\n");
    feedback.append("- 인덱스를 통해 필요한 데이터만 골라내어 속도가 매우 빠릅니다.\n");
}

```

## 데이터 셋업(520만의 대용량 데이터 구축)

520만 건의 대용량 실데이터 환경을 구축하여 쿼리 튜닝의 실질적인 성능 차이를 테스트할 수 있는 기반을 마련했습니다.  
인덱스 유무에 따라 시스템 부하와 실행 비용이 어떻게 변하는지 극명하게 보여주기 위한 고부하 데이터셋입니다.  

```sql
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    order_date TIMESTAMP,
    status TEXT
);

-- 데이터 삽입  
INSERT INTO orders (order_id, user_id, order_date, status)
SELECT i, (random() * 1000000)::INT, now(), 'COMPLETED'
FROM generate_series(1, 5200000) s(i);

-- 인덱스 생성 전/후 비교를 위함  
CREATE INDEX idx_orders_order_id ON orders(order_id);

```

## 실행 시간 측정 로직

단순 수치상의 비용(Cost)을 넘어, 나노초(ns) 단위의 실측 시스템 시간을 측정하여 사용자 체감 성능을 수치화했습니다.  
**EXPLAIN ANALYZE**를 통해 실제 DB 작업 시간과 쿼리 계획 수립 시간(Planning Time)을 분리하여 분석합니다.  

```java

long startTime = System.nanoTime();

String explainSql = "EXPLAIN (ANALYZE, FORMAT JSON) " + userQuery;
List<Map<String, Object>> result = jdbcTemplate.queryForList(explainSql);

long endTime = System.nanoTime();
double durationMs = (endTime - startTime) / 1_000_000.0;

```

## 성능 개선 결과
| 지표 | 튜닝 전 (Seq Scan) | 튜닝 후 (Index Scan) |
| :--- | :--- | :--- |
| **실행 시간** | 약 3,500ms | **15.55ms** |
| **비용(Cost)** | 95,404 | **8.45** |

---

