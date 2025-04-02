package com.pragmatix.steam.web.responses;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 31.03.2017 17:24
 *         <p>
 * Список кодов ошибок Steam'а
 * @see <a href="https://partner.steamgames.com/documentation/MicroTxn#ErrorCodes">MicroTxn#ErrorCodes</a>
 *
 * @see CustomErrors - "наши" собственные ошибки, введенные вдобавок к этим
 */
public class SteamErrors {
    public static final int SUCCESS = 1;        // Success
    public static final int FAILED = 2;         // Operation failed
    public static final int INVALID_PARAM = 3;  // Invalid parameter
    public static final int INTERNAL = 4;       // Internal error
    public static final int USER_NOT_APPROVED = 5;  // User has not approved transaction
    public static final int ALREADY_COMMITTED = 6;  // Transaction has already been committed
    public static final int USER_NOT_LOGGED_IN = 7; // User is not logged in
    public static final int CURRENCY_MISMATCH = 8;  // Currency does not match user's Steam Account currency
    public static final int USER_NOT_EXIST = 9;     // Account does not exist or is temporarily unavailable
    public static final int USER_DENIED = 10;       // Transaction has been denied by user
    public static final int DENIED_COUNTRY = 11;    // Transaction has been denied because user is in a restricted country
    public static final int DENIED_AGREEMENT = 12;  // Transaction has been denied because billing agreement is not active
    public static final int BILL_NOT_GAME = 13;     // Billing agreement cannot be processed because it is not type GAME
    public static final int BILL_DISPUTE = 14;      // Billing agreement is on hold due to billing dispute or chargeback
    public static final int BILL_NOT_STEAM = 15;    // Billing agreement cannot be processed because it is not type STEAM
    public static final int BILL_ALREADY = 16;      // User already has a billing agreement for this game
    public static final int INSUFFICIENT_FUNDS = 100;  // Insufficient funds
    public static final int TIME_LIMIT = 101;       // Time limit for finalization has been exceeded
    public static final int DISABLED = 102;         // Account is disabled
    public static final int NOT_ALLOWED = 103;      // Account is not allowed to purchase
    public static final int DENIED_FRAUD = 104;     // Transaction denied due to fraud detection
    public static final int NO_PAYMENT_METHOD = 105;// No cached payment method
    public static final int BILL_EXCEED = 106;      // Transaction would exceed the spending limit of the billing agreement
}
