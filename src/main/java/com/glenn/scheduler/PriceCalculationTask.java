package com.glenn.scheduler;

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

    //每3分钟执行一次
    @Scheduled(cron = "0 */3 *  * * * ")
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
            }
        }

        System.out.println(new Date() + "current price " + Arrays.asList(coins));
    }

    //每30分钟执行一次
    @Scheduled(cron = "0 */30 *  * * * ")
    public void beforePriceHalfAnHour() throws Exception {
        this.setPriceBeforeHalfAnHour(this.getPriceToMap());
        System.out.println(new Date() + "price half an hour ago " + this.getPriceBeforeHalfAnHour());
    }

    //每60分钟执行一次
    @Scheduled(cron = "0 */60 *  * * * ")
    public void beforePriceAnHour() throws Exception {
        this.setPriceBeforeAnHour(this.getPriceToMap());
        System.out.println(new Date() + "price an hour ago " + this.getPriceBeforeAnHour());
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
        this.getPriceBeforeAnHour().clear();
        this.getPriceBeforeHalfAnHour().clear();
        Coin[] coins = this.getPrice();
        HashMap<String, Coin> coinHashMap = new HashMap<>();
        for (Coin coin : coins) {
            coinHashMap.put(coin.getCoinName(), coin);
        }
        return coinHashMap;
    }

    private void isSellPriceUp(String sellPriceBeforeHalfAnHour, String sellPriceBeforeAnHour, String currentSellPrice, Coin coin) {
        if ((Double.valueOf(sellPriceBeforeAnHour) + Double.valueOf(sellPriceBeforeAnHour) * 0.02) < Double.valueOf(currentSellPrice)) {
            mailService.sendSimpleMail("【重要邮件】"+  coin.getCoinName() + " sell price is going up than half an hour ago" +"【及时查看】",coin.getCoinName() + " current sell price: " + currentSellPrice + " RMB, half an hour ago price: " + sellPriceBeforeHalfAnHour + " RMB");
        }

        if ((Double.valueOf(sellPriceBeforeHalfAnHour) + Double.valueOf(sellPriceBeforeHalfAnHour) * 0.02) < Double.valueOf(currentSellPrice)) {
            mailService.sendSimpleMail("【重要邮件】"+  coin.getCoinName() + " sell price is going up than an hour ago" +"【及时查看】",coin.getCoinName() + " current sell price: " + currentSellPrice + " RMB, an hour ago price: " + sellPriceBeforeAnHour + " RMB");
        }

    }

    private void isBuyPriceDown(String buyPriceBeforeHalfAnHour, String buyPriceBeforeAnHour, String currentBuyPrice, Coin coin) {

        if ((Double.valueOf(buyPriceBeforeHalfAnHour) - Double.valueOf(buyPriceBeforeHalfAnHour) * 0.02) > Double.valueOf(currentBuyPrice)) {
            mailService.sendSimpleMail("【重要邮件】"+  coin.getCoinName() + " buy price is going down than half an hour ago" +"【及时查看】",coin.getCoinName() + " current buy price: " + currentBuyPrice + " RMB, half an hour ago price: " + buyPriceBeforeHalfAnHour + " RMB");
        }

        if ((Double.valueOf(buyPriceBeforeAnHour) - Double.valueOf(buyPriceBeforeAnHour) * 0.02) > Double.valueOf(currentBuyPrice)) {
            mailService.sendSimpleMail("【重要邮件】"+  coin.getCoinName() + " buy price is going down than an hour ago" +"【及时查看】",coin.getCoinName() + " current buy price: " + currentBuyPrice + " RMB, an hour ago price: " + buyPriceBeforeAnHour + " RMB");
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
}
