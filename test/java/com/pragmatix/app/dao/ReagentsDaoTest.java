package com.pragmatix.app.dao;

import com.pragmatix.craft.dao.ReagentsDao;
import com.pragmatix.craft.dao.ReagentsMapper;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import org.junit.Assert;
import org.junit.Test;
import com.pragmatix.testcase.JdbcUnitTest;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.08.2016 11:34
 */
public class ReagentsDaoTest extends JdbcUnitTest {

    @Test
    public void crudTest() throws Exception {
        ReagentsMapper mapper = new ReagentsMapper();
        mapper.setJdbcTemplate(jdbcTemplate());
        mapper.setTransactionTemplate(transactionTemplate());
        mapper.init();

        ReagentsDao dao = new ReagentsDao(mapper);

        update("delete from " + mapper.tableName);

        ReagentsEntity entity = new ReagentsEntity(testerProfileId);
        dao.persist(entity);

        ReagentsEntity result = dao.select(testerProfileId);
        Assert.assertNull(result);

        entity.addReagentValue(Reagent.gear, 10);
        dao.persist(entity);

        result = dao.select(testerProfileId);
        Assert.assertEquals(10, result.getReagentValue(Reagent.gear));

        entity.addReagentValue(Reagent.gear, 10);
        entity.addReagentValue(Reagent.wood, 10);
        dao.persist(entity);

        result = dao.select(testerProfileId);
        Assert.assertEquals(20, result.getReagentValue(Reagent.gear));
        Assert.assertEquals(10, result.getReagentValue(Reagent.wood));
    }
}
