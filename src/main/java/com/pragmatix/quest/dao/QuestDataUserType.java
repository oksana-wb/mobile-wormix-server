package com.pragmatix.quest.dao;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import static com.pragmatix.quest.dao.QuestDao.GSON;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.2016 12:25
 */
public abstract class QuestDataUserType implements UserType {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static boolean useJsonb = false;

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
        try {
            String value = rs.getString(names[0]);
            return StringUtils.isNotEmpty(value) ? GSON.fromJson(value, returnedClass()) : newInstance();
        } catch (JsonSyntaxException e) {
            log.error(e.toString());
            return newInstance();
        }
    }

    private Object newInstance() {
        try {
            return returnedClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        String json = GSON.toJson(value);
        if(useJsonb) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(json);
            st.setObject(index, jsonObject);
        } else {
            st.setString(index, json);
        }
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
