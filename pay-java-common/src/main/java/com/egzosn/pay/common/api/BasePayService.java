package com.egzosn.pay.common.api;

import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.bean.TransferOrder;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.http.HttpRequestTemplate;
import com.egzosn.pay.common.util.sign.SignUtils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 支付基础服务
 * @author: egan
 *  <pre>
 *      email egzosn@gmail.com
 *      date 2017/3/5 20:36
 *   </pre>
 */
public abstract class BasePayService implements PayService {

    protected PayConfigStorage payConfigStorage;

    protected HttpRequestTemplate requestTemplate;
    protected int retrySleepMillis = 1000;

    protected int maxRetryTimes = 5;

    /**
     * 设置支付配置
     * @param payConfigStorage 支付配置
     */
    @Override
    public BasePayService setPayConfigStorage(PayConfigStorage payConfigStorage) {
        this.payConfigStorage = payConfigStorage;
        return this;
    }

    @Override
    public PayConfigStorage getPayConfigStorage() {
        return payConfigStorage;
    }
    @Override
    public HttpRequestTemplate getHttpRequestTemplate() {
        return requestTemplate;
    }

    /**
     * 设置并创建请求模版， 代理请求配置这里是否合理？？，
     * @param configStorage http请求配置
     * @return 支付服务
     */
    @Override
    public BasePayService setRequestTemplateConfigStorage(HttpConfigStorage configStorage) {
        this.requestTemplate = new HttpRequestTemplate(configStorage);
        return this;
    }


    public BasePayService(PayConfigStorage payConfigStorage) {
        this(payConfigStorage, null);
    }

    public BasePayService(PayConfigStorage payConfigStorage, HttpConfigStorage configStorage) {
        setPayConfigStorage(payConfigStorage);
        setRequestTemplateConfigStorage(configStorage);
    }


