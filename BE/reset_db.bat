@echo off
chcp 65001 >nul
echo ========================================
echo Reset Database
echo ========================================

docker exec -i itjob psql -U admin -d postgres -c "DROP DATABASE IF EXISTS itjob;"
docker exec -i itjob psql -U admin -d postgres -c "CREATE DATABASE itjob;"
docker exec -i itjob psql -U admin -d itjob < database_postgresql.sql
docker exec -i itjob psql -U admin -d itjob < database_postgresql_seed.sql
docker exec -i itjob psql -U admin -d itjob < add_test_accounts.sql

echo.
echo ✅ Done!
pause
