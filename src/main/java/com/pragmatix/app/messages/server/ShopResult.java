package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.TemporalStuffStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * команда отправляеться с сервера и говорит
 * о результате продажи
 *
 * @author denis
 *         date 25.11.2009
 *         time: 18:51:10
 */
@Command(10003)
public class ShopResult implements SecuredResponse {

    public ShopResultEnum result;
    /**
     * массив купленного оружия
     */
    public List<BackpackItemStructure> backpack;
    /**
     * массив купленных шапок, амулетов и тд
     */
    public List<Short> stuff;
    /**
     * массив купленных шапок, амулетов и тд
     */
    public List<TemporalStuffStructure> temporalStuff;

    public ShopResult() {
        this(ShopResultEnum.SUCCESS);
    }

    public ShopResult(ShopResultEnum result) {
        this.result = result;
    }

    public ShopResult(List<BackpackItemStructure> boughtBackpackList, List<Short> boughtStuffList, List<TemporalStuffStructure> boughtTemporalStuffList) {
        this(ShopResultEnum.SUCCESS);

        this.backpack = boughtBackpackList;
        this.stuff = boughtStuffList;
        this.temporalStuff = boughtTemporalStuffList;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ShopResult{" +
                "result=" + result +
                (backpack != null && !backpack.isEmpty() ? ", backpack=" + backpack : "") +
                (stuff != null && !stuff.isEmpty() ? ", stuff=" + stuff : "") +
                (temporalStuff != null && !temporalStuff.isEmpty() ? ", temporalStuff=" + temporalStuff : "") +
                '}';
    }

}
