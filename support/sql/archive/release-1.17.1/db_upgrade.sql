ALTER TABLE wormswar.referral_link_visit ADD COLUMN date timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE wormswar.referral_link_visit
  ADD CONSTRAINT referral_link_visit_referral_link_id_fkey FOREIGN KEY (referral_link_id)
      REFERENCES wormswar.referral_link (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
