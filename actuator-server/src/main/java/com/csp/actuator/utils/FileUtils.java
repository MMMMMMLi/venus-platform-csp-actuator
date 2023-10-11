package com.csp.actuator.utils;


import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件处理工具类
 *
 * @author yulang
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

    public static long KB = 1024L;
    public static long MB = 1024*1024L;
    public static long GB = 1024*1024*1024L;
    public static long TB = 1024*1024*1024*1024L;

    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);
    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os 输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 下载文件名重新编码
     *
     * @param request 请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName)
            throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 获取文件内容
     *
     * @param fileName 文件名称
     * @return 文件内容
     */
    public static String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

    /**
     * 获取输入流中的内容
     * @param inputStream 输入流
     * @return 字符内容
     */
    public static String readFileContentByStream(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            inputStreamReader = new InputStreamReader(inputStream);
            reader = new BufferedReader(inputStreamReader);
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return sbf.toString();
    }

    /**
     * 读取MultipartFile对象中的字符串内容
     * @param file
     * @return
     */
    public static String readMultipartFileStrContent(MultipartFile file) {
        String str = "";
        BufferedReader br = null;
        InputStreamReader isReader=null;
        try {
            FileInputStream is = (FileInputStream) file.getInputStream();
            isReader = new InputStreamReader(is, "GBK");
            br = new BufferedReader(isReader);
            //循环逐行读取
            while (br.ready()) {
                str+=br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isReader!=null){
                try {
                    isReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close(); //关闭流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return str;
    }


    /**
     * 文件数据写入（如果文件夹和文件不存在，则先创建，再写入）
     * @param filePath
     * @param content
     * @param flag true:如果文件存在且存在内容，则内容换行追加；false:如果文件存在且存在内容，则内容替换
     */
    public static String fileLinesWrite(String filePath, String content, boolean flag) {
        String filedo = "write";
        FileWriter fw = null;
        try {
            File file = new File(filePath);
            //如果文件夹不存在，则创建文件夹
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {//如果文件不存在，则创建文件,写入第一行内容
                file.createNewFile();
                fw = new FileWriter(file);
                filedo = "create";
            } else {//如果文件存在,则追加或替换内容
                fw = new FileWriter(file, flag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        pw.println(content);
        pw.flush();
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filedo;
    }

    /**
     * 文件数据写入（如果文件夹和文件不存在，则先创建，再写入）
     * @param filePath
     * @param contents
     * @param flag true:如果文件存在且存在内容，则内容换行追加；false:如果文件存在且存在内容，则内容替换
     */
    public static String fileLinesWrite(String filePath, List<String> contents, boolean flag) {
        String filedo = "write";
        FileWriter fw = null;
        try {
            File file = new File(filePath);
            //如果文件夹不存在，则创建文件夹
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {//如果文件不存在，则创建文件,写入第一行内容
                file.createNewFile();
                fw = new FileWriter(file);
                filedo = "create";
            } else {//如果文件存在,则追加或替换内容
                fw = new FileWriter(file, flag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        for (String content : contents) {
            pw.println(content);
        }
        pw.flush();
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filedo;
    }


    /**
     * 判断文件大小是否合法，超过指定大小返回false
     * @param file 上传文件对象
     * @param mSize  允许的大小 单位M
     */
    public static boolean fileMSizeCheck(MultipartFile file, double mSize) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        double length = file.getSize();
        if (length <= 0) {
            return false;
        }
        if ((length / (1024 * 1024)) > mSize) {
            return false;

        }
        return true;
    }


    /**
     * 判断文件大小是否合法，超过指定大小返回false
     *
     * @param file MultipartFile 文件类
     * @param size 限制大小
     * @param unit 限制单位（B,K,M,G）
     */
    public static boolean fileSizeCheck(MultipartFile file, int size, String unit) {
        // 获取文件实际大小
        long len = file.getSize();
        double fileSize = 0;
        if ("B".equalsIgnoreCase(unit)) {
            fileSize = (double) len;
        } else if ("K".equalsIgnoreCase(unit)) {
            fileSize = (double) len / 1024;
        } else if ("M".equalsIgnoreCase(unit)) {
            fileSize = (double) len / 1048576;
        } else if ("G".equalsIgnoreCase(unit)) {
            fileSize = (double) len / 1073741824;
        }
        return !(fileSize > size);
    }


    /**
     * 根据字符串内容生成指定名称的文件
     * @param jsonString 内容
     * @param filePath 存储路径
     * @param fileName 存储文件名
     * @return
     */
    public static boolean createFile(String jsonString, String filePath, String fileName) {
        // 标记文件生成是否成功
        boolean flag = true;
        // 拼接文件完整路径
        String fullPath = filePath + File.separator + fileName;
        // 生成json格式文件
        try {
            // 保证创建一个新文件
            File file = new File(fullPath);
            if (!file.getParentFile().exists()) { // 如果父目录不存在，创建父目录
                file.getParentFile().mkdirs();
            }
            if (file.exists()) { // 如果已存在,删除旧文件
                file.delete();
            }
            file.createNewFile();
            // 将格式化后的字符串写入文件
            Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            write.write(jsonString);
            write.flush();
            write.close();
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 文件下载
     * @param filePath 文件全路径
     * @param fileName 文件名
     * @param delete 下载后是否删除此文件
     */
    public static void downloadFile(String filePath, String fileName , Boolean delete, HttpServletResponse response) {
        try {
            response.reset();
            response.setContentType("application/force-download");
            response.setHeader("Content-Disposition", "attachment; fileName=" + URLEncoder.encode(fileName,"UTF-8"));
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            logger.error("下载文件失败", e);
        }
    }

    /**
     * 字符串内容下载
     * @param str
     * @param fileName
     * @param response
     */
    public  static void downloadStrFile(String str,String fileName,HttpServletResponse response){
        OutputStream os = null;
        try {
            response.reset();
            response.setContentType("application/force-download");
            response.setHeader("Content-Disposition", "attachment; fileName=" + URLEncoder.encode(fileName,"UTF-8"));
            os = response.getOutputStream();
            // 将字节流传入到响应流里,响应到浏览器
            byte[] bytes = str.getBytes("GBK");
            os.write(bytes);
            os.close();
        } catch (Exception ex) {
            logger.error("下载失败:", ex);
            throw new RuntimeException("下载失败");
        }finally {
            try {
                if (null != os) {
                    os.close();
                }
            } catch (IOException ioEx) {
                logger.error("下载失败:", ioEx);
            }
        }
    }


    /**
     * 下载文件名重新编码
     * @param response 响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException
    {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
    }
    /**
     * 下载文件名重新编码
     * @param response 响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeaderNew(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException
    {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; fileName=")
                .append(percentEncodedFileName);
        response.setHeader("Content-disposition", contentDispositionValue.toString());
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException
    {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");

    }

    /**
     * 读取文件中每行内容放到list集合中
     * @param file
     * @return
     */
    public static List<String> readFileStrContent(File file) {
        //String str = "";
        List<String> sList = new ArrayList<>();
        BufferedReader br = null;
        InputStreamReader isReader=null;
        try {
            FileInputStream is = new FileInputStream(file);
            isReader = new InputStreamReader(is, "UTF-8");
            br = new BufferedReader(isReader);
            //循环逐行读取
            while (br.ready()) {
                sList.add(br.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isReader!=null){
                try {
                    isReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close(); //关闭流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sList;
    }

    /**
     * 递归获取 某个文件夹下的所有子文件
     * 包含文件 和 文件夹
     */
    public static List<File> recursiveAll(File file,List<File> files){
        if(files==null){
            files = new ArrayList<>();
        }
        if(file==null || !file.exists()){
            return files;
        }
        if(file.isDirectory()){
            File[] files1 = file.listFiles();
            if(files1!=null){
                for (File f : files1){
                    files.add(f);
                    if(f.isDirectory()){
                        recursiveAll(f,files);
                    }
                }
            }
        }
        return files;
    }

    /**
     * 只包含文件
     * @param file
     * @param files
     * @return
     */
    public static List<File> recursiveFile(File file,List<File> files){
        List<File> files1 = recursiveAll(file,files);
        return files1.stream().filter(f->{
            return f.exists() && !f.isDirectory();
        }).collect(Collectors.toList());
    }

    /**
     * 只包含文件夹 不包含 参数传入[file]的文件夹
     * @param file
     * @param files
     * @return
     */
    public static List<File> recursiveDir(File file,List<File> files){
        List<File> files1 = recursiveAll(file,files);
        return files1.stream().filter(f->{
            return f.exists() && f.isDirectory();
        }).collect(Collectors.toList());
    }
    /**
     * 本地图片转换Base64的方法
     * @param filePath
     */
    public static String fileToBase64(String filePath) {
        byte[] data = null;
        // 读取图片字节数组
        try {
            InputStream in = new FileInputStream(filePath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data==null){
            return null;
        }
        // 返回Base64编码过的字节数组字符串
        return  new String(Base64.encodeBase64(data));
    }

    /**
     * 获取文件的最后一次更新时间
     * @return 文件毫秒值
     */
    public static long getLastUpdateFileTime(File file){
        try{
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attrs.lastModifiedTime().toMillis();
        }catch (Exception e){
            return -1L;
        }
    }

    // 目录下文件个数
    public static int getDirFile(String path){
        File file = null;
        if(StringUtils.isBlank(path) || !(file = new File(path)).exists() || !file.isDirectory()){
            return 0;
        }
        return file.listFiles().length;
    }

    // 目录大小
    public static long getDirSize(String path) {
        return FileUtils.sizeOfDirectory(new File(path));
    }

    /**
     * 字节转换 B->KB->MB->GB->TB 保留两位小数
     * @type 下标对应 0:B, 1:KB, 2:MB, 3:GB, 4:TB
     */
    public static double[] getDirSize(long size,int type) {
        BigDecimal sizeBig = new BigDecimal(size);
        BigDecimal kBig = new BigDecimal(KB);
        BigDecimal mBig = new BigDecimal(MB);
        BigDecimal gBig = new BigDecimal(GB);
        BigDecimal tBig = new BigDecimal(TB);
        BigDecimal b = BigDecimal.ZERO;
        BigDecimal k = BigDecimal.ZERO;
        BigDecimal m= BigDecimal.ZERO;
        BigDecimal g = BigDecimal.ZERO;
        BigDecimal t = BigDecimal.ZERO;
        switch (type) {
            case 1 :
                b = sizeBig.multiply(kBig);
                k = sizeBig;
                m = sizeBig.divide(mBig, 2, RoundingMode.HALF_UP);
                g = sizeBig.divide(gBig, 2, RoundingMode.HALF_UP);
                t = sizeBig.divide(tBig, 2, RoundingMode.HALF_UP);
                break;
            case 2 :
                b = sizeBig.multiply(kBig);
                k = sizeBig.multiply(mBig);
                m = sizeBig;
                g = sizeBig.divide(gBig, 2, RoundingMode.HALF_UP);
                t = sizeBig.divide(tBig, 2, RoundingMode.HALF_UP);
                break;
            case 3 :
                b = sizeBig.multiply(kBig);
                k = sizeBig.multiply(kBig);
                m = sizeBig.multiply(mBig);
                g = sizeBig;
                t = sizeBig.divide(tBig, 2, RoundingMode.HALF_UP);
                break;
            case 4 :
                b = sizeBig.multiply(kBig);
                k = sizeBig.multiply(kBig);
                m = sizeBig.multiply(mBig);
                g = sizeBig.multiply(gBig);
                t = sizeBig;
                break;
            default:
                b = sizeBig;
                k = sizeBig.divide(kBig, 2, RoundingMode.HALF_UP);
                m = sizeBig.divide(mBig, 2, RoundingMode.HALF_UP);
                g = sizeBig.divide(gBig, 2, RoundingMode.HALF_UP);
                t = sizeBig.divide(tBig, 2, RoundingMode.HALF_UP);
                break;
        }
        return new double[]{b.doubleValue(), k.doubleValue(), m.doubleValue(), g.doubleValue(), t.doubleValue()};
    }

    /**
     * 获取最优目录大小展示
     *  大于1小于1024的单位
     * @param size
     * @param type 5:TB,4:GB,3:MB,2:KB,1:B
     * @return
     */
    public static String getDirSizeStr(long size,int type){
        double[] dirSize = getDirSize(size, type);
        int j = 5;
        for (int i = 0; i < dirSize.length; i++) {
            if(dirSize[i]<1.0){
                j = i;
                break;
            }
        }
        String s;
        switch (j){
            case 5 : s = dirSize[4]+"TB"; break;
            case 4 : s = dirSize[3]+"GB" ; break;
            case 3 : s = dirSize[2]+"MB" ; break;
            case 2 : s = dirSize[1]+"kB" ; break;
            default: s = dirSize[0]+"B" ; break;
        }
        return s;
    }


//    public static void main(String[] args) {
//        double[] dirSize = getDirSize(1225, 0);
//        for (int i = 0; i < dirSize.length; i++) {
//            System.out.println(dirSize[i]);
//        }
//        System.out.println(getDirSizeStr(1225, 0));
//    }

}

