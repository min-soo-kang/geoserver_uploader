# geoserver_uploader 소스 정리

이 프로젝트는 **파일 시스템의 래스터 파일(TIFF/ASC)을 GeoServer REST API로 업로드**하는 Java 기반 배치 도구입니다.

## 1) 프로젝트 구조

```text
geoserver_uploader/
├─ src/com/ecobrain/Main.java        # 폭염(heatwave) 계열 업로드 엔트리포인트
├─ src/com/ecobrain/RnMain.java      # 호우(rn) 계열 업로드 엔트리포인트
├─ conf/properties.properties         # 업로드 대상/경로/카테고리 설정
├─ conf/log4j.properties             # 로그 설정
├─ lib/*.jar                         # geoserver-manager, log4j, slf4j 라이브러리
└─ src/META-INF/MANIFEST.MF          # 매니페스트
```

## 2) 주요 동작 개요

### Main.java (폭염/일반 업로드)
- CLI 인자: `확장자`, `카테고리`, `발표시각`.
- 지원 확장자: `uploader.fileList`(기본 `tiff|asc`).
- `tiff` 처리:
  - `uploader.list`에 포함된 카테고리만 허용.
  - `upload.<카테고리>.path` 하위에서 파일 검색 후 `.geotiff` 임시 파일 생성.
  - GeoServer workspace `heatwave`로 `publishGeoTIFF` 수행.
  - 필요 시 레이어 그룹(`group_<layer>`) 생성.
- `asc` 처리:
  - `uploader.weather.list`(기본 weather) 카테고리 대상.
  - 날짜 기반 하위 경로(YYYY/MM/DD/HH)에서 파일 검색.
  - `.arcgrid` 임시 파일 생성 후 `publishArcGrid` 수행.
- 업로드 성공/재생성 후 임시 파일 삭제.

### RnMain.java (호우 영향도 업로드)
- CLI 인자: `확장자`, `카테고리`, `[시제코드|발표시각]`.
- 지원 확장자: `uploader.rnfileList`(기본 `tiff`).
- 분기:
  - 시제코드(6자리) 업로드.
  - 발표시각(10자리) 업로드.
  - 예외적으로 `AMC` 카테고리는 시각 인자 없이 고정 데이터 업로드 지원.
- GeoServer workspace `rn`으로 `publishGeoTIFF` 수행.
- 동일 레이어가 이미 있으면 저장소 제거 후 재생성.

## 3) 설정 파일(conf/properties.properties)

핵심 키:
- `geoserver.host`: GeoServer REST 엔드포인트.
- `uploader.fileList`: Main에서 허용할 확장자 목록.
- `uploader.list`: Main의 TIFF 허용 카테고리.
- `uploader.weather.list`: Main의 ASC 허용 카테고리.
- `uploader.rnfileList`: RnMain에서 허용할 확장자 목록.
- `uploader.rnlist`: RnMain의 허용 카테고리 목록.
- `upload.<카테고리>.path`: 실제 입력 파일 루트 경로.

> 주의: `properties.properties`에 운영 경로가 하드코딩되어 있어, 환경별 배포 시 경로값 점검이 필요합니다.

## 4) 실행 예시

### 도움말
```bash
java -cp "lib/*:." com.ecobrain.Main help
java -cp "lib/*:." com.ecobrain.RnMain help
```

### Main (폭염 TIFF)
```bash
java -cp "lib/*:." com.ecobrain.Main tiff health_100m 2022010111
```

### Main (기상 ASC)
```bash
java -cp "lib/*:." com.ecobrain.Main asc weather 2022010111
```

### RnMain (시제코드)
```bash
java -cp "lib/*:." com.ecobrain.RnMain tiff IMPACT_LVL_LIVING 230101
```

### RnMain (발표시각)
```bash
java -cp "lib/*:." com.ecobrain.RnMain tiff IMPACT_LVL_LIVING 2024010100
```

## 5) 코드 특이사항 (정리 포인트)

- 파일 복사 로직(`FileInputStream`/`FileOutputStream` byte-by-byte)이 여러 곳에 중복되어 있습니다.
- `isTiffFiles`, `isAscFiles`, `isExtension`처럼 동일 패턴 검증 함수가 반복됩니다.
- GeoServer 접속 계정(`admin/geoserver`)이 코드에 고정되어 있습니다.
- 예외 처리가 broad catch 위주라 실패 원인 분리가 어려울 수 있습니다.

## 6) 개선 권장사항

1. 공통 유틸 분리
   - 확장자/카테고리 검증, 파일 복사, 업로드+재생성 패턴 공통화.
2. 보안/설정 분리
   - GeoServer 계정, workspace, style을 properties 또는 환경변수로 외부화.
3. 인자 파서 도입
   - 길이 기반(6/10) 분기 대신 명시적 옵션(`--mode sije|time`) 사용.
4. 로그 고도화
   - 업로드 대상 파일 수, 성공/실패 건수 요약 로그 추가.

---
필요하면 다음 단계로 **실제 리팩터링(중복 제거 + 설정 외부화)**까지 이어서 진행할 수 있습니다.
