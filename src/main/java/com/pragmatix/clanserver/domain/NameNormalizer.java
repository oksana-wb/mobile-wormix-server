package com.pragmatix.clanserver.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 20.06.13 17:12
 */
@Service
public class NameNormalizer {

    private static boolean upperCase = false;

    static char[][] convertChars = {
            {'а', 'a'},
            {'В', 'B'},
            {'г', 'r'},
            {'Е', 'E'},
            {'К', 'K'},
            {'М', 'M'},
            {'Н', 'H'},
            {'о', 'o'},
            {'Р', 'P'},
            {'с', 'c'},
            {'Т', 'T'},
            {'у', 'y'},
            {'Х', 'X'},
            {'ь', 'b'},
            {'№', 'N'},
    };

    @Value("${clan.clanName.min:3}")
    private int clanNameMin = 3;

    @Value("${clan.clanName.max:25}")
    private int clanNameMax = 25;

    @PostConstruct
    public void init() {

        for(char[] pair : convertChars) {
            pair[0] = toNormalCase(pair[0]);
            pair[1] = toNormalCase(pair[1]);
        }

        Arrays.sort(convertChars, (o1, o2) -> o1[0] - o2[0]);

    }

    public boolean isValidName(String name) {
        return name != null && name.length() >= clanNameMin && name.length() <= clanNameMax;
    }

    public String trim(String name) {
        return name != null ? name.trim() : "";
    }

    static String normalizeName(String name) {
        name = toNormalCase(name.trim());
        int[] buf = new int[name.length()];
        for(int i = 0; i < buf.length; i++) {
            buf[i] = (name.charAt(i) << 16) | i;
        }
        Arrays.sort(buf);

        char[] res = new char[buf.length];

        int ixc = 0;
        int ixb = 0;
        while (ixb < buf.length) {
            char c = (char) (buf[ixb] >> 16);

            if (ixc < convertChars.length) {
                char[] pair = convertChars[ixc];

                if (c == pair[0]) {
                    c = pair[1];
                } else if (pair[0] < c) {
                    ixc++;

                    continue;
                }
            }

            res[buf[ixb] & 0xFFFF] = c;

            ixb++;
        }

        return new String(res);
    }

    static String toNormalCase(String text) {
        return upperCase ? text.toUpperCase() : text.toLowerCase();
    }

    static char toNormalCase(char c) {
        return upperCase ? Character.toUpperCase(c) : Character.toLowerCase(c);
    }

}
