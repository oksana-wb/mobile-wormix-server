package com.pragmatix.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.08.12 18:57
 */
@Component
public class BankServiceRegistry {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<Byte, BankServiceI> bankServicesMap = new HashMap<Byte, BankServiceI>();

    @Autowired(required = false)
    public void setSocialServices(Set<BankServiceI> bankServices) {
        for(BankServiceI bankService : bankServices) {
            log.info("Registrate BankService [{}] for social [{}] ",bankService.getClass(), bankService.getSocialId());
            bankServicesMap.put((byte) bankService.getSocialId().getType(), bankService);
        }
    }

    public BankServiceI getBankServiceFor(byte socialNetId){
       return bankServicesMap.get(socialNetId);
    }

}
