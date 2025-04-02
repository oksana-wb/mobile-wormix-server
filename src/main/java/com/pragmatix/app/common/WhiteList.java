package com.pragmatix.app.common;

import com.pragmatix.gameapp.social.SocialServiceEnum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class WhiteList {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Set<String> set = Collections.emptySet();

    private final String dataFileName = "data/testers.txt";

    @PostConstruct
    public void init() {
        File file = new File(dataFileName);
        if(file.exists()) {
            try {
                List<String> lines = FileUtils.readLines(file, "UTF-8");
                if(!lines.isEmpty())
                    setSet(lines, false);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }
    }

    public void printSet() {
        log.info("WhiteList:");
        for(String item : set) {
            log.info("'{}'", item);
        }
    }

    public boolean isValid(SocialServiceEnum socialNet, Object id) {
        return set.isEmpty() || socialNet == SocialServiceEnum.mobile || set.contains(id.toString().trim().toLowerCase());
    }

    public Set<String> getSet() {
        return set;
    }

    public void setSet(Collection<String> seq, boolean writeToDisk) {
        TreeSet<String> newSet = new TreeSet<>();
        for(String line : seq) {
            String trim = line.toLowerCase().trim();
            if(!trim.isEmpty())
                newSet.add(trim);
        }
        this.set = newSet;

        if(writeToDisk) {
            try {
                FileUtils.writeLines(new File(dataFileName), seq);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }

        printSet();
    }

}