    /**
     *  Generate a Base64 encoded String from  user , password
     * @param user 用户名
     * @param password 密码
     * @return authorizationString
     */
    protected String authorizationString(String user, String password) {
        String base64ClientID = null;
        try {
            base64ClientID = com.egzosn.pay.common.util.sign.encrypt.Base64.encode(String.format("%s:%s", user , password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return base64ClientID;
    }

    /**
     * 创建签名
     *
     * @param content           需要签名的内容
     * @param characterEncoding 字符编码
     * @return 签名
     */
    @Override
    public String createSign(String content, String characterEncoding) {

        return  SignUtils.valueOf(payConfigStorage.getSignType()).createSign(content, payConfigStorage.getKeyPrivate(),characterEncoding);
    }
    /**
     * 创建签名
     *
     * @param content           需要签名的内容
     * @param characterEncoding 字符编码
     * @return 签名
     */
    @Override
    public String createSign(Map<String, Object> content, String characterEncoding) {
        return  SignUtils.valueOf(payConfigStorage.getSignType()).sign(content, payConfigStorage.getKeyPrivate(),characterEncoding);
    }

    /**
     * 将请求参数或者请求流转化为 Map
     *
     * @param parameterMap 请求参数
     * @param is           请求流
     * @return 获得回调的请求参数
     */
    @Override
    public Map<String, Object> getParameter2Map (Map<String, String[]> parameterMap, InputStream is) {

        Map<String, Object> params = new TreeMap<String,Object>();
        for (Iterator iter = parameterMap.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = parameterMap.get(name);
            String valueStr = "";
            for (int i = 0,len =  values.length; i < len; i++) {
                valueStr += (i == len - 1) ?  values[i] : values[i] + ",";
            }
            if (!valueStr.matches("\\w+")){
                try {
                    if(valueStr.equals(new String(valueStr.getBytes("iso8859-1"), "iso8859-1"))){
                        valueStr=new String(valueStr.getBytes("iso8859-1"), payConfigStorage.getInputCharset());
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            params.put(name, valueStr);
        }
        return params;
    }


    /**
     * 交易查询接口，带处理器
     * @param tradeNo    支付平台订单号
     * @param outTradeNo 商户单号
     * @param callback 处理器
     * @param <T> 返回类型
     * @return  返回查询回来的结果集
     */
    @Override
    public <T> T query(String tradeNo, String outTradeNo, Callback<T> callback) {

        return callback.perform(query(tradeNo, outTradeNo));
    }

    /**
     * 交易关闭接口
     *
     * @param tradeNo    支付平台订单号
     * @param outTradeNo 商户单号
     * @param callback 处理器
     * @param <T> 返回类型
     * @return 返回支付方交易关闭后的结果
     */
    @Override
    public <T> T close(String tradeNo, String outTradeNo, Callback<T> callback) {
        return  callback.perform(close(tradeNo, outTradeNo));
    }

    /**
     * 退款
     *
     * @param tradeNo      支付平台订单号
     * @param outTradeNo   商户单号
     * @param refundAmount 退款金额
     * @param totalAmount  总金额
     * @param callback     处理器
     * @param <T>          返回类型
     *
     * @return 处理过后的类型对象， 返回支付方申请退款后的结果
     * @see #refund(RefundOrder, Callback)
     */
    @Deprecated
    @Override
    public <T> T refund(String tradeNo, String outTradeNo, BigDecimal refundAmount, BigDecimal totalAmount, Callback<T> callback) {

        return callback.perform(refund(new RefundOrder(tradeNo, outTradeNo, refundAmount, totalAmount)));
    }

    /**
     * 申请退款接口
     *
     * @param refundOrder   退款订单信息
     * @return 返回支付方申请退款后的结果
     * @param callback 处理器
     * @param <T> 返回类型
     * @return 返回支付方申请退款后的结果
     */
    @Override
    public <T> T refund(RefundOrder refundOrder, Callback<T> callback) {

        return  callback.perform(refund(refundOrder));
    }


    /**
     * 查询退款
     *
     * @param tradeNo    支付平台订单号
     * @param outTradeNo 商户单号
     * @param callback   处理器
     * @param <T>        返回类型
     *
     * @return 处理过后的类型对象，返回支付方查询退款后的结果
     */
    @Override
    public <T> T refundquery(String tradeNo, String outTradeNo, Callback<T> callback) {
        return callback.perform(refundquery(tradeNo, outTradeNo));
    }

    /**
     * 目前只支持日账单
     *
     * @param billDate 账单时间：具体请查看对应支付平台
     * @param billType 账单类型，具体请查看对应支付平台
     * @param callback 处理器
     * @param <T>      返回类型
     *
     * @return 返回支付方下载对账单的结果
     */
    @Override
    public <T> T downloadbill(Date billDate, String billType, Callback<T> callback) {
        return callback.perform(downloadbill(billDate, billType));
    }

    /**
     * @param tradeNoOrBillDate 支付平台订单号或者账单类型， 具体请 类型为{@link String }或者 {@link Date }，类型须强制限制，类型不对应则抛出异常{@link PayErrorException}
     * @param outTradeNoBillType      商户单号或者 账单类型
     * @param transactionType         交易类型
     * @param callback                处理器
     * @param <T>                     返回类型
     * @return 返回支付方对应接口的结果
     */
    @Override
    public <T>T secondaryInterface(Object tradeNoOrBillDate, String outTradeNoBillType, TransactionType transactionType, Callback<T> callback){
        return callback.perform(secondaryInterface(tradeNoOrBillDate, outTradeNoBillType, transactionType));
    }

    /**
     * 转账
     *
     * @param order    转账订单
     * @param callback 处理器
     *
     * @return 对应的转账结果
     */
    @Override
    public <T> T transfer(TransferOrder order, Callback<T> callback) {
        return callback.perform(transfer(order));
    }


    /**
     * 转账
     *
     * @param order 转账订单
     *
     * @return 对应的转账结果
     */
    @Override
    public Map<String, Object> transfer(TransferOrder order) {
        return new HashMap<>(0);
    }

    /**
     * 转账查询
     *
     * @param outNo 商户转账订单号
     * @param tradeNo 支付平台转账订单号
     *
     * @return 对应的转账订单
     */
    @Override
    public Map<String, Object> transferQuery(String outNo, String tradeNo){
        return new HashMap<>(0);
    }

    /**
     * 转账查询
     *
     * @param outNo 商户转账订单号
     * @param tradeNo 支付平台转账订单号
     * @param callback 处理器
     * @param <T> 返回类型
     * @return 对应的转账订单
     */
    @Override
    public <T>T transferQuery(String outNo, String tradeNo, Callback<T> callback){
        return callback.perform(transferQuery(outNo, tradeNo));
    }
}
