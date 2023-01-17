package com.ecobrain;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSLayerGroupEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Main {

    static final Logger log = LogManager.getLogger(Main.class);

    static String uploaderPath = System.getProperty("user.dir");//개발환경 테스트 PATH
    //static String uploaderPath = "/GEOSERVER_UPLOADER"; //운영환경 테스트 PATH

    /**
     * log4j Init
     * @throws IOException
     */
    private static void log4jInit() throws IOException {
        File log4j = new File(uploaderPath + "/conf/log4j.properties");
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.setConfigLocation(log4j.toURI());
    }

    /**
     * 입력받은 카테고리 이름이 UPLOAD 리스트에 있는지
     * @param arg 찾을 문자열
     * @param strList Properties 에 정의해 놓은 upload list  ex) A|B|C|D
     * @return True/False
     */
    private static boolean isTiffFiles(String arg, String strList) {
        String[] list = strList.split("\\|");
        boolean b = false;
        for (String s : list) {
            if (s.equals(arg)) {
                b = true;
                break;
            }
        }

        return b;
    }
    /**
     * asc 파일 이름이 UPLOAD 리스트에 있는지
     * @param arg 찾을 문자열
     * @param strList Properties 에 정의해 놓은 upload list  ex) A|B|C|D
     * @return True/False
     */
    private static boolean isAscFiles(String arg, String strList) {
        String[] list = strList.split("\\|");
        boolean b = false;
        for (String s : list) {
            if (s.equals(arg)) {
                b = true;
                break;
            }
        }

        return b;
    }

    /**
     *  upload 할 파일형식이 Properties에 정의 되어있는지 확인메소드
     * @param arg 입력받은 파일 형식
     * @param strList Properties에 정의 되어 있는 List
     * @return true/false
     */
    private static boolean isExtension(String arg, String strList) {
        String[] list = strList.split("\\|");
        boolean b = false;
        for (String s : list) {
            if (s.equals(arg)) {
                b = true;
                break;
            }
        }

        return b;
    }

    /**
     *  hlep 입력시 표출해주는 로그
     * @param pro 정의 해놓은 프로퍼티
     */
    private static void getHelp(Properties pro) {
        log.info("==================================================================================");
        log.info("HOW TO USE GEOSERVER_UPLOADER");
        log.info("XXX.jar 업로드확장자          업로드할카테고리          발표날짜");
        log.info("업로드 확장자 : " + pro.getProperty("uploader.fileList"));
        log.info("업로드 TIFF 카테고리 : " + pro.getProperty("uploader.list").replace("|", ", "));
        log.info("업로드 ASC 카테고리 : " + pro.getProperty("uploader.weather.list").replace("|", ", "));
        log.info("EX) java -jar uploader.jar tiff health_100m 2022010111");
        log.info("==================================================================================");
        log.info("conf file path: " + uploaderPath + "/conf/properties.properties");

    }

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        try {
            log4jInit();
        } catch (Exception e) {
            System.out.println("####log4.properties read fail####");
            System.out.println(e.toString());
            return;
        }

        Properties pro = new Properties();
        try {
            pro.load(Files.newInputStream(Paths.get(uploaderPath + "/conf/properties.properties")));
        } catch (Exception e) {
            log.error("Properties 파일 불러오기 오류 -> " + e.toString());
            return;
        }

        if (args.length > 0) {
            String fileExt = args[0]; // 파일 형식
            if (fileExt.equals("help")) {
                getHelp(pro);
                return;
            }
            String date = args[2]; // 입력받은 발표시간
            if (date.length() < 9) {
                log.error("발표시간이 잘못되었습니다.");
                return;
            }
            if (isExtension(fileExt, pro.getProperty("uploader.fileList"))) {
                if (fileExt.equals("tiff")) {
                    String folderName = args[1]; //입력받은 카테고리
                    if (isTiffFiles(folderName, pro.getProperty("uploader.list"))) {
                        //Properties에서 정보 정보가져오기
                        String geoserver = pro.getProperty("geoserver.host"); //GEOSERVER HOST 정보
                        String baseRoot = pro.getProperty("upload." + folderName + ".path"); //UPLOAD FILE PATH
                        //String targetRoot = pro.getProperty("upload." + folderName + ".targetPath"); //생성한 파일 저장할 PATH [사용안함]
                        String extensionText = ".geotiff";//새로 생성할 파일형식
                        GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(geoserver, "admin", "geoserver");// GEOSERVER REST 접속
                        //입력받은 발표시간과 카테고리로 파일 찾기
                        File path = new File(baseRoot + File.separator + date.substring(0, 4));
                        File[] fileList = path.listFiles();
                        if (fileList == null) {
                            log.error(path.getPath() + "[파일 없음]");
                            return;
                        }
                        if (fileList.length > 0) {
                            log.info(args[1] + " 생성 START");
                            for (File file : fileList) {
                                String filename = file.getName().toLowerCase();
                                String filepath = file.getPath();
                                int idx = filename.lastIndexOf(".");
                                filename = filename.substring(0, idx); // 파일이름에서 파일형식 .tiff 제거

                                //  발표시간 파일 찾기
                                if (filename.indexOf(date) == -1) {
                                    continue;
                                } else {
                                    //새로 생성할 Path
                                    String newfilePath = baseRoot + date.substring(0, 4) + "/" + filename + extensionText;
                                    FileInputStream fin = new FileInputStream(filepath);
                                    FileOutputStream fout = new FileOutputStream(newfilePath);
                                    int tmp;
                                    while ((tmp = fin.read()) != -1) { //파일 생성
                                        fout.write(tmp);
                                    }

                                    fin.close();
                                    fout.close();

                                    File newfile = new File(newfilePath); // 생성한 파일 읽기
                                    log.info("GEOTIFF FILE : " + newfilePath + " [CREATE]");
                                    /**
                                     *  GEOSERVER TIFF 저장소 생성
                                     *  publishGeoTIFF(작업공간, 저장소이름,coverage이름, GeoTiff File, 좌표계, 투영정책, 기본스타일, bbox)
                                     */
                                    if (publisher.publishGeoTIFF("heatwave", filename, filename, newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null)) {
                                        log.info("LAYER : " + newfile.getName() + " [CREATE]");
                                        /**
                                         * 생성된 레이어로 그룹 레이어 생성하는 작업
                                         * 격자레이어와 시군구,시도 레이어를 그룹화해준다 ( 그룹화해서 썸네일 이미지에 사용하려고 )
                                         * 이미지로 사용할 필요가 있다면 그룹화를 해줘야한다
                                         */
                                        GSLayerGroupEncoder groupEncoder = new GSLayerGroupEncoder();
                                        groupEncoder.setWorkspace(null); // 작업공간이 다르면 레이어를 같이 사용할수없다. null은 최상위 작업공간이다.
                                        groupEncoder.setName("group_" + filename); // 레이어이름
                                        groupEncoder.addLayer(filename); // 추가할 레이어 (순서중요)
                                        groupEncoder.addLayer("tc_sido_info");// 추가할 레이어 (순서중요)
                                        groupEncoder.addLayer("tc_sigun_info");// 추가할 레이어 (순서중요)

                                        publisher.createLayerGroup(null, "group_" + filename, groupEncoder); //생성
                                    } else {
                                        log.error(filename + " LAYER HAVE");
                                        publisher.removeCoverageStore("rn", filename, true);
                                        log.info(filename + " DELETE STORE");
                                        publisher.publishGeoTIFF("heatwave", filename + "", filename + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null);
                                        log.info("LAYER : " + filename + " [RE CREATE]");
                                    }

                                    //log.info("GROUP_LAYER: " + "group_" + filename + "[생성완료]");
                                    //업로드한파일은 GEOSERVER_DATA_DIR 생성되기에
                                    //생성한 파일은 제거한다.
                                    File del = new File(String.valueOf(newfile));
                                    del.delete();
                                    //log.info("BASE FILE PATH : " + newfile.getName() + " [삭제완료]");
                                }
                            }
                            log.info(args[1] + " 생성 END");
                        } else {
                            log.error(baseRoot + " [파일없음]");
                        }
                    } else {
                        log.error("TIFF 폴더명 이상: " + args[1]);
                        log.error("가능한 폴더명 : " + pro.getProperty("uploader.list"));
                    }

                } else if (fileExt.equals("asc")) { // asc 파일형식 업로드
                    /**
                     * 위 Tiff 파일 형식 업로드와 90% 같다.
                     */
                    String name = args[1];
                    if (isAscFiles(name, pro.getProperty("uploader.weather.list"))) {
                        String geoserver = pro.getProperty("geoserver.host");
                        String baseRoot = pro.getProperty("upload." + name + ".path");
                        String extensionText = new String(".arcgrid");
                        GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(geoserver, "admin", "geoserver");
                        //TODO: 날짜 동적으로 변경필요
                        File path = new File(baseRoot + File.separator + date.substring(0, 4) + File.separator + date.substring(4, 6) + File.separator + date.substring(6, 8) + File.separator + date.substring(8, 10));

                        File[] fileList = path.listFiles();
                        if (fileList == null) {
                            log.error(path.getPath() + "[파일 없음]");
                            return;
                        }

                        if (fileList.length > 0) {
                            log.info(args[1] + " 생성 START");
                            for (File file : fileList) {

                                String filename = file.getName().toLowerCase();
                                String filepath = file.getPath();
                                int idx = filename.lastIndexOf(".");
                                filename = filename.substring(0, idx);


                                if (filename.indexOf(date.substring(0, 8)) == -1) {
                                    continue;
                                } else {
                                    String newfilePath = file.getParent() + File.separator + filename + extensionText;
                                    FileInputStream fin = new FileInputStream(filepath);
                                    FileOutputStream fout = new FileOutputStream(newfilePath);
                                    int tmp;
                                    while ((tmp = fin.read()) != -1) {
                                        fout.write(tmp);
                                    }

                                    fin.close();
                                    fout.close();

                                    String layerName = "";

                                    if (filename.contains("day")) {
                                        String catg = filename.substring(filename.indexOf("1km_") + 4, (filename.indexOf(date.substring(0,8)) - 1));
                                        layerName = catg + "_" + date.substring(0, 8) + "_day" + filename.substring(filename.length() - 1);
                                    }
                                    if (filename.lastIndexOf("hr") >= 10) {
                                        String catg = filename.substring(filename.indexOf("1km_") + 4, (filename.indexOf(date) - 1));
                                        String time = filename.substring(filename.indexOf(date) + 11, filename.indexOf(date) + 13);
                                        layerName = catg + "_" + date + "_" + time;
                                    }
                                    if (filename.indexOf("day") == -1 && filename.indexOf("hr") == -1) {
                                        log.info(filename + " [day,hr] 포함되어있지 않음");
                                        return;
                                    }

                                    File newfile = new File(newfilePath);
                                    log.info("ARCGRID FILE : " + newfilePath + " [CREATE]");
                                    if (publisher.publishArcGrid("heatwave", layerName + "", layerName + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "heatwave:tiff_risk_level", (double[]) null)) {
                                        log.info("LAYER : " + layerName + " [CREATE]");
                                        /*//시도레이어와 이 tiff 파일 작업공간이 다름으로 null로 설정해줘야함
                                        GSLayerGroupEncoder groupEncoder = new GSLayerGroupEncoder();
                                        groupEncoder.setWorkspace(null);
                                        groupEncoder.setName("group_" + layerName);
                                        groupEncoder.addLayer(layerName);
                                        groupEncoder.addLayer("tc_sido_info");
                                        groupEncoder.addLayer("tc_sigun_info");

                                        publisher.createLayerGroup(null, "group_" + layerName, groupEncoder);
                                        log.info("GROUP_LAYER: " + "group_" + layerName + "_sigun" + "[생성완료]");*/
                                    } else {
                                        log.error(layerName + " LAYER HAVE");
                                        publisher.removeCoverageStore("rn", layerName, true);
                                        log.info(layerName + " DELETE STORE");
                                        publisher.publishGeoTIFF("rn", layerName + "", layerName + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null);
                                        log.info("LAYER : " + layerName + " [RE CREATE]");
                                    }
                                    File del = new File(String.valueOf(newfile));
                                    del.delete();
                                    log.info("BASE FILE PATH : " + newfile.getName() + " [삭제완료]");

                                }

                            }
                        } else {
                            log.error(baseRoot + " [파일없음]");
                        }
                    } else {
                        log.error("ASC 폴더명 이상: " + args[1]);
                        log.error("가능한 폴더명 : " + pro.getProperty("uploader.weather.list"));
                    }
                }

            } else {
                log.error("확장자 이상: " + args[0]);
                log.error("가능한 확장자명 : " + pro.getProperty("uploader.fileList"));
                log.error("help 명령어를 사용해주세요.");

            }

        } else {
            log.info("help 명령어를 이용하세요");
            log.info("*.jar help");
        }

    }

}
