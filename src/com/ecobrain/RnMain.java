package com.ecobrain;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class RnMain {
    static final Logger log = LogManager.getLogger(Main.class);

    static String uploaderPath = System.getProperty("user.dir");
    //static String uploaderPath = "/GEOSERVER_UPLOADER";

    private static void log4jInit() throws IOException {
        File log4j = new File(uploaderPath + "/conf/log4j.properties");
//      property 타입으로 읽어서 configure와 연동
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.setConfigLocation(log4j.toURI());
    }

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

    private static void getHelp(Properties pro) {
        log.info("==================================================================================");
        log.info("HOW TO USE GEOSERVER_UPLOADER");
        log.info("XXX.jar 업로드확장자          업로드할카테고리          [시제코드/발표날짜]");
        log.info("업로드 확장자 : " + pro.getProperty("uploader.rnfileList"));
        log.info("업로드 TIFF 카테고리 : " + pro.getProperty("uploader.rnlist").replace("|", "\n "));
        log.info("EX) java -jar uploader.jar tiff IMPACT_LVL_LIVING {발표시간/시제코드}");
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
            log.error("파일 불러오기 오류 -> " + e.toString());
            return;
        }

        if (args.length > 0) {
            String fileExt = args[0];
            if (fileExt.equals("help")) {
                getHelp(pro);
                return;
            }
            if (isExtension(fileExt, pro.getProperty("uploader.rnfileList"))) {
                try {
                    String date = args[2];
                    if (date.length() == 6) { //시제코드
                        if (fileExt.equals("tiff")) {
                            String fileName = args[1];
                            String geoserver = pro.getProperty("geoserver.host");
                            String baseRoot = pro.getProperty("upload." + fileName + ".path");
                            String extensionText = ".geotiff";
                            GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(geoserver, "admin", "geoserver");
                            File path = new File(baseRoot);
                            File[] fileList = path.listFiles();
                            if (fileList == null) {
                                log.error(path.getPath() + "[파일 없음]");
                                return;
                            }
                            if (isTiffFiles(fileName, pro.getProperty("uploader.rnlist"))) {
                                log.info("++++ " + args[1] + " LAYER CREATE START ++++");
                                for (File file : fileList) {
                                    String name = file.getName().toLowerCase();
                                    String filepath = file.getPath();
                                    int idx = name.lastIndexOf(".");
                                    name = name.substring(0, idx);
                                    //파일중에 내가선택한 카테고리와 시제코드 확인
                                    String sijeCode = name.split("_")[name.split("_").length-1];
                                    String layerNm = name.substring(0, name.indexOf(sijeCode)-1);
                                    if (layerNm.equals(fileName.toLowerCase()) && sijeCode.equals(date)) {
                                        String newfilePath = baseRoot + name + extensionText;
                                        FileInputStream fin = new FileInputStream(filepath);
                                        FileOutputStream fout = new FileOutputStream(newfilePath);
                                        int tmp;
                                        while ((tmp = fin.read()) != -1) {
                                            fout.write(tmp);
                                        }

                                        fin.close();
                                        fout.close();

                                        File newfile = new File(newfilePath);
                                        log.info("GEOTIFF FILE : " + newfilePath + " [CREATE]");
                                        if (publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null)) {
                                            log.info("LAYER : " + newfile.getName() + " [CREATE]");
                                        } else {
                                            /**
                                             * 이미 업로드 되어있따면 삭제후 다시 생성후 파일 삭제
                                             */
                                            log.error(name + " LAYER HAVE");
                                            publisher.removeCoverageStore("rn", name, true);
                                            log.info(name + " DELETE STORE");
                                            publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null);
                                            log.info("LAYER : " + name + " [RE CREATE]");
                                        }
                                        File del = new File(String.valueOf(newfile));
                                        del.delete();
                                    }
                                }
                                log.info("+++++" + args[1] + " CREATE END +++++");
                            }
                        }
                    } else if (date.length() == 10) { //발표시간
                        if (fileExt.equals("tiff")) {
                            String fileName = args[1];
                            String geoserver = pro.getProperty("geoserver.host");
                            String baseRoot = pro.getProperty("upload." + fileName + ".path");
                            String extensionText = ".geotiff";
                            GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(geoserver, "admin", "geoserver");
                            File path = new File(baseRoot + File.separator + date.substring(0, 6) + File.separator + date.substring(6, 8) + File.separator + "tif/");
                            File[] fileList = path.listFiles();
                            if (fileList == null) {
                                log.error(path.getPath() + "[파일 없음]");
                                return;
                            }
                            if (isTiffFiles(fileName, pro.getProperty("uploader.rnlist"))) {
                                log.info("++++ " + args[1] + " LAYER CREATE START ++++");
                                for (File file : fileList) {
                                    String name = file.getName().toLowerCase();
                                    String filepath = file.getPath();
                                    int idx = name.lastIndexOf(".");
                                    name = name.substring(0, idx);

                                    if (name.substring(0,name.indexOf("hr")-3).toUpperCase().equals(fileName)) {
                                        String newfilePath = baseRoot + name + extensionText;
                                        FileInputStream fin = new FileInputStream(filepath);
                                        FileOutputStream fout = new FileOutputStream(newfilePath);
                                        int tmp;
                                        while ((tmp = fin.read()) != -1) {
                                            fout.write(tmp);
                                        }

                                        fin.close();
                                        fout.close();

                                        File newfile = new File(newfilePath);
                                        log.info("GEOTIFF FILE : " + newfilePath + " [CREATE]");
                                        if (publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null)) {
                                            log.info("LAYER : " + name + " [CREATE]");
                                        } else {
                                            /**
                                             * 이미 업로드 되어있따면 삭제후 다시 생성후 파일 삭제
                                             */
                                            log.error(name + " LAYER HAVE");
                                            publisher.removeCoverageStore("rn", name, true);
                                            log.info(name + " DELETE STORE");
                                            publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_risk_level", (double[]) null);
                                            log.info("LAYER : " + name + " [RE CREATE]");
                                        }

                                        File del = new File(String.valueOf(newfile));
                                        del.delete();
                                    }

                                }
                                log.info("+++++" + args[1] + " CREATE END +++++");
                            }
                        }
                    } else {
                        log.error("발표시간/시제코드 입력이 잘못되었습니다.");
                        return;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    /**
                     * 카테고리 AMC 경우에는 발표시간/시제코드 가 없는 고정데이터이라서
                     */
                    try {
                        String fileName = args[1];
                        if (fileName.toUpperCase().equals("AMC")) {
                            log.info("++++ " + args[1] + " LAYER CREATE START ++++");
                            String geoserver = pro.getProperty("geoserver.host");
                            String baseRoot = pro.getProperty("upload." + fileName + ".path");
                            String extensionText = ".geotiff";
                            GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(geoserver, "admin", "geoserver");
                            File path = new File(baseRoot);
                            File[] fileList = path.listFiles();
                            if (fileList == null) {
                                log.error(path.getPath() + "[파일 없음]");
                                return;
                            }
                            for (File file : fileList) {
                                String name = file.getName().toLowerCase();
                                String filepath = file.getPath();
                                int idx = name.lastIndexOf(".");
                                name = name.substring(0, idx);

                                String newfilePath = baseRoot + name + extensionText;
                                FileInputStream fin = new FileInputStream(filepath);
                                FileOutputStream fout = new FileOutputStream(newfilePath);
                                int tmp;
                                while ((tmp = fin.read()) != -1) {
                                    fout.write(tmp);
                                }

                                fin.close();
                                fout.close();

                                File newfile = new File(newfilePath);
                                log.info("GEOTIFF FILE : " + newfilePath + " [CREATE]");
                                if (publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_big_rns", (double[]) null)) {
                                    log.info("LAYER : " + newfile.getName() + " [CREATE]");
                                } else {
                                    log.error(name + " LAYER HAVE");
                                    publisher.removeCoverageStore("rn", name, true);
                                    log.info(name + " DELETE STORE");
                                    publisher.publishGeoTIFF("rn", name + "", name + "", newfile, "EPSG:5179", GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED, "tiff_big_rns", (double[]) null);
                                    log.info("LAYER : " + name + " [RE CREATE]");
                                }
                                File del = new File(String.valueOf(newfile));
                                del.delete();

                            }

                            log.info("+++++" + args[1] + " CREATE END +++++");
                        } else {
                            log.error("발표시간/시제코드 입력해주세요.");
                            return;
                        }
                    } catch (ArrayIndexOutOfBoundsException e1) {
                        log.error("생성할 카테고리를 입력해주세요");
                        return;
                    }
                }

            }

        } else {
            log.info("help 명령어를 이용하세요");
            log.info("*.jar help");
        }
    }
}
