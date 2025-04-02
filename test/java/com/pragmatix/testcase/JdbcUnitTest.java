package com.pragmatix.testcase;

import com.pragmatix.testcase.AbstractTest;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 19.05.2016 15:40
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/jdbc-beans.xml"})
public class JdbcUnitTest extends AbstractTest {

    @Resource
    JdbcTemplate jdbcTemplate;

    @Resource
    TransactionTemplate transactionTemplate;

    public void update(String query) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update(query);
            }
        });
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    public TransactionTemplate transactionTemplate() {
        return transactionTemplate;
    }
}
