package com.glenn.scheduler;

import com.glenn.constant.Constants;
import com.glenn.domain.Coin;
import com.glenn.service.HttpClientService;
import com.glenn.service.MailService;
import com.glenn.util.CommonUtils;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
@Configurable
public class PriceCalculationTask {

    private static String GET_PRICE_DETAIL = "https://otc-api.eiijo.cn/v1/data/market/detail?currencyId=1&portal=web";

    @Resource
    private HttpClientService httpClientService;

    @Resource
    private MailService mailService;

    private HashMap<String, Coin> priceBeforeHalfAnHour = new HashMap<>();

    private HashMap<String, Coin> priceBeforeAnHour = new HashMap<>();

    private  Boolean isBuyXRP = false;

    private Double sentBuyPrice = 0.00;

    private Double sentSellPrice = 0.00;

    //每3分钟执行一次
    @Scheduled(cron = "0 */10 *  * * * ")
    public void realTimePrice() throws Exception {
        Coin[] coins = this.getPrice();
        for (Coin coin : coins) {
            if (this.getPriceBeforeHalfAnHour().get(coin.getCoinName()) == null) {
                this.getPriceBeforeHalfAnHour().put(coin.getCoinName(), coin);
            }
            if (this.getPriceBeforeAnHour().get(coin.getCoinName()) == null) {
                this.getPriceBeforeAnHour().put(coin.getCoinName(), coin);
            }
            if (null != coin) {
                String buyPriceBeforeHalfAnHour = this.getPriceBeforeHalfAnHour().get(coin.getCoinName()).getBuy();
                String sellPriceBeforeHalfAnHour = this.getPriceBeforeHalfAnHour().get(coin.getCoinName()).getSell();
                String buyPriceBeforeAnHour = this.getPriceBeforeAnHour().get(coin.getCoinName()).getBuy();
                String sellPriceBeforeAnHour = this.getPriceBeforeAnHour().get(coin.getCoinName()).getSell();
                String currentBuyPrice = coin.getBuy();
                String currentSellPrice = coin.getSell();
                isSellPriceUp(sellPriceBeforeHalfAnHour, sellPriceBeforeAnHour, currentSellPrice, coin);
                isBuyPriceDown(buyPriceBeforeHalfAnHour, buyPriceBeforeAnHour, currentBuyPrice, coin);
                if (coin.getCoinId() == Constants.XRP) {
                    specifiedPrice(currentBuyPrice, currentSellPrice);
                }

//                if (coin.getCoinId() == 1) {
//                    System.out.println(coin.getCoinName() + " current buy: " + currentBuyPrice + " ,current sell " + currentSellPrice + " , buyPriceBeforeHalfAnHour " + buyPriceBeforeHalfAnHour + " ,sellPriceBeforeHalfAnHour " + sellPriceBeforeHalfAnHour + " ,buyPriceBeforeAnHour " + buyPriceBeforeAnHour + ", sellPriceBeforeAnHour " + sellPriceBeforeAnHour);
//                    mailService.sendSimpleMail("测试邮件", coin.getCoinName() + " current buy: " + currentBuyPrice + " ,current sell " + currentSellPrice + " , buyPriceBeforeHalfAnHour " + buyPriceBeforeHalfAnHour + " ,sellPriceBeforeHalfAnHour " + sellPriceBeforeHalfAnHour + " ,buyPriceBeforeAnHour " + buyPriceBeforeAnHour + ", sellPriceBeforeAnHour " + sellPriceBeforeAnHour);
//                }
            }
        }

    }

    //每30分钟执行一次
    @Scheduled(cron = "0 */29 *  * * * ")
    public void beforePriceHalfAnHour() throws Exception {
        if (this.getPriceBeforeHalfAnHour() != null) {
            this.getPriceBeforeHalfAnHour().clear();
        }
        this.setPriceBeforeHalfAnHour(this.getPriceToMap());
        //System.out.println("half an hour ago, buy " + this.getPriceBeforeHalfAnHour().get("BTC").getBuy() + ", sell " + this.getPriceBeforeHalfAnHour().get("BTC").getSell());
    }

    //每60分钟执行一次
    @Scheduled(cron = "0 */62 *  * * * ")
    public void beforePriceAnHour() throws Exception {
        if (this.getPriceBeforeAnHour() != null) {
            this.getPriceBeforeAnHour().clear();
        }
        this.setPriceBeforeAnHour(this.getPriceToMap());
        //System.out.println("an hour ago, buy " + this.getPriceBeforeAnHour().get("BTC").getBuy() + ", sell " + this.getPriceBeforeAnHour().get("BTC").getSell());
    }

