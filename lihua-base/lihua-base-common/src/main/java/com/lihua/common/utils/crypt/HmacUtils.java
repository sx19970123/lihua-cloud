package com.lihua.common.utils.crypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class HmacUtils {

    /**
     * 生成hmac签名
     */
    public static String hmacSha256(String key, String data) {
        try {
            String algorithm =  "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), algorithm);

            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes());

            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
