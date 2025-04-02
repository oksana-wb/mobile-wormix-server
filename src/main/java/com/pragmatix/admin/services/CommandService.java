package com.pragmatix.admin.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.05.11 17:41
 */
@Service
public class CommandService {

    private Map<String, ICommand> commandMap = new HashMap<String, ICommand>();

    public String execute(String conmmand, String param){
       if(commandMap.containsKey(conmmand)){
           return commandMap.get(conmmand).execute(param);
       }else{
           String result="Unknown command ["+conmmand+"]\n";
           List<ICommand> values = new ArrayList<ICommand>(commandMap.values());
           Collections.sort(values, new Comparator<ICommand>() {
               @Override
               public int compare(ICommand o1, ICommand o2) {
                   return o1.getName().compareTo(o2.getName());
               }
           });
           for(ICommand comm : values) {
               result+=comm.getName()+" ("+comm.getHint()+")\n";
           }
           return result;
       }
    }

    @Autowired(required = true)
    public void setCommands(List<ICommand> commands) {
        for(ICommand command : commands) {
            commandMap.put(command.getName(), command);
        }
    }

}
