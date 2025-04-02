package com.pragmatix.admin.services;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.05.11 17:41
 */
public interface ICommand {

    String getName();

    String getHint();

    String execute(String param);

}
