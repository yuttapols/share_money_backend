ALTER TABLE bank_accounts DROP CONSTRAINT bank_accounts_creditor_id_key;

CREATE INDEX ix_bank_accounts_creditor_id ON bank_accounts (creditor_id);
