package com.pragmatix.admin.services.command;

import com.pragmatix.admin.services.ICommand;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.02.12 11:22
 */
@Service
public class ExecGScriptCommand implements ICommand, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${GroovyScriptFolder:groovy}")
    private String scriptFolder;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getName() {
        return "execGScript";
    }

    @Override
    public String getHint() {
        return "execute groovy script from folder [" + scriptFolder + "]";
    }

    @Override
    public String execute(String param) {
        String[] args = param.split(";");
        if(args.length < 2) {
            return "usage: scriptFileName;method[;param]+";
        }

        File scriptFile = new File(scriptFolder, args[0] + ".groovy");

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            PrintWriter console = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"));

            try {
                GroovyShell shell = new GroovyShell();

                Script script = shell.parse(scriptFile);

                script.invokeMethod(args[1], new Object[]{applicationContext, Arrays.copyOfRange(args, 2, args.length), console});
            } catch (Exception e) {
                log.error(e.toString(), e);

                String consoleText = e.toString();
                if(consoleText.length() > 512) {
                    consoleText = consoleText.substring(0, 512);
                }
                console.print(consoleText);
            }

            console.flush();

            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
