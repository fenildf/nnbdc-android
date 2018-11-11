package com.nb.nnbdc.android;

import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.StringUtils;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import beidanci.vo.DictGroupVo;
import beidanci.vo.GetGameHallDataResult;
import beidanci.vo.GetWordResult;
import beidanci.vo.PagedResults;
import beidanci.vo.RawWordVo;
import beidanci.vo.Result;
import beidanci.vo.SearchWordResult;
import beidanci.vo.SelectedDictVo;
import beidanci.vo.SentenceVo;
import beidanci.vo.UserVo;

public class HttpClient {

    private String sessionId;
    private MyApp appContext;

    public HttpClient(MyApp appContext) {
        this.appContext = appContext;
    }

    public int getFileSize(String url) {
        URLConnection conn = null;
        try {
            URL url_ = new URL(url);
            conn = url_.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }

    public int getFileSize2(String url) throws IOException {
        try {
            String responseStr = "";

            URL postUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) (postUrl.openConnection());
            con.setRequestMethod("HEAD");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Accept-Language", "utf-8;zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
            con.setInstanceFollowRedirects(false);

            InputStream is = con.getInputStream();

            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                responseStr = responseStr + line;
            }

            int contentLen = -1;
            List<String> values = con.getHeaderFields().get("Content-Length");
            if (null != values && values.size() > 0) {
                contentLen = Integer.parseInt(values.get(0));
            }

            Log.i("", "接口：" + url + ",返回结果：" + responseStr);

            return contentLen;
        } catch (SocketTimeoutException e) {
            Util.showHintMsg("网络中断", appContext.getHintHandler());
            return -1;
        }
    }

    public String sendAjax(String url, String data, String contentType, String requestMethod, int timeout) throws IOException {
        try {
            String responseStr = "";

            if (requestMethod.equals("GET") && data != null) {
                url = url + "?" + data;
            }
            URL postUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) (postUrl.openConnection());
            con.setRequestMethod(requestMethod);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            con.setRequestProperty("Accept-Language", "utf-8;zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
            if (!StringUtils.isEmpty(data) && !requestMethod.equals("GET")) {
                // con.setRequestProperty("Content-Length", String.valueOf(data.length()));
            }
            con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            assert (contentType != null);
            con.setRequestProperty("Content-Type", contentType);
            con.setInstanceFollowRedirects(false);

            // 获取sessionId
            if (!StringUtils.isEmpty(sessionId)) {
                con.setRequestProperty("Cookie", sessionId);
            }

            // 写HTTP body
            if (requestMethod.equals("POST")) {
                OutputStream os = con.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "utf-8");
                if (data != null) {
                    osw.write(data);
                }
                osw.flush();
                osw.close();
            }

            InputStream is = con.getInputStream();
            // 处理重定向应答(之所以自己处理重定向，是为了使请求重定向资源时带上"X-Requested-With"头部)
            if (con.getResponseCode() == 302) {
                String redirectUrl = con.getHeaderField("Location");
                return sendAjax(redirectUrl, null, contentType, requestMethod, timeout);
            }

            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                responseStr = responseStr + line;
            }

            // 保存sessionID
            List<String> cookeList = con.getHeaderFields().get("Set-Cookie");
            if (null != cookeList && cookeList.size() > 0) {
                sessionId = getSessionId(cookeList.get(0));
            }
            Log.i("", "接口：" + url + ",返回结果：" + responseStr);

            if ("login".equals(con.getHeaderField("command"))) {
                Util.showHintMsg("请重新登录", appContext.getHintHandler());
                Util.reLogin(appContext.getReLoginHandler());
                return null;
            } else {
                return responseStr;
            }
        } catch (SocketTimeoutException e) {
            Util.showHintMsg("网络中断", appContext.getHintHandler());
            return null;
        }
    }


    public String sendAjax(String url, Map<String, Object> parameterMap, String requestMethod, int timeout) throws IOException {
        String parameter = null;
        /** 拼接参数 **/
        if (parameterMap != null) {
            StringBuffer parameterBuffer = new StringBuffer();
            Iterator<String> iterator = parameterMap.keySet().iterator();
            String key = null;
            String value = null;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                if (parameterMap.get(key) != null) {
                    value = parameterMap.get(key).toString();
                } else {
                    value = "";
                }

                value = URLEncoder.encode(value, "utf-8");
                parameterBuffer.append(key).append("=").append(value);
                if (iterator.hasNext()) {
                    parameterBuffer.append("&");
                }
            }

            parameter = parameterBuffer.toString();
        }

        return sendAjax(url, parameter, "application/x-www-form-urlencoded;charset=utf-8", requestMethod, timeout);
    }

    public String getSessionId(String original_SetCookie) {
        int index = original_SetCookie.indexOf(";");
        String finalStr = original_SetCookie.substring(0, index);
        return finalStr;
    }


    public Object[] getNewVersionCodeAndName() throws IOException, JSONException {
        String serviceUrl = appContext.getString(R.string.updateUrl);
        String result = sendAjax(serviceUrl, null, "GET", 10000);
        JSONArray array = new JSONArray(result);
        if (array.length() > 0) {
            JSONObject obj = array.getJSONObject(0);
            Object[] codeAndName = new Object[2];
            codeAndName[0] = Integer.parseInt(obj.getString("verCode"));
            codeAndName[1] = obj.getString("verName");
            return codeAndName;

        }
        return null;
    }

    public GetWordResult getNextWord(boolean isAnswerCorrect, boolean isWordMastered, boolean shouldEnterNextPage) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getWords.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("isAnswerCorrect", isAnswerCorrect);
        requestParams.put("isWordMastered", isWordMastered);
        requestParams.put("shouldEnterNextStage", shouldEnterNextPage);

        String result = sendAjax(serviceUrl, requestParams, "GET", 30000);
        Type objectType = new TypeToken<GetWordResult>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public GetGameHallDataResult getGameHallData() throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getGameHallData.do";

        String result = sendAjax(serviceUrl, null, "GET", 30000);
        Type objectType = new TypeToken<GetGameHallDataResult>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }


    public SearchWordResult searchWord(String spell) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/searchWord.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("word", spell);

        String result = sendAjax(serviceUrl, requestParams, "GET", 30000);
        Type objectType = new TypeToken<SearchWordResult>() {
        }.getType();
        SearchWordResult searchWordResult = Util.getGsonBuilder().create().fromJson(result, objectType);
        if (searchWordResult != null && searchWordResult.getWord() != null) {
            searchWordResult.getWord().setSentences(searchWordResult.getSentencesWithUGC());
        }
        return searchWordResult;
    }

    public Result addRawWord(String spell) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/addRawWord.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("spell", spell);
        requestParams.put("addManner", "逐个添加");

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        return Util.parseResult(result);
    }


    public Result handSentenceUgcChinese(int chineseId) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/handSentenceDiyItem.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("id", chineseId);

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        return Util.parseResult(result);
    }

    public Result footSentenceUgcChinese(int chineseId) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/footSentenceDiyItem.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("id", chineseId);

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        return Util.parseResult(result);
    }

    public Result deleteSentenceUgcChinese(int chineseId) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/deleteSentenceDiyItem.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("id", chineseId);

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        return Util.parseResult(result);
    }

    public SentenceVo getSentence(int sentenceId) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getSentence.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("sentenceId", sentenceId);

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        Type objectType = new TypeToken<SentenceVo>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }


    public PagedResults<RawWordVo> getRawWordsForAPage(int pageNo, int pageSize) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getRawWordsForAPage.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("pageNo", pageNo);
        requestParams.put("pageSize", pageSize);

        String result = sendAjax(serviceUrl, requestParams, "POST", 30000);
        Type objectType = new TypeToken<PagedResults<RawWordVo>>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public Result deleteRawWord(int id) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/deleteRawWord.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("id", id);

        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);
        return Util.parseResult(result);
    }

    public List<DictGroupVo> getDictGroups() throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getDictGroups.do";
        String result = sendAjax(serviceUrl, null, "POST", 30000);
        Type objectType = new TypeToken<List<DictGroupVo>>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public List<SelectedDictVo> getSelectedDicts() throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getSelectedDicts.do";
        String result = sendAjax(serviceUrl, null, "POST", 30000);
        Type objectType = new TypeToken<List<SelectedDictVo>>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public UserVo getLoggedInUser() throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/getLoggedInUser.do";

        String result = sendAjax(serviceUrl, null, "GET", 5000);
        Type objectType = new TypeToken<UserVo>() {
        }.getType();
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public Result<SentenceVo> saveSentenceDiyItem(int sentenceId, String chinese) throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/saveSentenceDiyItem.do";
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("sentenceId", sentenceId);
        requestParams.put("chinese", chinese);
        String result = sendAjax(serviceUrl, requestParams, "POST", 5000);

        Type objectType = new TypeToken<Result<SentenceVo>>() {
        }.getType();
        result = Util.preHandleResult(result);
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public Result<Integer> saveDakaRecord() throws IOException {
        String serviceUrl = appContext.getString(R.string.service_url) + "/saveDakaRecord.do";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", "好好学习，天天向上");
        String result = sendAjax(serviceUrl, parameters, "POST", 5000);

        Type objectType = new TypeToken<Result<Integer>>() {
        }.getType();
        result = Util.preHandleResult(result);
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }
}
