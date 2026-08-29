DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'share_money') THEN
      CREATE ROLE share_money LOGIN PASSWORD 'share_money';
   END IF;
END
$$;
