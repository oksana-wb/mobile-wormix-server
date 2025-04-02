--  платежи контакта
ALTER TABLE wormswar.payment_statistic ADD COLUMN item character varying(32) NOT NULL;

-- в поле s_killed_worms теперь будут учитываться убитые драконы
update achieve.worms_achievements set s_killed_worms=0