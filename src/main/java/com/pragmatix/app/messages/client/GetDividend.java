package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 21.04.2016 9:39
 *         <p>
 * Забрать проценты по всем вкладам на сегодня
 *
 * @see com.pragmatix.app.controllers.PaymentController#onGetDividend(GetDividend, com.pragmatix.app.model.UserProfile)
 * @see com.pragmatix.app.messages.server.GetDividendResult
 */
@Command(133)
public class GetDividend {
}
