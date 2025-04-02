-- новая таблица по приглашениям вернутся в игру
CREATE TABLE wormswar.callback_friend
(
  friend_id integer NOT NULL,
  profile_id integer NOT NULL,
  date timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT callback_friend_pkey PRIMARY KEY (friend_id, profile_id),
  CONSTRAINT callback_friend_fk_friend_id FOREIGN KEY (friend_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.callback_friend
  OWNER TO smos;

-- новые платежи контакта
ALTER TABLE wormswar.payment_statistic ADD COLUMN transaction_id bigint NOT NULL;
ALTER TABLE wormswar.payment_statistic
  ADD CONSTRAINT payment_statistic_transaction_id_key UNIQUE(transaction_id);
