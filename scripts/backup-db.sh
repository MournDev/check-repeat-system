#!/bin/bash
#
# 数据库备份脚本
# 用法: ./backup-db.sh
# 建议: 通过 crontab 配置每日备份
#       0 2 * * * /path/to/scripts/backup-db.sh >> /var/log/db-backup.log 2>&1

set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_DIR:-/data/backups/database}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
DB_NAME="${DB_NAME:-check-repeat-system}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[$(date)] 开始备份数据库 $DB_NAME..."

mysqldump \
  --host="$MYSQL_HOST" \
  --port="$MYSQL_PORT" \
  --user="$MYSQL_USER" \
  --password="$MYSQL_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  "$DB_NAME" | gzip > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
  echo "[$(date)] 备份成功: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
else
  echo "[$(date)] 备份失败！"
  exit 1
fi

# 清理过期备份
echo "[$(date)] 清理 ${RETENTION_DAYS} 天前的备份..."
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -delete

# 保留最近 7 天备份的计数
RECENT_COUNT=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime -7 | wc -l)
echo "[$(date)] 最近7天备份数: $RECENT_COUNT"
echo "[$(date)] 备份完成."
