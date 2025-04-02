-- Function: archive_stat_table(character varying)

CREATE OR REPLACE FUNCTION archive_stat_table(p_table character varying)
  RETURNS character varying AS
$BODY$
DECLARE
  v_new_table varchar;
  v_query varchar;
BEGIN
  -- добавляем к имени таблицы текущую дату
  v_new_table := p_table || '_' || to_char(now(), 'YYYY_MM_DD') ;

  raise notice 'archive [%] to [%] ...', p_table, v_new_table;
  -- удаляем первичный ключ
  execute 'ALTER TABLE wormswar.' || p_table || ' DROP CONSTRAINT ' || p_table || '_pkey';
  -- индивидуальная очистка некоторых таблиц
  if p_table = 'payment_statistic' then
    execute 'ALTER TABLE wormswar.payment_statistic DROP CONSTRAINT payment_statistic_fk_profile_id';
    execute 'DROP INDEX wormswar.payment_statistic_date_idx';
    execute 'DROP TRIGGER payment_statistic_after_success ON wormswar.payment_statistic';
  end if;
  -- переименовываем таблицу
  execute 'ALTER TABLE wormswar.' || p_table || ' RENAME TO ' || v_new_table;
  -- переносим в схему log
  execute 'ALTER TABLE wormswar.' || v_new_table || '  SET SCHEMA "log"';
  -- добавляем первичный ключ
  execute 'ALTER TABLE "log".' || v_new_table || ' ADD CONSTRAINT ' || v_new_table || '_pkey PRIMARY KEY(id)';
  -- создаем индекс по полю profile_id
  execute 'CREATE INDEX ' || v_new_table || '_i_profile_id ON "log".' || v_new_table || ' USING btree (profile_id)';

  -- создаем  новую таблицу
  execute 'CREATE TABLE wormswar.' || p_table || '( LIKE  "log".' || v_new_table || ' )';
  execute 'ALTER TABLE wormswar.' || p_table || ' INHERIT public.' || p_table || '_parent';
  execute 'ALTER TABLE wormswar.' || p_table || ' OWNER TO smos';
  execute 'ALTER TABLE wormswar.' || p_table || ' ADD CONSTRAINT ' || p_table || '_pkey PRIMARY KEY(id)';
  -- индивидуальные настройки для некоторых таблиц
  if p_table = 'payment_statistic' then
    execute 'ALTER TABLE wormswar.payment_statistic ADD CONSTRAINT payment_statistic_fk_profile_id FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION';
    execute 'CREATE INDEX payment_statistic_date_idx ON wormswar.payment_statistic USING btree (date DESC NULLS LAST)';
    execute 'CREATE TRIGGER payment_statistic_after_success AFTER INSERT OR UPDATE ON wormswar.payment_statistic FOR EACH ROW EXECUTE PROCEDURE wormswar.payment_statistic_success_trigger()';
  end if;

  return 'OK';
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION archive_stat_table(character varying)
  OWNER TO smos;

--select archive_stat_table('payment_statistic');
select archive_stat_table('shop_statistic');
select archive_stat_table('award_statistic');

-- запустить на одноклассгиках
GRANT SELECT ON TABLE wormswar.payment_statistic TO nagios_role;





