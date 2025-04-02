package com.pragmatix.steam.web.request;


import com.pragmatix.steam.web.responses.TxnStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 9:35
 *         <p>
 * Структура отдельной позиции корзины покупки в {@link InitTxnRequest}
 *
 * @see InitTxnRequest#lineitems
 */
public class OrderItemStructure {

    /**
     * Game ID number of item
     */
    public int itemid;

    /**
     * Quantity of this item
     */
    public int qty;

    /**
     * 	Total cost (in cents), for example, 199 (=1.99)
     */
    public int amount;

    /**
     * Description of item
     */
    public String description;

    /**
     * Optional category grouping for item
     */
    public String category;

    /**
     * NB: используется только в QueryTxnResponse
     *
     * Total VAT or tax (in cents)
     */
    public Integer vat;

    /**
     * NB: используется только в QueryTxnResponse
     *
     * Status of item within the order
     */
    public TxnStatus itemstatus;

    // для json-десериализации
    public OrderItemStructure() { }

    public OrderItemStructure(int itemid, int qty, int amount, String description) {
        this.itemid = itemid;
        this.qty = qty;
        this.amount = amount;
        this.description = description;
    }

    public Map<String, String> toMap() {
        Map<String, String> res = new LinkedHashMap<>();
        res.put("itemid", String.valueOf(itemid));
        res.put("qty", String.valueOf(qty));
        res.put("amount", String.valueOf(amount));
        res.put("description", description);
        if (category != null && !category.isEmpty()) {
            res.put("category", category);
        }
        return res;
    }

    @Override
    public String toString() {
        return "{" +
                "itemid=" + itemid +
                ", qty=" + qty +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                (vat != null ? ", vat=" + vat : "") +
                (itemstatus != null ? ", itemstatus=" + itemstatus : "") +
                '}';
    }

    public String mkString(String currency) {
        return "{id " + itemid +
                (description != null ? ": " + description : "") +
                "×" + qty +
                " = " + new BigDecimal(amount).movePointLeft(2) + currency +
                (vat != null ? ", VAT: " + new BigDecimal(vat).movePointLeft(2) + currency : "") +
                (itemstatus != null ? ", " + itemstatus : "") +
                (category != null && !category.isEmpty() ? " #" + category : "") +
                '}';
    }

    public static String mkString(Collection<OrderItemStructure> items, String currency) {
        if (items != null) {
            return items.stream()
                    .map(x -> x != null ? x.mkString(currency) : "null")
                    .collect(Collectors.joining(", ", "[", "]"));
        } else {
            return "null";
        }
    }
}
