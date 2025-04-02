package com.pragmatix.steam.web.responses;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 13:09
 *         <p>
 * Статус покупки, созданной {@link com.pragmatix.df.server.social.steam.request.InitTxnRequest}
 * @see QueryTxnResponse
 */
public enum TxnStatus {
    Init,	        // Order has been created but not authorized by user.
    Approved,	    // Order has been approved by user.
    Succeeded,	    // Order has been successfully processed.
    Failed,	        // Order has failed or been denied.
    Refunded,	    // Order has been refunded and product should be revoked by the game. A refund may be initiated by the customer or forced by Valve when a fraudulent transaction has been identified.
    PartialRefund,	// One or more items in the cart have been refunded. Check itemstatus field of each item for details.
    Chargedback,	// Order is fraudulent or disputed and product should be revoked by the game.
    ;
}
