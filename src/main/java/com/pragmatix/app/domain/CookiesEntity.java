package com.pragmatix.app.domain;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pragmatix.gameapp.common.Identifiable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.11.2016 10:43
 */
@Entity
@Table(schema = "wormswar", name = "cookies")
@TypeDef(name = "CookiesValuesType", typeClass = CookiesEntity.CookiesValuesType.class)
@NamedQuery(name = "CookiesEntity.update", query = "update CookiesEntity e set e.values = :values where e.profileId = :profileId")
public class CookiesEntity implements Identifiable<Integer> {

    @Id
    private int profileId;

    @Column(name = "values_as_json")
    @Type(type = "CookiesValuesType")
    private Object[] values;

    @Transient
    public volatile boolean dirty = false;

    @Transient
    public volatile boolean newly = false;

    public CookiesEntity() {
    }

    public CookiesEntity(int profileId) {
        this.profileId = profileId;
        this.values = ArrayUtils.EMPTY_OBJECT_ARRAY;
        this.newly = true;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public boolean isEmpty() {
        return ArrayUtils.isEmpty(values);
    }

    public void setValue(String rawKey, String rawValue) {
        Object key = tryConvertToNumber(rawKey);
        Map<Object, Object> map = valuesAsMap();
        if(StringUtils.isBlank(rawValue)) {
            Object oldValue = map.remove(key);
            values = mapToArray(map);

            dirty |= oldValue != null;
        } else {
            Object value = tryConvertToNumber(rawValue);
            Object oldValue = map.put(key, value);
            values = mapToArray(map);

            dirty |= !Objects.equals(oldValue, value);
        }
    }

    private static Object[] mapToArray(Map<Object, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .toArray(Object[]::new);
    }

    private Map<Object, Object> valuesAsMap() {
        return valuesAsMap(values);
    }

    private static final Comparator<Object> comparator = (o1, o2) -> {
        if(o1 instanceof String && o2 instanceof Number) {
            return 1;
        } else if(o1 instanceof Number && o2 instanceof String) {
            return -1;
        } else {
            return ((Comparable) o1).compareTo(o2);
        }
    };

    private static Map<Object, Object> valuesAsMap(Object[] values) {
        Map<Object, Object> map = new TreeMap<>(comparator);
        for(int i = 0; i < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }
        return map;
    }

    private static Object tryConvertToNumber(String s) {
        if(StringUtils.isNumeric(s)) {
            try {
                return Byte.valueOf(s);
            } catch (NumberFormatException e) {
                return Integer.valueOf(s);
            }
        } else {
            return s;
        }
    }

    private static Object tryConvertToNumber(Object o) {
        if(o instanceof Double) {
            int i = ((Double) o).intValue();
            if(i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
                return (byte) i;
            } else {
                return i;
            }
        } else {
            return o;
        }
    }

    @Override
    public Integer getId() {
        return profileId;
    }

    public static class CookiesValuesType implements UserType {

        private static final Logger log = LoggerFactory.getLogger(CookiesValuesType.class);

        private static final Gson GSON = new Gson();

        @Override
        public Class<Object[]> returnedClass() {
            return Object[].class;
        }

        @Override
        public int[] sqlTypes() {
            return new int[]{Types.JAVA_OBJECT};
        }

        @Override
        public boolean equals(Object o, Object o1) throws HibernateException {
            return Objects.equals(o, o1);
        }

        @Override
        public int hashCode(Object o) throws HibernateException {
            return Objects.hashCode(o);
        }

        @Override
        public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
            String value = rs.getString(names[0]);
            return fromJson(value);
        }

        public static Object[] fromJson(String value) {
            try {
                if(StringUtils.isNotEmpty(value)) {
                    Map<Object, Object> resultMap = new TreeMap<>(comparator);
                    Map<String, Object> rawMap = GSON.fromJson(value, Map.class);
                    for(Map.Entry<String, Object> entry : rawMap.entrySet()) {
                        resultMap.put(tryConvertToNumber(entry.getKey()), tryConvertToNumber(entry.getValue()));
                    }
                    return mapToArray(resultMap);
                } else {
                    return newInstance();
                }
            } catch (JsonSyntaxException e) {
                log.error(e.toString());
                return newInstance();
            }
        }

        private static Object[] newInstance() {
            return ArrayUtils.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
            String json = toJson((Object[]) value);
            st.setString(index, json);
        }

        public static String toJson(Object[] value) {
            return GSON.toJson(valuesAsMap(value));
        }

        @Override
        public Object deepCopy(Object value) throws HibernateException {
            return value;
        }

        @Override
        public boolean isMutable() {
            return true;
        }

        @Override
        public Serializable disassemble(Object o) throws HibernateException {
            return null;
        }

        @Override
        public Object assemble(Serializable serializable, Object o) throws HibernateException {
            return null;
        }

        @Override
        public Object replace(Object original, Object target, Object owner) throws HibernateException {
            return null;
        }

    }

}
