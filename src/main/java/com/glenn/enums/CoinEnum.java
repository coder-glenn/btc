package com.glenn.enums;

import javax.persistence.criteria.CriteriaBuilder;

public enum CoinEnum {

    BTC(1),USDT(2),ETH(3),HT(4),EOS(5),XRP(7);

    private Integer id;

    private CoinEnum(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return this.id;
    }

}
