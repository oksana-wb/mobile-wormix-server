package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда для взятия конкретной страницы друзей со списка друзей
 * User: denis
 * Date: 17.04.2010
 * Time: 17:00:37
 *
 * @see com.pragmatix.app.controllers.FriendListController#onGetFriendListPage(GetFriendListPage, com.pragmatix.app.model.UserProfile)
 */
@Command(18)
public class GetFriendListPage {

    /**
     * номер страницы списка друзей
     * <b>нумерация страниц идет с еденицы</b>
     */
    public int pageIndex;

    public GetFriendListPage() {
    }

    @Override
    public String toString() {
        return "GetFriendListPage{" +
                "pageIndex=" + pageIndex +
                '}';
    }
}
