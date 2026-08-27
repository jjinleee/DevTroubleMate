SELECT 'CREATE DATABASE devtroublemate_test'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'devtroublemate_test'
)\gexec
