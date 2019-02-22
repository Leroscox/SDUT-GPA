package ero.hentai.sdut.gpa.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ero.hentai.sdut.gpa.service.SDUTGPAService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author MiK
 * @version 0.0.2
 * @since JDK1.8
 */
@Service("sdutGPAServiceImpl")
public class SDUTGPAServiceImpl implements SDUTGPAService {
    private Logger log = LoggerFactory.getLogger(SDUTGPAServiceImpl.class);
    private OkHttpClient client = new OkHttpClient();
    private ThreadLocal<Gson> gsonThreadLocal = ThreadLocal.withInitial(Gson::new);

    @Override
    public Map compute(int year, String JSESSIONID) throws IOException {
        Map<String, Map> rawsMap = new HashMap<>();
        String rawData;
        Map rawMap;
        for (int i = 0; i < 6; i++) {
            rawData = getJSONData(String.valueOf(year + i), JSESSIONID);
            log.debug("rawData -> {}", rawData);
            rawMap = gsonThreadLocal.get().fromJson(rawData, new TypeToken<Map>() {
            }.getType());
            if (null != rawMap) {
                rawsMap.put(String.valueOf(year + i), rawMap);
            }
        }
        return doCompute(rawsMap);
    }

    private String getJSONData(String year, String JSESSIONID) throws IOException {
        // Generate by postman
        //---
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded;charset=utf-8");
//        RequestBody body = RequestBody.create(mediaType, "xnm=2015&xqm=&_search=false&nd=1550645385377&queryModel.showCount=5000&queryModel.currentPage=1&queryModel.sortName=&queryModel.sortOrder=asc&time=14");
        RequestBody body = RequestBody.create(mediaType, "xnm=" + year + "&xqm=&_search=false&nd=1550645385377&queryModel.showCount=5000&queryModel.currentPage=1&queryModel.sortName=&queryModel.sortOrder=asc&time=13");
        Request request = new Request.Builder()
//                .url("http://211.64.28.123/jwglxt/cjcx/cjcx_cxDgXscj.html?doType=query&gnmkdm=N305005")
                .url("http://211.64.28.123/jwglxt/cjcx/cjcx_cxDgXscj.html?doType=query")
                .post(body)
                .addHeader("host", "211.64.28.123")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0")
                .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("accept-language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
//                .addHeader("accept-encoding", "gzip, deflate")
                .addHeader("accept-encoding", "deflate")
//                .addHeader("referer", "http://211.64.28.123/jwglxt/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default&su=15110572122")
                .addHeader("referer", "http://211.64.28.123/jwglxt/cjcx/cjcx_cxDgXscj.html?layout=default&su=15110572122")
//                .addHeader("content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=utf-8")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("content-length", "149")
                .addHeader("connection", "keep-alive")
//                .addHeader("cookie", "JSESSIONID=A46761E9C3CE1B969AF42BB58F6729B0")
                .addHeader("cookie", "JSESSIONID=" + JSESSIONID)
                .addHeader("pragma", "no-cache")
                .addHeader("cache-control", "no-cache")
                .build();
        Response response = client.newCall(request).execute();
        // ---
        if (null == response.body()) {
            return "{}";
        }

        String tmp;
        StringBuilder respJSONStr = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            while ((tmp = br.readLine()) != null) {
                respJSONStr.append(tmp);
            }
        }
        return respJSONStr.toString();
    }

    @NotNull
    private Map<String, Object> doCompute(Map<String, Map> rawsMap) {
        Map<String, Object> rsMap = new HashMap<>();
        rsMap.put("raws", rawsMap);
        // --
        // 在同一科目多个成绩中选出得分最高的一次作为有效记录
        // 键为课程ID，值为存储单条成绩记录的 Map 对象
        Map<String, Map> uniqueMap = new HashMap<>();
        // 同 uniqueMap ，但不包括公选课
        Map<String, Map> effectiveMap = new HashMap<>();
        // 遍历原始数据并取出成绩列表进一步遍历
        for (Map rawMap : rawsMap.values()) {
            Object tmp = rawMap.get("items");
            if (!(tmp instanceof List)) {
                continue;
            }
            List items = (List) tmp;
            // 遍历成绩列表
            for (Object item : items) {
                if (!(item instanceof Map)) {
                    break;
                }
                Map itemMap = (Map) item;
                Object key = itemMap.get("kch_id");
                Object type = itemMap.get("kcxzmc");
                if (!(key instanceof String) || !(type instanceof String)) {
                    throw new RuntimeException("Data Error!");
                }
                // 定义为 uniqueMap 与 effectiveMap 的 compute 方法传递的行为化参数
                // 比较并选择百分制成绩较高的一项
                BiFunction<String, Map, Map> biFunction = (k, ov) -> {
                    if (null == ov) {
                        return itemMap;
                    }
                    log.debug("oldBfzcj={}, newBfzcj={}", ov.get("bfzcj"), itemMap.get("bfzcj"));
                    BigDecimal oldBfzcj = new BigDecimal(String.valueOf(ov.get("bfzcj")));
                    BigDecimal newBfzcj = new BigDecimal(String.valueOf(itemMap.get("bfzcj")));
                    return newBfzcj.compareTo(oldBfzcj) > 0 ? itemMap : ov;
                };
                uniqueMap.compute((String) key, biFunction);
                if (!"公选课".equals(type)) {
                    effectiveMap.compute((String) key, biFunction);
                }
            }
        }
        rsMap.put("uniqueMap", uniqueMap);
        rsMap.put("effectiveMap", effectiveMap);
        // --
        // 为避免精度问题使用 BigDecimal 进行精确运算
        BigDecimal bfzcjMulXfSum = new BigDecimal(0);
        BigDecimal xfSum = new BigDecimal(0);
        for (Map effectiveItem : effectiveMap.values()) {
            BigDecimal bfzcj = new BigDecimal(String.valueOf(effectiveItem.get("bfzcj")));
            BigDecimal xf = new BigDecimal(String.valueOf(effectiveItem.get("xf")));
            // 百分制成绩低于 60 视作无效成绩，不计入总绩点（分子），但计入总学分（分母）
            if (bfzcj.compareTo(BigDecimal.valueOf(60)) < 0) {
                bfzcj = BigDecimal.valueOf(0);
            }
            // 精确计算总绩点与总学分
            BigDecimal bfzcjMulXf = bfzcj.multiply(xf);
            bfzcjMulXfSum = bfzcjMulXfSum.add(bfzcjMulXf);
            xfSum = xfSum.add(xf);
        }
        rsMap.put("bfzcjMulXfSum", bfzcjMulXfSum.doubleValue());
        rsMap.put("xfSum", xfSum.doubleValue());
        log.debug("bfzcjMulXfSum={}, xfSum={}", bfzcjMulXfSum, xfSum);
        // --
        if (xfSum.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal gpa = bfzcjMulXfSum.divide(xfSum, 8, BigDecimal.ROUND_DOWN);
            rsMap.put("GPA", gpa.toString());
        } else {
            rsMap.put("GPA", "-");
        }
        return rsMap;
    }
}
