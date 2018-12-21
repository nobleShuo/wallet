package com.blockchain.wallet.service.impl;

import com.blockchain.wallet.entity.*;
import com.blockchain.wallet.enums.*;
import com.blockchain.wallet.service.*;
import com.blockchain.wallet.utils.CurrencyMathUtil;
import com.blockchain.wallet.utils.UrlConstUtil;
import com.blockchain.wallet.utils.Web3jUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author QiShuo
 * @version 1.0
 * @create 2018/9/13 上午11:07
 */
@Service
@Slf4j
public class WalletServiceImpl implements IWalletService {
    @Resource
    private Web3jUtil web3jUtil;
    @Resource
    private IAddressService addressService;
    @Resource
    private ITransactionHistoryService transactionHistoryService;
    @Resource
    private ITransactionOrderService transactionOrderService;

    @Resource
    private IWithdrawPublicService withdrawPublicService;

    @Value("${privateEth.gas-limit}")
    private String gasLimit;
    @Value("${privateEth.gas-price}")
    private String gasPrice;

    @Override
    public String createAddr() {
        AddressEntity addressEntity = web3jUtil.createAddr(UrlConstUtil.ETH_PRIVATE_NODE_URL);
        if (null == addressEntity) {
            log.error("Failed to create user address");
            return null;
        }
        addressService.insertAddressEntity(addressEntity);
        return addressEntity.getWalletAddress();
    }

    @Override
    public AddressEntity queryAddressEntity(String addr) {
        return addressService.findAddress(addr);
    }

    @Override
    public String buildTransaction(TransactionParamEntity txParam) {
        TransactionOrderEntity transactionOrder;
        AddressEntity fromAddressEntity;
        if (StringUtils.isEmpty(txParam.getFrom())) {
            //使用系统默认的手续费进行转账
            String fee = CurrencyMathUtil.multiply(this.gasLimit, this.gasPrice);
            txParam.setFee(fee);
            transactionOrder = getTransactionOrder(txParam, TransactionOrderTypeEnum.PRIVATE_SYSTEM_TO_PRIVATE_USER);
            //添加交易订单
            transactionOrderService.insertTransactionOrder(transactionOrder);
            return transactionOrder.getTransactionId();
        }
        fromAddressEntity = addressService.findAddress(txParam.getFrom());
        if (Objects.isNull(fromAddressEntity)) {
            log.info("From address does not exist ,address:{}", txParam.getFrom());
            return null;
        }
        String balance = fromAddressEntity.getBalance();
        if (CurrencyMathUtil.compare(balance, CurrencyMathUtil.add(txParam.getValue(), txParam.getFee())) > -1) {
            if (TransactionOrderTypeEnum.PRIVATE_TO_PRIVATE.getCode().equals(txParam.getType())) {
                AddressEntity toAddressEntity = addressService.findAddress(txParam.getTo());
                if (Objects.isNull(toAddressEntity)) {
                    log.info("To address does not exist,address:{}", txParam.getTo());
                    return null;
                }
                transactionOrder = getTransactionOrder(txParam, TransactionOrderTypeEnum.PRIVATE_TO_PRIVATE);
            } else {
                transactionOrder = getTransactionOrder(txParam, TransactionOrderTypeEnum.PRIVATE_TO_PUBLIC);
            }
            //添加交易订单
            transactionOrderService.insertTransactionOrder(transactionOrder);
            return transactionOrder.getTransactionId();
        } else {
            log.info("The address balance is insufficient:{}", txParam.getFrom());
            return null;
        }
    }


    @Override
    public List<TransactionHistoryEntity> findAddressOutTransaction(String addr) {
        return transactionHistoryService.findAddressTransaction(addr, TransactionTypeEnum.TRANSACTION_OUT.getType());
    }

    @Override
    public List<TransactionHistoryEntity> findAddressInTransaction(String addr) {
        return transactionHistoryService.findAddressTransaction(addr, TransactionTypeEnum.TRANSACTION_IN.getType());
    }

    @Override
    public List<TransactionOrderEntity> getTransactionOrderList(Integer state, Integer type) {
        return transactionOrderService.findStateAndType(state, type);
    }

    @Override
    public String handlerPublicWithdrawOrder(String transactionId, Integer state) {
        transactionOrderService.updateTransactionOrderEntity(transactionId, state);
        return transactionId;
    }

    @Override
    public List<TransactionOrderEntity> getAddressWithdrawOrderList(String fromAddress) {
        return transactionOrderService.findFromAddress(fromAddress, TransactionOrderTypeEnum.PRIVATE_TO_PUBLIC.getCode());
    }

    @Override
    public WithdrawPublicEntity getWithdrawDetail(String transactionId) {
        return withdrawPublicService.findWithdrawDetail(transactionId);
    }

    @Override
    public String getPrivateChainTransactionHash(String transactionId) {
        return Optional.ofNullable(transactionHistoryService.findTransactionHistory(transactionId)).map(TransactionHistoryEntity::getTransactionHash).orElse(null);
    }

    /**
     * 获取交易订单
     *
     * @param txParam 交易订单参数
     * @param txType  提现交易类型
     * @return
     */
    private TransactionOrderEntity getTransactionOrder(TransactionParamEntity txParam, TransactionOrderTypeEnum txType) {

        TransactionOrderEntity txOrder = new TransactionOrderEntity();
        String transactionId = txParam.getTransactionId();
        if (StringUtils.isEmpty(transactionId)) {
            //生成交易订单ID
            transactionId = UUID.randomUUID().toString().replace("-", "");
        }
        txOrder.setTransactionId(transactionId);
        txOrder.setFromAddr(txParam.getFrom());
        txOrder.setToAddr(txParam.getTo());
        txOrder.setValue(txParam.getValue());
        txOrder.setMemo(txParam.getMemo());
        if (StringUtils.isEmpty(txParam.getFee())) {
            //给定一个默认手续费
            String fee = CurrencyMathUtil.multiply(this.gasLimit, this.gasPrice);
            txOrder.setFee(fee);
        } else {
            txOrder.setFee(txParam.getFee());
        }
        txOrder.setType(txType.getCode());
        txOrder.setState(TransactionOrderStateEnum.INIT.getCode());

        return txOrder;
    }

}