    private Coin[] getPrice() {
        ArrayList<Coin> coinsList = new ArrayList<Coin>();
        try {
            String result = httpClientService.doGet(GET_PRICE_DETAIL);
            if (null != result && !result.equals("")) {
                Map<String, Object> huoBiResult = CommonUtils.convertJsonStringToObject(result, Map.class);
                Map<String, Object> coinInfo = (Map<String, Object>) huoBiResult.get("data");
                ArrayList<Object> coinsObject = (ArrayList<Object>) coinInfo.get("detail");
                for (Object object : coinsObject) {
                    Coin coin = new Coin();
                    coin.setCoinId((Integer) ((Map)object).get("coinId"));
                    coin.setCoinName((String) ((Map)object).get("coinName"));
                    coin.setBuy((String) ((Map)object).get("buy"));
                    coin.setSell((String) ((Map)object).get("sell"));
                    coinsList.add(coin);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return (Coin[]) coinsList.toArray(new Coin[coinsList.size()]);
    }

    private HashMap<String, Coin> getPriceToMap() {
        Coin[] coins = this.getPrice();
        HashMap<String, Coin> coinHashMap = new HashMap<>();
        for (Coin coin : coins) {
            coinHashMap.put(coin.getCoinName(), coin);
        }
        return coinHashMap;
    }

    private void isSellPriceUp(String sellPriceBeforeHalfAnHour, String sellPriceBeforeAnHour, String currentSellPrice, Coin coin) {
        if ((Double.valueOf(sellPriceBeforeAnHour) + Double.valueOf(sellPriceBeforeAnHour) * 0.02) < Double.valueOf(currentSellPrice)) {
            mailService.sendSimpleMail("【Important】"+  coin.getCoinName() + " sale price is going up than half an hour ago " +"【Important】",coin.getCoinName() + " current sale price: " + currentSellPrice + " RMB, half an hour ago price: " + sellPriceBeforeHalfAnHour + " RMB");
        }

        if ((Double.valueOf(sellPriceBeforeHalfAnHour) + Double.valueOf(sellPriceBeforeHalfAnHour) * 0.02) < Double.valueOf(currentSellPrice)) {
            mailService.sendSimpleMail("【Important】"+  coin.getCoinName() + " sale price is going up than an hour ago " +"【Important】",coin.getCoinName() + " current sale price: " + currentSellPrice + " RMB, an hour ago price: " + sellPriceBeforeAnHour + " RMB");
        }

    }

    private void isBuyPriceDown(String buyPriceBeforeHalfAnHour, String buyPriceBeforeAnHour, String currentBuyPrice, Coin coin) {

        if ((Double.valueOf(buyPriceBeforeHalfAnHour) - Double.valueOf(buyPriceBeforeHalfAnHour) * 0.02) > Double.valueOf(currentBuyPrice)) {
            mailService.sendSimpleMail("【Important】"+  coin.getCoinName() + " purchase price is going down than half an hour ago " +"【Important】",coin.getCoinName() + " current purchase price: " + currentBuyPrice + " RMB, half an hour ago price: " + buyPriceBeforeHalfAnHour + " RMB");
        }

        if ((Double.valueOf(buyPriceBeforeAnHour) - Double.valueOf(buyPriceBeforeAnHour) * 0.02) > Double.valueOf(currentBuyPrice)) {
            mailService.sendSimpleMail("【Important】"+  coin.getCoinName() + " purchase price is going down than an hour ago " +"【Important】",coin.getCoinName() + " current purchase price: " + currentBuyPrice + " RMB, an hour ago price: " + buyPriceBeforeAnHour + " RMB");
        }
    }


    private void specifiedPrice(String currentBuyPrice, String currentSellPrice) {
        Double expectedXRPBuyPrice = Double.valueOf(2.11);
        Double expectedXRPSellPrice = Double.valueOf(2.12);

        if (Double.valueOf(currentBuyPrice) != getSentBuyPrice() && Double.valueOf(currentBuyPrice) <= expectedXRPBuyPrice) {
            mailService.sendSimpleMail("【重要邮件】波波币当前["+ currentBuyPrice+"]价格可买入【及时查看】", "波波币当前价格为 " + currentBuyPrice);
            setSentBuyPrice(Double.valueOf(currentBuyPrice));
        }

        if (Double.valueOf(currentSellPrice) != getSentSellPrice() && Double.valueOf(currentSellPrice) >= expectedXRPSellPrice) {
            mailService.sendSimpleMail("【重要邮件】波波币当前[" + currentSellPrice + "]价格可出售【及时查看】", "波波币当前价格为 " + currentSellPrice);
            setSentSellPrice(Double.valueOf(currentSellPrice));;
        }
    }

    public HashMap<String, Coin> getPriceBeforeHalfAnHour() {
        return priceBeforeHalfAnHour;
    }

    public void setPriceBeforeHalfAnHour(HashMap<String, Coin> priceBeforeHalfAnHour) {
        this.priceBeforeHalfAnHour = priceBeforeHalfAnHour;
    }

    public HashMap<String, Coin> getPriceBeforeAnHour() {
        return priceBeforeAnHour;
    }

    public void setPriceBeforeAnHour(HashMap<String, Coin> priceBeforeAnHour) {
        this.priceBeforeAnHour = priceBeforeAnHour;
    }

    public Boolean getBuyXRP() {
        return isBuyXRP;
    }

    public void setBuyXRP(Boolean buyXRP) {
        isBuyXRP = buyXRP;
    }

    public Double getSentBuyPrice() {
        return sentBuyPrice;
    }

    public void setSentBuyPrice(Double sentBuyPrice) {
        this.sentBuyPrice = sentBuyPrice;
    }

    public Double getSentSellPrice() {
        return sentSellPrice;
    }

    public void setSentSellPrice(Double sentSellPrice) {
        this.sentSellPrice = sentSellPrice;
    }
}
