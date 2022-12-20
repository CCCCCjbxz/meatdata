import com.alibaba.fastjson.JSONObject;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author zhengwen
 **/

public class ImgTestCode {
    public static void main(String[] args) throws Exception {

        File file = new File("/Users/zuijiaoshangyang/Desktop/IMG_1361.jpg");
        readImageInfo(file);
    }

    /**
     * 提取照片里面的信息
     *
     * @param file 照片文件
     * @throws ImageProcessingException
     * @throws Exception
     */
    private static void readImageInfo(File file) throws ImageProcessingException, Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);

        System.out.println("---打印全部详情---");
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                System.out.format("[%s] - %s = %s\n",
                        directory.getName(), tag.getTagName(), tag.getDescription());
            }
            if (directory.hasErrors()) {
                for (String error : directory.getErrors()) {
                    System.err.format("ERROR: %s", error);
                }
            }
        }

        System.out.println("--打印常用信息---");

        Double lat = null;
        Double lng = null;
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName();  //标签名
                String desc = tag.getDescription(); //标签信息
                if (tagName.equals("Image Height")) {
                    System.err.println("图片高度: " + desc);
                } else if (tagName.equals("Image Width")) {
                    System.err.println("图片宽度: " + desc);
                } else if (tagName.equals("Date/Time Original")) {
                    System.err.println("拍摄时间: " + desc);
                } else if (tagName.equals("GPS Latitude")) {
                    System.err.println("纬度 : " + desc);
                    System.err.println("纬度(度分秒格式) : " + pointToLatlong(desc));
                    lat = latLng2Decimal(desc);
                } else if (tagName.equals("GPS Longitude")) {
                    System.err.println("经度: " + desc);
                    System.err.println("经度(度分秒格式): " + pointToLatlong(desc));
                    lng = latLng2Decimal(desc);
                }
            }
        }
        System.err.println("--经纬度转地址--");
        //经纬度转地主使用百度api
        convertGpsToLoaction(lat, lng);


    }

    /**
     * 经纬度格式  转换为  度分秒格式 ,如果需要的话可以调用该方法进行转换
     *
     * @param point 坐标点
     * @return
     */
    public static String pointToLatlong(String point) {
        Double du = Double.parseDouble(point.substring(0, point.indexOf("°")).trim());
        Double fen = Double.parseDouble(point.substring(point.indexOf("°") + 1, point.indexOf("'")).trim());
        Double miao = Double.parseDouble(point.substring(point.indexOf("'") + 1, point.indexOf("\"")).trim());
        Double duStr = du + fen / 60 + miao / 60 / 60;
        return duStr.toString();
    }

    /***
     * 经纬度坐标格式转换（* °转十进制格式）
     * @param gps
     */
    public static double latLng2Decimal(String gps) {
        String a = gps.split("°")[0].replace(" ", "");
        String b = gps.split("°")[1].split("'")[0].replace(" ", "");
        String c = gps.split("°")[1].split("'")[1].replace(" ", "").replace("\"", "");
        double gps_dou = Double.parseDouble(a) + Double.parseDouble(b) / 60 + Double.parseDouble(c) / 60 / 60;
        return gps_dou;
    }

    /**
     * api_key：注册的百度api的key
     * coords：经纬度坐标
     * http://api.map.baidu.com/reverse_geocoding/v3/?ak="+api_key+"&output=json&coordtype=wgs84ll&location="+coords
     * <p>
     * 经纬度转地址信息
     *
     */
    private static void convertGpsToLoaction(double gps_latitude, double gps_longitude) throws IOException {
        String apiKey = "YNxcSCAphFvuPD4LwcgWXwC3SEZZc7Ra";

        String res = "";
        String url = "http://api.map.baidu.com/reverse_geocoding/v3/?ak=" + apiKey + "&output=json&coordtype=wgs84ll&location=" + (gps_latitude + "," + gps_longitude);
        System.err.println("【url】" + url);

        res=acquire(url);
        JSONObject object = JSONObject.parseObject(res);
        if (object.containsKey("result")) {
            JSONObject result = object.getJSONObject("result");
            if (result.containsKey("addressComponent")) {
                JSONObject address = object.getJSONObject("result").getJSONObject("addressComponent");
                System.err.println("拍摄地点：" + address.get("country") + " " + address.get("province") + " " + address.get("city") + " " + address.get("district") + " "
                        + address.get("street") + " " + result.get("formatted_address") + " " + result.get("business"));
            }
        }

    }


    //public static void convertGpsToLoaction(Double lat,Double lng){
    //    String apiKey = "YNxcSCAphFvuPD4LwcgWXwC3SEZZc7Ra";
    //    String location = lat+","+lng;
    //    String url="http://api.map.baidu.com/reverse_geocoding/v3/?ak="+apiKey+"&output=json&coordtype=bd09ll&location="+location;
    //    String res=acquire(url);
    //
    //    //获取详细地址
    //    //   String addressLocation= JSON.parseObject(res).getJSONObject("result").getString("formatted_address");
    //    //获取省份
    //    String province="";
    //    if(res.contains("province")){
    //        province= JSON.parseObject(res).getJSONObject("result").getJSONObject("addressComponent").getString("province");
    //    }
    //    //获取详细地址
    //    String address="";
    //
    //    // System.out.println(addressLocation);
    //    System.out.println(province);
    //}

    public static String acquire(String url){
        CloseableHttpClient httpClient= HttpClientBuilder.create().build();
        HttpGet httpGet=new HttpGet(url);
        CloseableHttpResponse response=null;
        try {
            response=httpClient.execute(httpGet);
            HttpEntity responseEntity=response.getEntity();
            if (responseEntity!=null){
                return EntityUtils.toString(responseEntity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(httpClient!=null){
                    httpClient.close();
                }
                if (response!=null){
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}