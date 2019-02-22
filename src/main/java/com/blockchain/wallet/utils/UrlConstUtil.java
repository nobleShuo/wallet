package com.blockchain.wallet.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author QiShuo
 * @version 1.0
 * @create 2018/11/26 6:22 PM
 */
public class UrlConstUtil {
    /**
     * 私有链eth节点的服务器
//     */

    /**
     * 获取私钥的端口+地址
     */
    public static final String PRIVATE_KEY_PORT = "8001/getPrivateKey";
    /**
     * 私有链节点地址
     */
    // public static final String ETH_PRIVATE_NODE_URL = "";
    /**
     * node服务获取地址私钥
     */
    // public static final String NODE_SERVICE_GET_PRIVATE_KEY = "";
    /**
     * node服务获取token余额
     */
    public static final String NODE_SERVICE_GET_TOKEN_BALANCE = "http://localhost:8002/getBalance";
    /**
     * 公有链节点地址(使用的nodeJS服务中的地址)
     */
    public static final String ETH_PUBLIC_NODE_URL = "";

}
